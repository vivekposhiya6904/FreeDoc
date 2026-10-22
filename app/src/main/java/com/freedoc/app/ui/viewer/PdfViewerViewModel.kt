package com.freedoc.app.ui.viewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedoc.app.engine.pdf.NativePdfDocument
import com.freedoc.app.engine.pdf.NativePdfRendererEngine
import com.freedoc.app.engine.pdf.PageDimensions
import com.freedoc.app.engine.pdf.PdfDocument
import com.freedoc.app.engine.pdf.PdfErrorInfo
import com.freedoc.app.engine.pdf.PdfErrorType
import com.freedoc.app.engine.pdf.PdfException
import com.freedoc.app.engine.pdf.PdfSearchEngine
import com.freedoc.app.engine.pdf.PdfSelection
import com.freedoc.app.engine.pdf.PdfTextExtractor
import com.freedoc.app.engine.pdf.PdfTextSelectionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * ViewModel governing PDF selection, lifecycle, continuous multi-page rendering,
 * LRU caching, and direct page navigation.
 *
 * Enforces memory safety and concurrency rules:
 * - Employs a bounded LRU memory cache ([PdfPageCache]) to budget RAM during continuous vertical scrolling.
 * - Serializes and dedupes page render requests.
 * - Dispatches all disk I/O and page rasterization off the main thread.
 * - Releases previous document resources ([PdfRenderer], [ParcelFileDescriptor], cache) on close or document replacement.
 */
/**
 * Programmatic navigation request sent to the continuous reader composable.
 *
 * @param requestId Monotonically increasing unique timestamp ensuring recomposition triggers even for same-page matches.
 * @param pageIndex 0-based PDF page index.
 * @param matchIndex 0-based index of the selected search match, or -1 for page-number jumps.
 * @param isSmoothScroll True for animated Next/Previous scrolling; false for instant jumps.
 */
data class NavigationRequest(
    val requestId: Long,
    val pageIndex: Int,
    val matchIndex: Int = -1,
    val isSmoothScroll: Boolean = true
)

/**
 * ViewModel governing PDF selection, lifecycle, continuous multi-page rendering,
 * LRU caching, and direct page navigation.
 *
 * Enforces memory safety and concurrency rules:
 * - Employs a bounded LRU memory cache ([PdfPageCache]) to budget RAM during continuous vertical scrolling.
 * - Serializes and dedupes page render requests.
 * - Dispatches all disk I/O and page rasterization off the main thread.
 * - Releases previous document resources ([PdfRenderer], [ParcelFileDescriptor], cache) on close or document replacement.
 */
class PdfViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Idle)
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _navigationRequest = MutableStateFlow<NavigationRequest?>(null)
    val navigationRequest: StateFlow<NavigationRequest?> = _navigationRequest.asStateFlow()

    private val _selection = MutableStateFlow<PdfSelection?>(null)
    val selection: StateFlow<PdfSelection?> = _selection.asStateFlow()

    @Volatile
    private var currentDocument: PdfDocument? = null
    private var appContext: Context? = null
    private var extractedPages: Map<Int, String>? = null
    private var extractedPageContents: Map<Int, PdfTextExtractor.PageTextContent>? = null
    private var hasSearchableText: Boolean = true
    private var extractionErrorMessage: String? = null
    private val extractionMutex = Mutex()
    private var searchJob: Job? = null

    private val pageCache = PdfPageCache(maxEntries = 8)
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private val pageDimensions = ConcurrentHashMap<Int, PageDimensions>()

    /**
     * Opens a document from the given [uri].
     *
     * @param context Application context used for ContentResolver operations.
     * @param uri Scoped document URI obtained via Storage Access Framework.
     */
    fun openDocument(context: Context, uri: Uri) {
        // Cancel all existing rendering work and release previous document
        cancelAllJobs()
        searchJob?.cancel()
        _navigationRequest.value = null
        _selection.value = null
        extractedPages = null
        extractedPageContents = null
        hasSearchableText = true
        extractionErrorMessage = null
        _searchState.value = SearchUiState()
        pageCache.clear()
        pageDimensions.clear()
        currentDocument?.close()
        currentDocument = null

        _uiState.value = PdfViewerUiState.Loading("Opening document…")

        val appContext = context.applicationContext
        this.appContext = appContext
        viewModelScope.launch {
            val result = NativePdfRendererEngine.open(appContext, uri)
            result.onSuccess { doc ->
                currentDocument = doc

                // Pre-populate page dimensions for initial pages to ensure stable LazyColumn item heights
                val initialCount = minOf(doc.pageCount, 10)
                for (i in 0 until initialCount) {
                    try {
                        pageDimensions[i] = doc.getPageDimensions(i)
                    } catch (_: Exception) {
                    }
                }

                _uiState.value = PdfViewerUiState.Ready(
                    metadata = doc.metadata,
                    currentPage = 0,
                    totalPages = doc.pageCount,
                    pageBitmaps = pageCache.snapshot(),
                    pageDimensions = pageDimensions.toMap(),
                    isPageLoading = true
                )

                // Request initial visible page and adjacent prefetch
                requestPage(0)
                if (doc.pageCount > 1) {
                    requestPage(1)
                }

                // Eagerly pre-extract document text in background so text selection and hit-testing are instant
                viewModelScope.launch(Dispatchers.IO) {
                    ensureTextExtracted()
                }
            }.onFailure { throwable ->
                val errorInfo = mapThrowableToErrorInfo(throwable)
                _uiState.value = PdfViewerUiState.Error(
                    errorType = errorInfo.errorType,
                    title = errorInfo.title,
                    message = errorInfo.message
                )
            }
        }
    }

    /**
     * Called by the scrolling presentation when a page becomes visible in the viewport.
     */
    fun onPageVisible(visiblePageIndex: Int) {
        val doc = currentDocument ?: return
        if (visiblePageIndex !in 0 until doc.pageCount) return

        val currentState = _uiState.value
        if (currentState is PdfViewerUiState.Ready) {
            if (currentState.currentPage != visiblePageIndex) {
                _uiState.value = currentState.copy(currentPage = visiblePageIndex)
            }
        }

        // Cancel render jobs for pages far outside the visible viewport (> 3 pages away)
        // to prevent obsolete background rasterization from blocking the PdfRenderer lock.
        val distantPages = activeJobs.keys.filter { abs(it - visiblePageIndex) > 3 }
        for (page in distantPages) {
            activeJobs.remove(page)?.cancel()
        }

        // Request visible page and prefetch adjacent pages
        requestPage(visiblePageIndex)
        if (visiblePageIndex + 1 < doc.pageCount) {
            requestPage(visiblePageIndex + 1)
        }
        if (visiblePageIndex - 1 >= 0) {
            requestPage(visiblePageIndex - 1)
        }
    }

    /**
     * Requests asynchronous rendering for [pageIndex].
     * Deduplicates requests if the page is already cached or an active render job is in flight.
     */
    fun requestPage(pageIndex: Int, targetWidth: Int = 1080) {
        val doc = currentDocument ?: return
        if (pageIndex !in 0 until doc.pageCount) return

        // Skip if already in memory cache
        if (pageCache.contains(pageIndex)) return

        // Skip if a render job for this page is already running
        if (activeJobs[pageIndex]?.isActive == true) return

        val job = viewModelScope.launch {
            try {
                // Ensure dimensions are cached for accurate aspect ratio
                if (!pageDimensions.containsKey(pageIndex)) {
                    val dims = doc.getPageDimensions(pageIndex)
                    pageDimensions[pageIndex] = dims
                }

                val bitmap = doc.renderPage(pageIndex, targetWidth)
                if (isActive) {
                    pageCache.put(pageIndex, bitmap)
                    val currentState = _uiState.value
                    if (currentState is PdfViewerUiState.Ready) {
                        _uiState.value = currentState.copy(
                            pageBitmaps = pageCache.snapshot(),
                            pageDimensions = pageDimensions.toMap(),
                            isPageLoading = activeJobs.values.any { it.isActive && it != coroutineContext[Job] }
                        )
                    }
                }
            } catch (_: Exception) {
                // Individual page rendering failure does not crash the entire continuous viewer
            } finally {
                activeJobs.remove(pageIndex)
            }
        }

        activeJobs[pageIndex] = job
    }

    /**
     * Internal helper to request programmatic navigation to [pageIndex] and optionally highlight [matchIndex].
     * Emits a fresh [NavigationRequest] with a unique timestamp ensuring the LazyColumn scrolls
     * even if navigating between multiple matches on the same page.
     */
    fun requestNavigation(pageIndex: Int, matchIndex: Int = -1, isSmoothScroll: Boolean = true) {
        val doc = currentDocument ?: return
        if (pageIndex !in 0 until doc.pageCount) return

        val currentState = _uiState.value
        if (currentState is PdfViewerUiState.Ready && currentState.currentPage != pageIndex) {
            _uiState.value = currentState.copy(currentPage = pageIndex)
        }

        // Cancel distant jobs so target page can render immediately without queue delay
        val distantPages = activeJobs.keys.filter { abs(it - pageIndex) > 3 }
        for (page in distantPages) {
            activeJobs.remove(page)?.cancel()
        }

        requestPage(pageIndex)
        if (pageIndex + 1 < doc.pageCount) {
            requestPage(pageIndex + 1)
        }
        if (pageIndex - 1 >= 0) {
            requestPage(pageIndex - 1)
        }

        _navigationRequest.value = NavigationRequest(
            requestId = System.nanoTime(),
            pageIndex = pageIndex,
            matchIndex = matchIndex,
            isSmoothScroll = isSmoothScroll
        )
    }

    /**
     * Jumps directly to a 1-based [targetPageOneBased] human page number.
     *
     * @return True if [targetPageOneBased] is within [1, totalPages], false otherwise.
     */
    fun jumpToPage(targetPageOneBased: Int): Boolean {
        val doc = currentDocument ?: return false
        val validation = PageValidationResult.validate(targetPageOneBased.toString(), doc.pageCount)
        return when (validation) {
            is PageValidationResult.Valid -> {
                val targetIndex = validation.targetIndexZeroBased
                requestNavigation(targetIndex, matchIndex = -1, isSmoothScroll = false)
                true
            }
            is PageValidationResult.Invalid -> false
        }
    }

    /**
     * Navigates to the next page if available.
     */
    fun nextPage() {
        val state = _uiState.value
        if (state is PdfViewerUiState.Ready && state.hasNextPage) {
            jumpToPage(state.displayPageNumber + 1)
        }
    }

    /**
     * Navigates to the previous page if available.
     */
    fun previousPage() {
        val state = _uiState.value
        if (state is PdfViewerUiState.Ready && state.hasPreviousPage) {
            jumpToPage(state.displayPageNumber - 1)
        }
    }

    /**
     * Jumps directly to the specified [targetPage] (0-based index) for backward compatibility.
     */
    fun goToPage(targetPage: Int) {
        jumpToPage(targetPage + 1)
    }

    /**
     * Closes the active document and returns to [PdfViewerUiState.Idle].
     */
    fun closeDocument() {
        cancelAllJobs()
        searchJob?.cancel()
        _navigationRequest.value = null
        _selection.value = null
        extractedPages = null
        extractedPageContents = null
        hasSearchableText = true
        _searchState.value = SearchUiState()
        pageCache.clear()
        pageDimensions.clear()
        currentDocument?.close()
        currentDocument = null
        appContext = null
        _uiState.value = PdfViewerUiState.Idle
    }

    /**
     * Activates the compact search interface and pre-extracts document text if not already cached.
     */
    fun openSearch() {
        _searchState.value = _searchState.value.copy(isSearchActive = true)
        if (extractedPages == null) {
            viewModelScope.launch(Dispatchers.IO) {
                ensureTextExtracted()
            }
        }
    }

    /**
     * Closes the search interface and clears active search results.
     */
    fun closeSearch() {
        searchJob?.cancel()
        _searchState.value = SearchUiState(isSearchActive = false)
    }

    /**
     * Updates the search query and searches across all pages of the document.
     */
    fun onSearchQueryChanged(newQuery: String) {
        searchJob?.cancel()
        val trimmed = newQuery.trim()
        if (trimmed.isEmpty()) {
            _searchState.value = _searchState.value.copy(
                query = newQuery,
                isSearching = false,
                matches = emptyList(),
                currentMatchIndex = -1,
                errorMessage = null
            )
            return
        }

        _searchState.value = _searchState.value.copy(
            query = newQuery,
            isSearching = true,
            errorMessage = null
        )

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            if (extractedPages == null) {
                ensureTextExtracted()
            }
            val pages = extractedPages ?: emptyMap()
            if (!hasSearchableText) {
                val errorMsg = if (extractionErrorMessage != null) {
                    "Error reading document: $extractionErrorMessage"
                } else {
                    "No searchable text found in this PDF"
                }
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    matches = emptyList(),
                    currentMatchIndex = -1,
                    hasSearchableText = false,
                    errorMessage = errorMsg
                )
                return@launch
            }

            val matches = PdfSearchEngine.search(
                query = trimmed,
                pages = pages,
                pageContents = extractedPageContents ?: emptyMap(),
                documentSessionId = currentDocument?.uri?.toString() ?: ""
            )
            _searchState.value = _searchState.value.copy(
                isSearching = false,
                matches = matches,
                currentMatchIndex = if (matches.isNotEmpty()) 0 else -1,
                hasSearchableText = true,
                errorMessage = null
            )

            // Automatically navigate to the first match
            if (matches.isNotEmpty()) {
                requestNavigation(matches[0].pageIndex, matchIndex = 0, isSmoothScroll = false)
            }
        }
    }

    /**
     * Navigates to the next search match with circular wrapping.
     */
    fun nextSearchMatch() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val nextIdx = (state.currentMatchIndex + 1) % state.matches.size
        _searchState.value = state.copy(currentMatchIndex = nextIdx)
        requestNavigation(state.matches[nextIdx].pageIndex, matchIndex = nextIdx, isSmoothScroll = true)
    }

    /**
     * Navigates to the previous search match with circular wrapping.
     */
    fun previousSearchMatch() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val prevIdx = if (state.currentMatchIndex - 1 < 0) state.matches.size - 1 else state.currentMatchIndex - 1
        _searchState.value = state.copy(currentMatchIndex = prevIdx)
        requestNavigation(state.matches[prevIdx].pageIndex, matchIndex = prevIdx, isSmoothScroll = true)
    }

    private suspend fun ensureTextExtracted() = extractionMutex.withLock {
        if (extractedPages != null) return@withLock
        val doc = currentDocument ?: return@withLock
        val context = appContext
        try {
            val result = if (doc is NativePdfDocument) {
                val file = doc.tempFile ?: if (context != null) {
                    try {
                        val temp = File.createTempFile("freedoc_search_", ".pdf", context.cacheDir)
                        context.contentResolver.openInputStream(doc.uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        }
                        doc.tempFile = temp
                        temp
                    } catch (_: Exception) {
                        null
                    }
                } else null

                if (file != null && file.exists()) {
                    PdfTextExtractor.extract(file, context)
                } else {
                    context?.contentResolver?.openInputStream(doc.uri)?.use { stream ->
                        PdfTextExtractor.extract(stream, context)
                    } ?: PdfTextExtractor.ExtractionResult(emptyMap(), emptyMap(), false)
                }
            } else {
                context?.contentResolver?.openInputStream(doc.uri)?.use { stream ->
                    PdfTextExtractor.extract(stream, context)
                } ?: PdfTextExtractor.ExtractionResult(emptyMap(), emptyMap(), false)
            }

            extractedPages = result.pages
            extractedPageContents = result.pageContents
            hasSearchableText = result.hasSearchableText
            extractionErrorMessage = result.errorMessage
        } catch (e: Exception) {
            extractedPages = emptyMap()
            extractedPageContents = emptyMap()
            hasSearchableText = false
            extractionErrorMessage = e.localizedMessage ?: e.message
        }
    }

    private fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    private fun mapThrowableToErrorInfo(throwable: Throwable): PdfErrorInfo {
        return when (throwable) {
            is PdfException -> {
                when (throwable.errorType) {
                    PdfErrorType.EMPTY_FILE -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Empty Document",
                        message = "The selected file is empty or contains no readable content."
                    )
                    PdfErrorType.INVALID_FORMAT -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Invalid PDF Document",
                        message = "The selected file is corrupted or is not a valid PDF."
                    )
                    PdfErrorType.PASSWORD_PROTECTED -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Password-Protected PDF",
                        message = "This PDF is encrypted with a password. Encrypted PDFs are not supported in this version."
                    )
                    PdfErrorType.PERMISSION_DENIED -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Storage Access Denied",
                        message = "Could not access the selected document. Storage permission may have expired."
                    )
                    PdfErrorType.OUT_OF_MEMORY -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Out of Memory",
                        message = "Device has insufficient memory to render this page."
                    )
                    PdfErrorType.IO_ERROR -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Unable to Read File",
                        message = "Could not read the document from storage."
                    )
                    else -> PdfErrorInfo(
                        errorType = throwable.errorType,
                        title = "Unable to Open PDF",
                        message = throwable.message ?: "An unexpected error occurred."
                    )
                }
            }
            else -> PdfErrorInfo(
                errorType = PdfErrorType.UNKNOWN,
                title = "Error Opening PDF",
                message = throwable.localizedMessage ?: "An unexpected error occurred while processing the document."
            )
        }
    }

    private var activeDragAnchorIndex: Int? = null

    /**
     * Hit-tests the touched point ([pdfX], [pdfY]) on [pageIndex] and selects the tapped word.
     */
    fun selectWordAt(pageIndex: Int, pdfX: Float, pdfY: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            if (extractedPageContents == null) {
                ensureTextExtracted()
            }
            val content = extractedPageContents?.get(pageIndex) ?: return@launch
            val charIndex = PdfTextSelectionEngine.hitTestChar(content, pdfX, pdfY)
            if (charIndex >= 0) {
                val wordRange = PdfTextSelectionEngine.findWordRange(content.text, charIndex)
                if (wordRange != null) {
                    val sel = PdfTextSelectionEngine.createSelection(
                        pageIndex = pageIndex,
                        content = content,
                        startIndex = wordRange.first,
                        endIndex = wordRange.second
                    )
                    _selection.value = sel
                }
            }
        }
    }

    /**
     * Captures the fixed opposite boundary anchor when beginning a drag on either handle.
     */
    fun startSelectionDrag(isStartHandle: Boolean) {
        val currentSel = _selection.value ?: return
        activeDragAnchorIndex = if (isStartHandle) currentSel.endIndex else currentSel.startIndex
    }

    /**
     * Dynamically updates the selection boundary when dragging start or end handles.
     */
    fun updateSelectionBoundary(pageIndex: Int, pdfX: Float, pdfY: Float, isStartHandle: Boolean) {
        val currentSel = _selection.value ?: return
        if (currentSel.pageIndex != pageIndex) return
        val content = extractedPageContents?.get(pageIndex) ?: return
        val charIndex = PdfTextSelectionEngine.hitTestChar(content, pdfX, pdfY)
        if (charIndex >= 0) {
            val updated = PdfTextSelectionEngine.adjustSelectionBoundary(
                currentSelection = currentSel,
                content = content,
                targetCharIndex = charIndex,
                isStartHandle = isStartHandle,
                anchorCharIndex = activeDragAnchorIndex
            )
            if (updated != null) {
                _selection.value = updated
            }
        }
    }

    /**
     * Resets the active drag anchor when boundary dragging finishes or cancels.
     */
    fun endSelectionDrag() {
        activeDragAnchorIndex = null
    }

    /**
     * Selects all text on the specified [pageIndex].
     */
    fun selectAllOnPage(pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (extractedPageContents == null) {
                ensureTextExtracted()
            }
            val content = extractedPageContents?.get(pageIndex) ?: return@launch
            val sel = PdfTextSelectionEngine.createSelection(
                pageIndex = pageIndex,
                content = content,
                startIndex = 0,
                endIndex = content.text.length
            )
            _selection.value = sel
        }
    }

    /**
     * Clears any active user text selection.
     */
    fun clearSelection() {
        _selection.value = null
        activeDragAnchorIndex = null
    }

    /**
     * Returns extracted page content if available for testing or direct inspection.
     */
    fun getExtractedPageContent(pageIndex: Int): PdfTextExtractor.PageTextContent? {
        return extractedPageContents?.get(pageIndex)
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
        searchJob?.cancel()
        _navigationRequest.value = null
        _selection.value = null
        activeDragAnchorIndex = null
        extractedPages = null
        extractedPageContents = null
        hasSearchableText = true
        _searchState.value = SearchUiState()
        pageCache.clear()
        pageDimensions.clear()
        currentDocument?.close()
        currentDocument = null
        appContext = null
    }
}
