package com.freedoc.app.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReadingProgressTest {

    @Test
    fun testPercentageCalculationForSinglePageDocument() {
        val percentage = ReadingProgress.calculatePercentage(currentPageOneBased = 1, totalPages = 1)
        assertEquals(100, percentage)
    }

    @Test
    fun testPercentageCalculationForMultiPageDocument() {
        val totalPages = 120

        // Page 1 of 120 -> round(1 / 120 * 100) = round(0.833) = 1%
        val firstPagePercent = ReadingProgress.calculatePercentage(1, totalPages)
        assertEquals(1, firstPagePercent)

        // Page 24 of 120 -> round(24 / 120 * 100) = 20%
        val intermediatePercent = ReadingProgress.calculatePercentage(24, totalPages)
        assertEquals(20, intermediatePercent)

        // Page 60 of 120 -> 50%
        val middlePercent = ReadingProgress.calculatePercentage(60, totalPages)
        assertEquals(50, middlePercent)

        // Page 120 of 120 -> 100%
        val lastPagePercent = ReadingProgress.calculatePercentage(120, totalPages)
        assertEquals(100, lastPagePercent)
    }

    @Test
    fun testPercentageCalculationEdgeCasesAndClamping() {
        // Zero or negative total pages
        assertEquals(0, ReadingProgress.calculatePercentage(1, 0))
        assertEquals(0, ReadingProgress.calculatePercentage(1, -5))

        // Page number less than 1 is clamped to 1
        assertEquals(1, ReadingProgress.calculatePercentage(0, 100))
        assertEquals(1, ReadingProgress.calculatePercentage(-10, 100))

        // Page number exceeding total pages is clamped to total pages (100%)
        assertEquals(100, ReadingProgress.calculatePercentage(150, 100))
    }

    @Test
    fun testFormatProgressString() {
        assertEquals("1 / 1 · 100%", ReadingProgress.formatProgress(1, 1))
        assertEquals("24 / 120 · 20%", ReadingProgress.formatProgress(24, 120))
        assertEquals("1 / 120 · 1%", ReadingProgress.formatProgress(1, 120))
        assertEquals("120 / 120 · 100%", ReadingProgress.formatProgress(120, 120))
        assertEquals("0 / 0 · 0%", ReadingProgress.formatProgress(1, 0))
    }

    @Test
    fun testScrubberFractionCalculation() {
        // Single page document
        assertEquals(0f, ReadingProgress.calculateScrubberFraction(0, 1), 0.001f)

        // 100 pages document (indexes 0..99)
        val totalPages = 100
        assertEquals(0f, ReadingProgress.calculateScrubberFraction(0, totalPages), 0.001f)
        assertEquals(0.5f, ReadingProgress.calculateScrubberFraction(49, 99), 0.01f)
        assertEquals(1f, ReadingProgress.calculateScrubberFraction(99, totalPages), 0.001f)

        // Clamping on out of bounds
        assertEquals(0f, ReadingProgress.calculateScrubberFraction(-5, totalPages), 0.001f)
        assertEquals(1f, ReadingProgress.calculateScrubberFraction(200, totalPages), 0.001f)
    }

    @Test
    fun testPageIndexFromScrubberFraction() {
        val totalPages = 100

        // Start (0f -> page index 0)
        assertEquals(0, ReadingProgress.pageIndexFromScrubberFraction(0f, totalPages))

        // Quarter (0.25f -> page index 25)
        assertEquals(25, ReadingProgress.pageIndexFromScrubberFraction(0.25f, totalPages))

        // Half (0.50f -> page index 50)
        assertEquals(50, ReadingProgress.pageIndexFromScrubberFraction(0.50f, totalPages))

        // Approximately 90%
        assertEquals(89, ReadingProgress.pageIndexFromScrubberFraction(0.90f, totalPages))

        // End (1.0f -> page index 99)
        assertEquals(99, ReadingProgress.pageIndexFromScrubberFraction(1.0f, totalPages))

        // Single page document always maps to index 0
        assertEquals(0, ReadingProgress.pageIndexFromScrubberFraction(0.5f, 1))
    }
}
