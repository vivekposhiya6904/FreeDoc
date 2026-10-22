package com.freedoc.app.ui.viewer

import android.graphics.Bitmap
import com.freedoc.app.core.model.DocumentMetadata
import com.freedoc.app.engine.pdf.PageDimensions
import com.freedoc.app.engine.pdf.PdfErrorType

/**
 * UI State for the PDF Viewer screen following Unidirectional Data Flow (UDF).
 */
sealed interface PdfViewerUiState {

    /**
     * No document is currently loaded.
     */
    data object Idle : PdfViewerUiState

    /**
     * Document is being opened or page is rendering.
     */
    data class Loading(
        val message: String = "Opening document…"
    ) : PdfViewerUiState

    /**
     * Document is open and ready for continuous vertical viewing.
     *
     * @param metadata Scoped document metadata.
     * @param currentPage 0-based index of the currently visible / selected page.
     * @param totalPages Total page count in document.
     * @param pageBitmaps Bounded memory cache of currently rendered page bitmaps.
     * @param pageDimensions Cached aspect ratio / points dimensions for pages.
     * @param isPageLoading True if a requested page or background render is actively running.
     */
    data class Ready(
        val metadata: DocumentMetadata,
        val currentPage: Int,
        val totalPages: Int,
        val pageBitmaps: Map<Int, Bitmap> = emptyMap(),
        val pageDimensions: Map<Int, PageDimensions> = emptyMap(),
        val isPageLoading: Boolean = false
    ) : PdfViewerUiState {
        val hasPreviousPage: Boolean
            get() = currentPage > 0

        val hasNextPage: Boolean
            get() = currentPage < totalPages - 1

        val displayPageNumber: Int
            get() = currentPage + 1

        val readingPercentage: Int
            get() = ReadingProgress.calculatePercentage(displayPageNumber, totalPages)

        val readingProgressText: String
            get() = ReadingProgress.formatProgress(displayPageNumber, totalPages)

        /**
         * Backward-compatible property exposing the currently selected page's bitmap.
         */
        val pageBitmap: Bitmap?
            get() = pageBitmaps[currentPage]

        /**
         * Backward-compatible constructor for single-bitmap creation and legacy tests.
         */
        constructor(
            metadata: DocumentMetadata,
            currentPage: Int,
            totalPages: Int,
            pageBitmap: Bitmap?,
            isPageLoading: Boolean = false
        ) : this(
            metadata = metadata,
            currentPage = currentPage,
            totalPages = totalPages,
            pageBitmaps = if (pageBitmap != null) mapOf(currentPage to pageBitmap) else emptyMap(),
            isPageLoading = isPageLoading
        )
    }

    /**
     * An error occurred while opening or rendering the document.
     */
    data class Error(
        val errorType: PdfErrorType,
        val title: String,
        val message: String
    ) : PdfViewerUiState
}

/**
 * UI State for the inline/compact PDF text search experience.
 */
data class SearchUiState(
    val isSearchActive: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false,
    val matches: List<com.freedoc.app.engine.pdf.SearchMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val hasSearchableText: Boolean = true,
    val errorMessage: String? = null
) {
    val totalMatches: Int get() = matches.size

    val currentMatchDisplay: String
        get() = if (totalMatches > 0 && currentMatchIndex in matches.indices) {
            "${currentMatchIndex + 1} of $totalMatches"
        } else if (isSearching) {
            "Searching…"
        } else if (query.isNotBlank()) {
            if (!hasSearchableText) "No searchable text" else "0 matches"
        } else {
            ""
        }
}

