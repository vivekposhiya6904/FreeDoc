package com.freedoc.app.engine.pdf

import android.graphics.Bitmap
import android.net.Uri
import com.freedoc.app.core.model.DocumentMetadata

/**
 * Common interface representing an opened, readable PDF document.
 *
 * Implementations manage document lifecycle and single-page rendering off the main thread.
 * Only one page bitmap is rendered per call to enforce minimal memory footprint.
 */
interface PdfDocument : AutoCloseable {
    val uri: Uri
    val metadata: DocumentMetadata
    val pageCount: Int

    /**
     * Renders a single page identified by [pageIndex] into a [Bitmap].
     *
     * @param pageIndex 0-based page index (must satisfy 0 <= pageIndex < pageCount).
     * @param targetWidth Desired target width in pixels for scaling page dimensions.
     * @return Rendered [Bitmap] representing the page with white background.
     * @throws IndexOutOfBoundsException if pageIndex is out of range.
     * @throws IllegalStateException if document is closed or page rendering fails.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap

    override fun close()
}
