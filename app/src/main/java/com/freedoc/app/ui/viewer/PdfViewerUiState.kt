package com.freedoc.app.ui.viewer

import android.graphics.Bitmap
import com.freedoc.app.core.model.DocumentMetadata
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
     * Document page is loaded and displayed.
     */
    data class Ready(
        val metadata: DocumentMetadata,
        val currentPage: Int,
        val totalPages: Int,
        val pageBitmap: Bitmap? = null,
        val isPageLoading: Boolean = false
    ) : PdfViewerUiState {
        val hasPreviousPage: Boolean
            get() = currentPage > 0

        val hasNextPage: Boolean
            get() = currentPage < totalPages - 1

        val displayPageNumber: Int
            get() = currentPage + 1
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
