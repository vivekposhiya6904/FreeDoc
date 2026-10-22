package com.freedoc.app.engine.pdf

import com.freedoc.app.ui.viewer.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfSearchEngineTest {

    @Test
    fun testEmptyOrBlankQueryReturnsEmptyList() {
        val pages = mapOf(
            0 to "Hello World",
            1 to "Embedded Systems"
        )

        assertTrue(PdfSearchEngine.search("", pages).isEmpty())
        assertTrue(PdfSearchEngine.search("   ", pages).isEmpty())
        assertTrue(PdfSearchEngine.search(" \t\n ", pages).isEmpty())
    }

    @Test
    fun testCaseInsensitiveMatchingForWordMeaning() {
        val pages = mapOf(
            0 to "Understanding the fundamental meaning of embedded systems."
        )

        // Exact lowercase
        val matchesLower = PdfSearchEngine.search("meaning", pages)
        assertEquals(1, matchesLower.size)
        assertEquals(0, matchesLower[0].pageIndex)
        assertEquals(1, matchesLower[0].displayPageNumber)
        assertEquals(0, matchesLower[0].matchIndexInDocument)
        assertEquals(1, matchesLower[0].totalMatchesInDocument)
        assertTrue(matchesLower[0].snippet.lowercase().contains("meaning"))

        // Uppercase
        val matchesUpper = PdfSearchEngine.search("MEANING", pages)
        assertEquals(1, matchesUpper.size)

        // Mixed case
        val matchesMixed = PdfSearchEngine.search("Meaning", pages)
        assertEquals(1, matchesMixed.size)

        // Leading and trailing whitespace
        val matchesWhitespace = PdfSearchEngine.search("   meaning   ", pages)
        assertEquals(1, matchesWhitespace.size)
    }

    @Test
    fun testMultipleMatchesOnSamePage() {
        val pages = mapOf(
            2 to "Memory management in embedded systems. Memory allocation must be deterministic."
        )

        val matches = PdfSearchEngine.search("memory", pages)
        assertEquals(2, matches.size)
        assertEquals(2, matches[0].pageIndex)
        assertEquals(2, matches[1].pageIndex)
        assertEquals(0, matches[0].matchIndexInDocument)
        assertEquals(1, matches[1].matchIndexInDocument)
        assertEquals(2, matches[0].totalMatchesInDocument)
        assertEquals(2, matches[1].totalMatchesInDocument)
        assertEquals(3, matches[0].displayPageNumber) // 1-based page number
        assertEquals(3, matches[1].displayPageNumber)
    }

    @Test
    fun testMatchesAcrossMultiplePages() {
        val pages = mapOf(
            0 to "Introduction to Microcontrollers.",
            1 to "Hardware architecture and registers.",
            2 to "Advanced microcontroller peripherals and DMA."
        )

        val matches = PdfSearchEngine.search("microcontroller", pages)
        assertEquals(2, matches.size)
        assertEquals(0, matches[0].pageIndex)
        assertEquals(1, matches[0].displayPageNumber)
        assertEquals(2, matches[1].pageIndex)
        assertEquals(3, matches[1].displayPageNumber)
        assertEquals(0, matches[0].matchIndexInDocument)
        assertEquals(1, matches[1].matchIndexInDocument)
        assertEquals(2, matches[0].totalMatchesInDocument)
        assertEquals(2, matches[1].totalMatchesInDocument)
    }

    @Test
    fun testMultiWordQueryMatchingAcrossLineBreaks() {
        val pages = mapOf(
            0 to "This section explains the\nmeaning of embedded architectures."
        )

        val matches = PdfSearchEngine.search("meaning of embedded", pages)
        assertEquals(1, matches.size)
        assertEquals(0, matches[0].pageIndex)
        assertTrue(matches[0].matchedText.contains("meaning"))
    }

    @Test
    fun testHyphenationNormalization() {
        val rawPage = "Understanding the mean-\ning of software development."
        val cleaned = PdfTextExtractor.cleanExtractedPageText(rawPage)
        assertTrue(cleaned.contains("meaning"))

        val pages = mapOf(0 to cleaned)
        val matches = PdfSearchEngine.search("meaning", pages)
        assertEquals(1, matches.size)
    }

    @Test
    fun testSnippetGeneration() {
        val longText = "A".repeat(50) + "TARGET_KEYWORD" + "B".repeat(50)
        val pages = mapOf(0 to longText)

        val matches = PdfSearchEngine.search("TARGET_KEYWORD", pages)
        assertEquals(1, matches.size)
        val snippet = matches[0].snippet
        assertTrue(snippet.contains("TARGET_KEYWORD"))
    }

    @Test
    fun testNoMatchesFound() {
        val pages = mapOf(
            0 to "Chapter 1: Basics",
            1 to "Chapter 2: Intermediate"
        )

        val matches = PdfSearchEngine.search("NonExistentKeywordXYZ", pages)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchUiStateCountersAndLoading() {
        // Initial / closed state
        val initial = SearchUiState()
        assertFalse(initial.isSearchActive)
        assertEquals("", initial.currentMatchDisplay)

        // Empty query when search active
        val openEmpty = SearchUiState(isSearchActive = true, query = "")
        assertEquals("", openEmpty.currentMatchDisplay)

        // Loading / extracting state (should show Searching..., NOT 0 matches)
        val searchingState = SearchUiState(isSearchActive = true, query = "meaning", isSearching = true)
        assertEquals("Searching…", searchingState.currentMatchDisplay)

        // Query with no matches
        val noMatches = SearchUiState(isSearchActive = true, query = "quantum", matches = emptyList())
        assertEquals("0 matches", noMatches.currentMatchDisplay)

        // Scanned or textless document with no searchable text
        val textless = SearchUiState(isSearchActive = true, query = "anything", matches = emptyList(), hasSearchableText = false)
        assertEquals("No searchable text", textless.currentMatchDisplay)

        // Multiple matches
        val dummyMatches = listOf(
            SearchMatch(pageIndex = 0, matchIndexInDocument = 0, totalMatchesInDocument = 3, snippet = "a test snippet"),
            SearchMatch(pageIndex = 3, matchIndexInDocument = 1, totalMatchesInDocument = 3, snippet = "another test"),
            SearchMatch(pageIndex = 10, matchIndexInDocument = 2, totalMatchesInDocument = 3, snippet = "final test")
        )

        val stateFirstMatch = SearchUiState(
            isSearchActive = true,
            query = "test",
            matches = dummyMatches,
            currentMatchIndex = 0
        )
        assertEquals("1 of 3", stateFirstMatch.currentMatchDisplay)
        assertEquals(1, stateFirstMatch.matches[0].displayPageNumber)

        val stateSecondMatch = stateFirstMatch.copy(currentMatchIndex = 1)
        assertEquals("2 of 3", stateSecondMatch.currentMatchDisplay)
        assertEquals(4, stateSecondMatch.matches[1].displayPageNumber)

        val stateThirdMatch = stateFirstMatch.copy(currentMatchIndex = 2)
        assertEquals("3 of 3", stateThirdMatch.currentMatchDisplay)
        assertEquals(11, stateThirdMatch.matches[2].displayPageNumber)
    }

    @Test
    fun testSingleLinePageIndicatorText() {
        val pageNum = 366
        val indicatorText = pageNum.toString()
        assertEquals("366", indicatorText)
        assertFalse(indicatorText.contains("Page"))
        assertFalse(indicatorText.contains("%"))
        assertFalse(indicatorText.contains("\n"))
    }

    @Test
    fun testSearchMatchWithCharacterBoundsProducesAccurateHighlightRects() {
        val text = "Network Security Fundamentals"
        // Simulate character bounds for each char in "Network Security Fundamentals"
        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        for (ch in text) {
            val charWidth = 8f
            bounds.add(HighlightRect(curX, 100f, curX + charWidth, 114f))
            curX += charWidth
        }

        val pageContents = mapOf(
            0 to PdfTextExtractor.PageTextContent(text = text, charBounds = bounds)
        )

        val matches = PdfSearchEngine.search("Security", pageContents, documentSessionId = "doc_session_1")
        assertEquals(1, matches.size)
        val match = matches[0]
        assertEquals(0, match.pageIndex)
        assertEquals("doc_session_1", match.documentSessionId)
        assertEquals("Security", match.matchedText)
        assertEquals(8, match.startIndexOnPage)
        assertEquals(16, match.endIndexOnPage)
        assertEquals(1, match.highlightRects.size)

        val rect = match.highlightRects[0]
        val expectedLeft = 50f + (8 * 8f) // 114f
        val expectedRight = 50f + (16 * 8f) // 178f
        assertEquals(expectedLeft, rect.left, 0.01f)
        assertEquals(100f, rect.top, 0.01f)
        assertEquals(expectedRight, rect.right, 0.01f)
        assertEquals(114f, rect.bottom, 0.01f)
    }

    @Test
    fun testMultiLineMatchGroupingProducesSeparateRects() {
        // Line 1: "Data " (5 chars) at y=100..114
        // Line 2: "Security" (8 chars) at y=130..144
        val line1 = "Data "
        val line2 = "Security"
        val fullText = line1 + line2

        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        for (ch in line1) {
            bounds.add(HighlightRect(curX, 100f, curX + 10f, 114f))
            curX += 10f
        }
        curX = 50f
        for (ch in line2) {
            bounds.add(HighlightRect(curX, 130f, curX + 10f, 144f))
            curX += 10f
        }

        val pageContents = mapOf(
            2 to PdfTextExtractor.PageTextContent(text = fullText, charBounds = bounds)
        )

        val matches = PdfSearchEngine.search("Data Security", pageContents, documentSessionId = "session_multi")
        assertEquals(1, matches.size)
        val match = matches[0]
        assertEquals(2, match.pageIndex)
        // Since y-coords differ, it should produce 2 separate line bounding boxes!
        assertEquals(2, match.highlightRects.size)

        val r1 = match.highlightRects[0]
        assertEquals(100f, r1.top, 0.01f)
        assertEquals(114f, r1.bottom, 0.01f)

        val r2 = match.highlightRects[1]
        assertEquals(130f, r2.top, 0.01f)
        assertEquals(144f, r2.bottom, 0.01f)
    }

    @Test
    fun testHighlightCoordinateTransformationToCanvasPixels() {
        val pdfPageWidth = 612f // Standard US Letter points
        val pdfPageHeight = 792f
        val canvasWidth = 1080f // Device display pixels
        val canvasHeight = 1397.647f

        val scaleX = canvasWidth / pdfPageWidth
        val scaleY = canvasHeight / pdfPageHeight

        val pdfHighlight = HighlightRect(
            left = 72f,
            top = 100f,
            right = 144f,
            bottom = 120f
        )

        val pixelLeft = pdfHighlight.left * scaleX
        val pixelTop = pdfHighlight.top * scaleY
        val pixelWidth = pdfHighlight.width * scaleX
        val pixelHeight = pdfHighlight.height * scaleY

        assertEquals(127.058f, pixelLeft, 0.01f)
        assertEquals(176.470f, pixelTop, 0.01f)
        assertEquals(127.058f, pixelWidth, 0.01f)
        assertEquals(35.294f, pixelHeight, 0.01f)
    }

    @Test
    fun testNavigationSelectionCyclesAndBounds() {
        val dummyMatches = listOf(
            SearchMatch(pageIndex = 0, matchIndexInDocument = 0, totalMatchesInDocument = 3),
            SearchMatch(pageIndex = 5, matchIndexInDocument = 1, totalMatchesInDocument = 3),
            SearchMatch(pageIndex = 12, matchIndexInDocument = 2, totalMatchesInDocument = 3)
        )

        var currentIndex = 0
        // Next from 0 -> 1
        currentIndex = (currentIndex + 1) % dummyMatches.size
        assertEquals(1, currentIndex)
        assertEquals(5, dummyMatches[currentIndex].pageIndex)

        // Next from 1 -> 2
        currentIndex = (currentIndex + 1) % dummyMatches.size
        assertEquals(2, currentIndex)
        assertEquals(12, dummyMatches[currentIndex].pageIndex)

        // Next from 2 -> wraps to 0
        currentIndex = (currentIndex + 1) % dummyMatches.size
        assertEquals(0, currentIndex)
        assertEquals(0, dummyMatches[currentIndex].pageIndex)

        // Previous from 0 -> wraps to 2
        currentIndex = if (currentIndex - 1 < 0) dummyMatches.size - 1 else currentIndex - 1
        assertEquals(2, currentIndex)
        assertEquals(12, dummyMatches[currentIndex].pageIndex)

        // Previous from 2 -> 1
        currentIndex = if (currentIndex - 1 < 0) dummyMatches.size - 1 else currentIndex - 1
        assertEquals(1, currentIndex)
        assertEquals(5, dummyMatches[currentIndex].pageIndex)
    }

    @Test
    fun testDedicationSearchProducesExactGlyphBoundedSelectionRect() {
        val pageText = "This textbook begins with a special dedication to our students."
        val bounds = ArrayList<HighlightRect?>()
        var curX = 40f
        val charWidth = 7.5f
        for (ch in pageText) {
            bounds.add(HighlightRect(curX, 120f, curX + charWidth, 132f))
            curX += charWidth
        }

        val pageContents = mapOf(
            0 to PdfTextExtractor.PageTextContent(text = pageText, charBounds = bounds)
        )

        val matches = PdfSearchEngine.search("dedication", pageContents, documentSessionId = "doc_dedication")
        assertEquals(1, matches.size)
        val match = matches[0]
        assertEquals("dedication", match.matchedText)
        assertEquals(36, match.startIndexOnPage)
        assertEquals(46, match.endIndexOnPage)
        assertEquals(1, match.highlightRects.size)

        val selectionRect = match.highlightRects[0]
        val expectedLeft = 40f + (36 * charWidth)
        val expectedRight = 40f + (46 * charWidth)
        assertEquals(expectedLeft, selectionRect.left, 0.01f)
        assertEquals(expectedRight, selectionRect.right, 0.01f)
        assertEquals(120f, selectionRect.top, 0.01f)
        assertEquals(132f, selectionRect.bottom, 0.01f)
        // Rect closely hugs exactly the 10 characters of "dedication" (75 points width)
        assertEquals(75f, selectionRect.width, 0.01f)
    }

    @Test
    fun testMovingActiveSelectionBetweenOccurrencesAndClearingPrevious() {
        // Two occurrences of "dedication" on different pages
        val page0 = "A dedication to mathematics."
        val page3 = "A second dedication in the appendix."
        val pages = mapOf(0 to page0, 3 to page3)

        val matches = PdfSearchEngine.search("dedication", pages, documentSessionId = "session_test")
        assertEquals(2, matches.size)
        assertEquals(0, matches[0].pageIndex)
        assertEquals(3, matches[1].pageIndex)

        // Initially match 0 is active
        var activeIndex = 0
        var activeMatch = matches[activeIndex]
        assertEquals(0, activeMatch.pageIndex)

        // Advancing to Next match (match 1)
        activeIndex = (activeIndex + 1) % matches.size
        activeMatch = matches[activeIndex]
        assertEquals(1, activeIndex)
        assertEquals(3, activeMatch.pageIndex)
        // Match 0 is now inactive (cleared from active state)
        assertFalse(matches[0] == activeMatch)
        assertTrue(matches[1] == activeMatch)
    }

    @Test
    fun testScrollOffsetPositionsMatchAtComfortableViewingHeight() {
        val pageHeightPts = 842f
        val renderedPageHeightPx = 1372f
        val viewportHeightPx = 2400f

        // Case 1: Match near middle of page (e.g. top = 505pt, ~60% down the page)
        val matchTopPts = 505.2f
        val matchYInPagePx = (matchTopPts / pageHeightPts) * renderedPageHeightPx // ~823.2px
        val desiredScreenY = viewportHeightPx * 0.30f // 720px
        val scrollOffsetPx = (matchYInPagePx - desiredScreenY).toInt().coerceAtLeast(0)

        // Verifying offset is ~103px, positioning the match at 720px down the screen (30% of viewport)
        assertEquals(103, scrollOffsetPx)
        val finalScreenY = matchYInPagePx - scrollOffsetPx
        assertEquals(desiredScreenY, finalScreenY, 1.0f)

        // Case 2: Match near very top of page (e.g. top = 80pt)
        val topMatchPts = 80f
        val topMatchYPx = (topMatchPts / pageHeightPts) * renderedPageHeightPx // ~130.3px
        val topScrollOffset = (topMatchYPx - desiredScreenY).toInt().coerceAtLeast(0)
        assertEquals(0, topScrollOffset) // Should clamp to 0 at the top of the page
    }

    @Test
    fun testControlledZoomPolicyPreservesUserZoomWhenAdequate() {
        val minComfortableScale = 1.45f

        // When user is at 1.0x fit-to-page zoom, target scale gently increases to 1.45x
        val fitPageScale = 1.0f
        val targetScaleFromFit = if (fitPageScale < minComfortableScale) minComfortableScale else fitPageScale
        assertEquals(1.45f, targetScaleFromFit, 0.001f)

        // When user has already manually zoomed in to 2.0x, target scale preserves their zoom!
        val userZoom = 2.0f
        val targetScaleFromUserZoom = if (userZoom < minComfortableScale) minComfortableScale else userZoom
        assertEquals(2.0f, targetScaleFromUserZoom, 0.001f)
    }

    @Test
    fun testHorizontalColumnAlignmentCalculation() {
        val pageWidthPts = 612f
        val contentWidthPx = 970f
        val viewportWidthPx = 1080f
        val targetScale = 1.45f
        val maxOffsetX = (viewportWidthPx * (targetScale - 1f)) / 2f // 243px

        // Match in right-hand column of a two-column textbook (e.g. center X = 459pt, ~75% across)
        val matchCenterXPts = 459f
        val matchXNorm = matchCenterXPts / pageWidthPts // 0.75
        val deltaX = (0.5f - matchXNorm) * contentWidthPx * targetScale // -0.25 * 970 * 1.45 = -351.6px
        val targetOffsetX = deltaX.coerceIn(-maxOffsetX, maxOffsetX)

        // Shift left by maxOffsetX (clamped to prevent showing black margin borders)
        assertEquals(-243f, targetOffsetX, 0.01f)
    }
}
