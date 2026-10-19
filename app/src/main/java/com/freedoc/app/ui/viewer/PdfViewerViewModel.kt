package com.freedoc.app.ui.viewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedoc.app.engine.pdf.NativePdfRendererEngine
import com.freedoc.app.engine.pdf.PdfDocument
import com.freedoc.app.engine.pdf.PdfErrorInfo
import com.freedoc.app.engine.pdf.PdfErrorType
import com.freedoc.app.engine.pdf.PdfException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel governing PDF selection, lifecycle, asynchronous page rendering, and navigation.
 *
 * Enforces memory safety and concurrency rules:
 * - Cancels stale render requests when pages are navigated rapidly.
 * - Releases previous document resources ([PdfRenderer], [ParcelFileDescriptor], cache) on close or document replacement.
 * - Dispatches all disk I/O and page rasterization off the main thread.
 */
class PdfViewerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Idle)
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    @Volatile
    private var currentDocument: PdfDocument? = null
    private var renderJob: Job? = null

    /**
     * Opens a document from the given [uri].
     *
     * @param context Application context used for ContentResolver operations.
     * @param uri Scoped document URI obtained via Storage Access Framework.
     */
    fun openDocument(context: Context, uri: Uri) {
        // Cancel existing rendering work and release previous document
        renderJob?.cancel()
        currentDocument?.close()
        currentDocument = null

        _uiState.value = PdfViewerUiState.Loading("Opening document…")

        val appContext = context.applicationContext
        viewModelScope.launch {
            val result = NativePdfRendererEngine.open(appContext, uri)
            result.onSuccess { doc ->
                currentDocument = doc
                loadPage(0)
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
     * Navigates to the next page if available.
     */
    fun nextPage() {
        val state = _uiState.value
        if (state is PdfViewerUiState.Ready && state.hasNextPage) {
            loadPage(state.currentPage + 1)
        }
    }

    /**
     * Navigates to the previous page if available.
     */
    fun previousPage() {
        val state = _uiState.value
        if (state is PdfViewerUiState.Ready && state.hasPreviousPage) {
            loadPage(state.currentPage - 1)
        }
    }

    /**
     * Jumps directly to the specified [targetPage] (0-based index).
     */
    fun goToPage(targetPage: Int) {
        val doc = currentDocument ?: return
        val clamped = targetPage.coerceIn(0, doc.pageCount - 1)
        loadPage(clamped)
    }

    /**
     * Closes the active document and returns to [PdfViewerUiState.Idle].
     */
    fun closeDocument() {
        renderJob?.cancel()
        currentDocument?.close()
        currentDocument = null
        _uiState.value = PdfViewerUiState.Idle
    }

    private fun loadPage(pageIndex: Int) {
        val doc = currentDocument ?: return
        if (pageIndex !in 0 until doc.pageCount) return

        renderJob?.cancel()

        // Maintain existing page display while rendering next page to avoid flashing
        val currentState = _uiState.value
        if (currentState is PdfViewerUiState.Ready) {
            _uiState.value = currentState.copy(isPageLoading = true)
        } else {
            _uiState.value = PdfViewerUiState.Loading("Rendering page ${pageIndex + 1}…")
        }

        renderJob = viewModelScope.launch {
            try {
                val bitmap = doc.renderPage(pageIndex)
                if (isActive) {
                    _uiState.value = PdfViewerUiState.Ready(
                        metadata = doc.metadata,
                        currentPage = pageIndex,
                        totalPages = doc.pageCount,
                        pageBitmap = bitmap,
                        isPageLoading = false
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    val errorInfo = mapThrowableToErrorInfo(e)
                    _uiState.value = PdfViewerUiState.Error(
                        errorType = errorInfo.errorType,
                        title = errorInfo.title,
                        message = errorInfo.message
                    )
                }
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        renderJob?.cancel()
        currentDocument?.close()
        currentDocument = null
    }
}
