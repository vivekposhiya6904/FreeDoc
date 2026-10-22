package com.freedoc.app.engine.pdf

import android.graphics.Bitmap
import android.net.Uri
import com.freedoc.app.core.model.DocumentMetadata

/**
 * Geometric dimensions of a single PDF page in typographic points (1/72 inch).
 */
data class PageDimensions(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 0.707f // Fallback to standard A4 ratio
}

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
     * Retrieves the unscaled typographic dimensions (in points) for [pageIndex].
     * Used by layout containers to compute stable aspect ratios without full rasterization.
     */
    fun getPageDimensions(pageIndex: Int): PageDimensions

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
