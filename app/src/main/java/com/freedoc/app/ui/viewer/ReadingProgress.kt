package com.freedoc.app.ui.viewer

import kotlin.math.roundToInt

/**
 * Utility for calculating and formatting reading progress, completion percentages,
 * and scrubber track fractions.
 */
object ReadingProgress {

    /**
     * Calculates the 1-based completion percentage:
     * `round(currentPage / totalPages * 100)`
     *
     * @param currentPageOneBased 1-based current page number.
     * @param totalPages Total page count in document.
     * @return Clamped integer in range 0..100.
     */
    fun calculatePercentage(currentPageOneBased: Int, totalPages: Int): Int {
        if (totalPages <= 0) return 0
        val clampedPage = currentPageOneBased.coerceIn(1, totalPages)
        val raw = (clampedPage.toFloat() / totalPages.toFloat()) * 100f
        return raw.roundToInt().coerceIn(0, 100)
    }

    /**
     * Formats reading progress as `currentPage / totalPages · percentage%`.
     * Example: `24 / 120 · 20%`.
     *
     * @param currentPageOneBased 1-based current page number.
     * @param totalPages Total page count in document.
     */
    fun formatProgress(currentPageOneBased: Int, totalPages: Int): String {
        if (totalPages <= 0) return "0 / 0 · 0%"
        val clampedPage = currentPageOneBased.coerceIn(1, totalPages)
        val percentage = calculatePercentage(clampedPage, totalPages)
        return "$clampedPage / $totalPages · $percentage%"
    }

    /**
     * Calculates the normalized vertical position (0f..1f) of the scrubber thumb
     * based on the zero-based [currentPageIndexZeroBased] and [totalPages].
     */
    fun calculateScrubberFraction(currentPageIndexZeroBased: Int, totalPages: Int): Float {
        if (totalPages <= 1) return 0f
        val clampedIndex = currentPageIndexZeroBased.coerceIn(0, totalPages - 1)
        return (clampedIndex.toFloat() / (totalPages - 1).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Maps a scrubber vertical fraction (0f..1f) to a 0-based page index.
     */
    fun pageIndexFromScrubberFraction(fraction: Float, totalPages: Int): Int {
        if (totalPages <= 1) return 0
        val clampedFraction = fraction.coerceIn(0f, 1f)
        return (clampedFraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
    }
}
