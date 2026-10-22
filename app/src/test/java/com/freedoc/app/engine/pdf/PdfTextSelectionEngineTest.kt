package com.freedoc.app.engine.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfTextSelectionEngineTest {

    private fun createSampleContent(): PdfTextExtractor.PageTextContent {
        // Multi-line text sample:
        // Line 1: "Selected text refers to" (top = 100f, bottom = 114f)
        // Line 2: "the portion of content" (top = 120f, bottom = 134f)
        val line1 = "Selected text refers to"
        val line2 = "the portion of content"
        val fullText = "$line1\n$line2"

        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        val charW = 8f
        val line1H = 14f

        for (ch in line1) {
            if (ch == ' ') {
                bounds.add(null)
            } else {
                bounds.add(HighlightRect(curX, 100f, curX + charW, 100f + line1H))
            }
            curX += charW
        }

        // newline
        bounds.add(null)

        curX = 50f
        val line2H = 14f
        for (ch in line2) {
            if (ch == ' ') {
                bounds.add(null)
            } else {
                bounds.add(HighlightRect(curX, 120f, curX + charW, 120f + line2H))
            }
            curX += charW
        }

        return PdfTextExtractor.PageTextContent(
            text = fullText,
            charBounds = bounds
        )
    }

    @Test
    fun testHitTestCharExact() {
        val content = createSampleContent()
        // 'S' is at index 0, left=50, top=100, right=58, bottom=114
        val charIndex = PdfTextSelectionEngine.hitTestChar(content, 54f, 105f)
        assertEquals(0, charIndex)
        assertEquals('S', content.text[charIndex])
    }

    @Test
    fun testHitTestCharWithinToleranceOnSameLine() {
        val content = createSampleContent()
        // Tap slightly above 'e' (index 1: left=58, right=66)
        val charIndex = PdfTextSelectionEngine.hitTestChar(content, 60f, 96f)
        assertEquals(1, charIndex)
        assertEquals('e', content.text[charIndex])
    }

    @Test
    fun testFindWordRangeForSingleWord() {
        val content = createSampleContent()
        // Tap on 'l' in "Selected" (index 2)
        val wordRange = PdfTextSelectionEngine.findWordRange(content.text, 2)
        assertNotNull(wordRange)
        val (start, end) = wordRange!!
        assertEquals(0, start)
        assertEquals(8, end)
        assertEquals("Selected", content.text.substring(start, end))
    }

    @Test
    fun testFindWordRangeForSecondWord() {
        val content = createSampleContent()
        // "text" starts at index 9: 't', 'e', 'x', 't' -> [9, 13)
        val wordRange = PdfTextSelectionEngine.findWordRange(content.text, 10)
        assertNotNull(wordRange)
        val (start, end) = wordRange!!
        assertEquals(9, start)
        assertEquals(13, end)
        assertEquals("text", content.text.substring(start, end))
    }

    @Test
    fun testCreateSelectionSingleWord() {
        val content = createSampleContent()
        // Select "Selected" (indices 0 to 8)
        val selection = PdfTextSelectionEngine.createSelection(0, content, 0, 8)
        assertNotNull(selection)
        assertEquals(0, selection!!.pageIndex)
        assertEquals(0, selection.startIndex)
        assertEquals(8, selection.endIndex)
        assertEquals("Selected", selection.text)
        assertFalse(selection.isMultiLine)
        assertEquals(1, selection.lineRects.size)

        val rect = selection.lineRects[0]
        assertEquals(50f, rect.left, 0.01f)
        assertEquals(50f + (8 * 8f), rect.right, 0.01f)
        assertEquals(100f, rect.top, 0.01f)
        assertEquals(114f, rect.bottom, 0.01f)

        // 8 characters (all printable, no whitespace)
        assertEquals(8, selection.selectedChars.size)
        assertEquals('S', selection.selectedChars[0].char)
    }

    @Test
    fun testCreateSelectionMultipleWordsOnSameLineMatchesReferenceImage() {
        val content = createSampleContent()
        // Select "Selected text" (indices 0 to 13)
        val selection = PdfTextSelectionEngine.createSelection(0, content, 0, 13)
        assertNotNull(selection)
        assertEquals("Selected text", selection!!.text)
        assertFalse(selection.isMultiLine)
        assertEquals(1, selection.lineRects.size)

        val rect = selection.lineRects[0]
        assertEquals(50f, rect.left, 0.01f)
        // Rect covers from start of "Selected" (50f) to end of "text" (50f + 13 * 8f = 154f)
        assertEquals(154f, rect.right, 0.01f)
        assertEquals(100f, rect.top, 0.01f)
        assertEquals(114f, rect.bottom, 0.01f)

        // Selected characters exclude space
        assertEquals(12, selection.selectedChars.size)
    }

    @Test
    fun testCreateMultiLineSelectionProducesIndependentLineRectangles() {
        val content = createSampleContent()
        // Select from "refers to" on Line 1 through "the portion" on Line 2
        // "refers" starts at index 14
        // Line 2: "the portion" ends at index 35
        val selection = PdfTextSelectionEngine.createSelection(0, content, 14, 35)
        assertNotNull(selection)
        assertTrue(selection!!.isMultiLine)
        assertEquals(2, selection.lineRects.size)

        val line1Rect = selection.lineRects[0]
        val line2Rect = selection.lineRects[1]

        // Each line rectangle must follow its own vertical bounds and horizontal extent
        assertEquals(100f, line1Rect.top, 0.01f)
        assertEquals(114f, line1Rect.bottom, 0.01f)

        assertEquals(120f, line2Rect.top, 0.01f)
        assertEquals(134f, line2Rect.bottom, 0.01f)
        assertEquals(50f, line2Rect.left, 0.01f)
    }

    @Test
    fun testAdjustSelectionBoundaryWithStartHandle() {
        val content = createSampleContent()
        // Initial selection: "text" (indices 9 to 13)
        val initialSel = PdfTextSelectionEngine.createSelection(0, content, 9, 13)!!

        // Drag start handle backward to index 0 ("Selected text")
        val expanded = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = initialSel,
            content = content,
            targetCharIndex = 0,
            isStartHandle = true
        )
        assertNotNull(expanded)
        assertEquals(0, expanded!!.startIndex)
        assertEquals(13, expanded.endIndex)
        assertEquals("Selected text", expanded.text)
    }

    @Test
    fun testAdjustSelectionBoundaryWithEndHandle() {
        val content = createSampleContent()
        // Initial selection: "Selected" (indices 0 to 8)
        val initialSel = PdfTextSelectionEngine.createSelection(0, content, 0, 8)!!

        // Drag end handle forward to index 12 ('t' of "text")
        val expanded = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = initialSel,
            content = content,
            targetCharIndex = 12,
            isStartHandle = false
        )
        assertNotNull(expanded)
        assertEquals(0, expanded!!.startIndex)
        assertEquals(13, expanded.endIndex)
        assertEquals("Selected text", expanded.text)
    }

    @Test
    fun testPartialWordSelectionSupported() {
        val content = createSampleContent()
        // Select partial word: "elect" from "Selected" (indices 1 to 6)
        val selection = PdfTextSelectionEngine.createSelection(0, content, 1, 6)
        assertNotNull(selection)
        assertEquals("elect", selection!!.text)
        assertEquals(5, selection.selectedChars.size)
        assertEquals(50f + 8f, selection.lineRects[0].left, 0.01f)
        assertEquals(50f + 6 * 8f, selection.lineRects[0].right, 0.01f)
    }

    @Test
    fun testEmptyOrInvalidSelectionReturnsNull() {
        val content = createSampleContent()
        assertNull(PdfTextSelectionEngine.createSelection(0, content, 5, 5))
        assertNull(PdfTextSelectionEngine.createSelection(0, content, 200, 205))
    }

    @Test
    fun testCoordinateMappingUnderZoomAndScaling() {
        val content = createSampleContent()
        // Simulate a page displayed at scaleX = 2.0f, scaleY = 2.0f (e.g. 2x zoom or display DPI scaling)
        val scaleX = 2.0f
        val scaleY = 2.0f

        // 'S' is at left = 50f, top = 100f, right = 58f, bottom = 114f in PDF points
        // In screen Canvas pixels under 2x scale:
        // leftPx = 100f, topPx = 200f, rightPx = 116f, bottomPx = 228f
        val touchPxX = 105f
        val touchPxY = 210f

        // Touch coordinate mapping from canvas pixels back to PDF points:
        val mappedPdfX = touchPxX / scaleX
        val mappedPdfY = touchPxY / scaleY

        val charIndex = PdfTextSelectionEngine.hitTestChar(content, mappedPdfX, mappedPdfY)
        assertEquals(0, charIndex)
        assertEquals('S', content.text[charIndex])

        // When zoomed in 3.5x:
        val zoom35X = 3.5f
        val zoom35Y = 3.5f
        // Tap on 't' in "text" (index 9: left = 50f + 9*8f = 122f, top = 100f)
        val screenX35 = 124f * zoom35X
        val screenY35 = 106f * zoom35Y

        val mappedPdfX35 = screenX35 / zoom35X
        val mappedPdfY35 = screenY35 / zoom35Y
        val hitChar35 = PdfTextSelectionEngine.hitTestChar(content, mappedPdfX35, mappedPdfY35)
        assertEquals(9, hitChar35)
        assertEquals('t', content.text[hitChar35])
    }

    @Test
    fun testSelectionClearingAndReplacement() {
        val content = createSampleContent()

        // 1. Initial selection: "Selected"
        var activeSelection: PdfSelection? = PdfTextSelectionEngine.createSelection(0, content, 0, 8)
        assertNotNull(activeSelection)
        assertEquals("Selected", activeSelection!!.text)

        // 2. Clear selection
        activeSelection = null
        assertNull(activeSelection)

        // 3. New independent selection: "portion" (Line 2: indices 28 to 35)
        val wordRange = PdfTextSelectionEngine.findWordRange(content.text, 30)
        assertNotNull(wordRange)
        activeSelection = PdfTextSelectionEngine.createSelection(0, content, wordRange!!.first, wordRange.second)
        assertNotNull(activeSelection)
        assertEquals("portion", activeSelection!!.text)
        assertEquals(1, activeSelection.lineRects.size)
        assertEquals(120f, activeSelection.lineRects[0].top, 0.01f)
    }

    @Test
    fun testSeparationBetweenSearchHighlightingAndTextSelection() {
        val content = createSampleContent()
        val pageContents = mapOf(0 to content)

        // 1. Search for "text"
        val searchMatches = PdfSearchEngine.search("text", pageContents, documentSessionId = "session_separation")
        assertEquals(1, searchMatches.size)
        val searchMatch = searchMatches[0]
        assertEquals("text", searchMatch.matchedText)
        assertEquals(0, searchMatch.pageIndex)

        // 2. User selects "Selected"
        val userSelection = PdfTextSelectionEngine.createSelection(0, content, 0, 8)
        assertNotNull(userSelection)
        assertEquals("Selected", userSelection!!.text)

        // Assert search match and user selection have distinct geometries and data structures
        assertFalse(searchMatch.matchedText == userSelection.text)
        assertEquals(9, searchMatch.startIndexOnPage)
        assertEquals(0, userSelection.startIndex)
        assertEquals(13, searchMatch.endIndexOnPage)
        assertEquals(8, userSelection.endIndex)

        // Search match highlightRect does not overlap user selection lineRect
        val searchRect = searchMatch.highlightRects[0]
        val selectionRect = userSelection.lineRects[0]
        assertTrue(selectionRect.right <= searchRect.left)
    }

    @Test
    fun testWordBoundariesTrimPunctuationAndParentheses() {
        val text = "Hello, (FreeDoc) is an offline-first, state-of-the-art reader!"
        
        // Tap "Hello" (before comma)
        val helloRange = PdfTextSelectionEngine.findWordRange(text, 2)
        assertNotNull(helloRange)
        assertEquals("Hello", text.substring(helloRange!!.first, helloRange.second))

        // Tap "FreeDoc" inside parentheses
        val freedocIndex = text.indexOf("FreeDoc") + 2
        val freedocRange = PdfTextSelectionEngine.findWordRange(text, freedocIndex)
        assertNotNull(freedocRange)
        assertEquals("FreeDoc", text.substring(freedocRange!!.first, freedocRange.second))

        // Tap "offline-first" (internal hyphen should be preserved, trailing comma trimmed)
        val offlineIndex = text.indexOf("offline") + 2
        val offlineRange = PdfTextSelectionEngine.findWordRange(text, offlineIndex)
        assertNotNull(offlineRange)
        assertEquals("offline-first", text.substring(offlineRange!!.first, offlineRange.second))

        // Tap standalone exclamation mark at the end
        val exclIndex = text.indexOf("!")
        val exclRange = PdfTextSelectionEngine.findWordRange(text, exclIndex)
        assertNotNull(exclRange)
        assertEquals("!", text.substring(exclRange!!.first, exclRange.second))
    }

    @Test
    fun testCrossingSelectionHandlesWithFixedAnchor() {
        val content = createSampleContent()
        // Selection: "text" (indices 9 to 13). Fixed anchor for start handle drag is 13.
        val initialSel = PdfTextSelectionEngine.createSelection(0, content, 9, 13)!!

        // Drag start handle forward past the end handle to index 17 ('e' in "refers")
        val anchor = initialSel.endIndex // 13
        val crossed = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = initialSel,
            content = content,
            targetCharIndex = 17,
            isStartHandle = true,
            anchorCharIndex = anchor
        )
        assertNotNull(crossed)
        assertEquals(13, crossed!!.startIndex)
        assertEquals(18, crossed.endIndex)
        assertEquals(" refe", crossed.text)

        // Now drag back before the anchor to index 0
        val uncrossed = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = crossed,
            content = content,
            targetCharIndex = 0,
            isStartHandle = true,
            anchorCharIndex = anchor
        )
        assertNotNull(uncrossed)
        assertEquals(0, uncrossed!!.startIndex)
        assertEquals(13, uncrossed.endIndex)
        assertEquals("Selected text", uncrossed.text)
    }

    @Test
    fun testLargeWhitespaceGapSplitsIntoIndependentLineRectangles() {
        // Line with "Left" at x=50..82, a 100pt gap, and "Right" at x=182..222
        val text = "Left Right"
        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        for (ch in "Left") {
            bounds.add(HighlightRect(curX, 100f, curX + 8f, 114f))
            curX += 8f
        }
        bounds.add(null) // space
        curX = 182f // large gap > 28pt
        for (ch in "Right") {
            bounds.add(HighlightRect(curX, 100f, curX + 8f, 114f))
            curX += 8f
        }

        val content = PdfTextExtractor.PageTextContent(text = text, charBounds = bounds)
        val selection = PdfTextSelectionEngine.createSelection(0, content, 0, text.length)
        assertNotNull(selection)
        // Must produce 2 distinct lineRects on the same line to avoid highlighting the empty gap!
        assertEquals(2, selection!!.lineRects.size)
        assertEquals(50f, selection.lineRects[0].left, 0.01f)
        assertEquals(82f, selection.lineRects[0].right, 0.01f)
        assertEquals(182f, selection.lineRects[1].left, 0.01f)
        assertEquals(222f, selection.lineRects[1].right, 0.01f)
    }

    @Test
    fun testMissingCharacterGeometryFailsGracefully() {
        // Content with text but null geometry
        val content = PdfTextExtractor.PageTextContent(
            text = "Scanned document with missing glyph bounds",
            charBounds = List(42) { null }
        )
        assertEquals(-1, PdfTextSelectionEngine.hitTestChar(content, 100f, 100f))
        assertNull(PdfTextSelectionEngine.createSelection(0, content, 0, 10))
    }

    @Test
    fun testStartAndEndHandlesPositionedAccuratelyOnGlyphs() {
        val content = createSampleContent()
        // Select "Selected" (indices 0 to 8)
        val selection = PdfTextSelectionEngine.createSelection(0, content, 0, 8)
        assertNotNull(selection)
        assertNotNull(selection!!.startHandleRect)
        assertNotNull(selection.endHandleRect)

        // 'S' is at 50f..58f, top=100f, bottom=114f
        assertEquals(50f, selection.startHandleRect!!.left, 0.01f)
        assertEquals(100f, selection.startHandleRect!!.top, 0.01f)
        assertEquals(114f, selection.startHandleRect!!.bottom, 0.01f)

        // 'd' (char index 7) is at 50f + 7*8f = 106f..114f
        assertEquals(106f, selection.endHandleRect!!.left, 0.01f)
        assertEquals(114f, selection.endHandleRect!!.right, 0.01f)
    }

    @Test
    fun testShortFinalLineHitTestingWhenPointerIsHorizontallyBeyond() {
        // Line 1: Long line spanning x=50f..578f, y=100f..114f
        val line1 = "This is a long second-to-last line of text that reaches far across"
        // Line 2: Short line spanning x=50f..130f, y=130f..144f
        val line2 = "Short end."
        val fullText = "$line1\n$line2"

        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        val charW = 8f
        for (ch in line1) {
            if (ch == ' ') bounds.add(null)
            else bounds.add(HighlightRect(curX, 100f, curX + charW, 114f))
            curX += charW
        }
        bounds.add(null) // \n

        curX = 50f
        for (ch in line2) {
            if (ch == ' ') bounds.add(null)
            else bounds.add(HighlightRect(curX, 130f, curX + charW, 144f))
            curX += charW
        }

        val content = PdfTextExtractor.PageTextContent(text = fullText, charBounds = bounds)
        val lastCharOnLine2Index = fullText.length - 1
        assertEquals('.', fullText[lastCharOnLine2Index])

        // 1. Pointer at x=400f, y=137f: vertically on the short final line (y in 130..144),
        // but horizontally far beyond its text (ends at 130f). Preceding Line 1 has characters at x=400f.
        // Line-first hit-testing must resolve to the last character of Line 2, NOT a character on Line 1!
        val hitIndex = PdfTextSelectionEngine.hitTestChar(content, x = 400f, y = 137f)
        assertEquals(lastCharOnLine2Index, hitIndex)
        assertEquals('.', fullText[hitIndex])

        // 2. Pointer slightly below Line 2 and horizontally beyond (x=500f, y=148f)
        val hitBelowIndex = PdfTextSelectionEngine.hitTestChar(content, x = 500f, y = 148f)
        assertEquals(lastCharOnLine2Index, hitBelowIndex)
        assertEquals('.', fullText[hitBelowIndex])

        // 3. Pointer before the start of Line 2 (x=20f, y=137f)
        val firstCharOnLine2Index = line1.length + 1 // 'S' in "Short end."
        assertEquals('S', fullText[firstCharOnLine2Index])
        val hitBeforeIndex = PdfTextSelectionEngine.hitTestChar(content, x = 20f, y = 137f)
        assertEquals(firstCharOnLine2Index, hitBeforeIndex)
    }

    @Test
    fun testDraggingSelectionHandleToShortFinalLineExpandsAndContractsCorrectly() {
        val line1 = "This is a long second-to-last line of text that reaches far across"
        val line2 = "Short end."
        val fullText = "$line1\n$line2"

        val bounds = ArrayList<HighlightRect?>()
        var curX = 50f
        val charW = 8f
        for (ch in line1) {
            if (ch == ' ') bounds.add(null)
            else bounds.add(HighlightRect(curX, 100f, curX + charW, 114f))
            curX += charW
        }
        bounds.add(null)

        curX = 50f
        for (ch in line2) {
            if (ch == ' ') bounds.add(null)
            else bounds.add(HighlightRect(curX, 130f, curX + charW, 144f))
            curX += charW
        }

        val content = PdfTextExtractor.PageTextContent(text = fullText, charBounds = bounds)

        // 1. Initial selection: "This is a long" on Line 1 (indices 0 to 14)
        val initialSel = PdfTextSelectionEngine.createSelection(0, content, 0, 14)!!
        assertEquals("This is a long", initialSel.text)
        assertFalse(initialSel.isMultiLine)

        // 2. Drag end handle vertically downward to the short final line at x=380f, y=137f
        val hitChar = PdfTextSelectionEngine.hitTestChar(content, x = 380f, y = 137f)
        val expanded = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = initialSel,
            content = content,
            targetCharIndex = hitChar,
            isStartHandle = false,
            anchorCharIndex = 0
        )
        assertNotNull(expanded)
        assertTrue(expanded!!.isMultiLine)
        assertEquals(2, expanded.lineRects.size)
        // Must include all text through "Short end."
        assertTrue(expanded.text.endsWith("Short end."))
        assertEquals(fullText.length, expanded.endIndex)

        // Visible end handle must be at the '.' on the short final line
        assertNotNull(expanded.endHandleRect)
        assertEquals(130f, expanded.endHandleRect!!.right, 0.01f)
        assertEquals(130f, expanded.endHandleRect!!.top, 0.01f)

        // 3. Reverse drag: contract selection back up to Line 1 (x=250f, y=107f)
        val hitCharUp = PdfTextSelectionEngine.hitTestChar(content, x = 250f, y = 107f)
        val contracted = PdfTextSelectionEngine.adjustSelectionBoundary(
            currentSelection = expanded,
            content = content,
            targetCharIndex = hitCharUp,
            isStartHandle = false,
            anchorCharIndex = 0
        )
        assertNotNull(contracted)
        assertFalse(contracted!!.isMultiLine)
        assertEquals(1, contracted.lineRects.size)
        assertEquals(100f, contracted.lineRects[0].top, 0.01f)
    }
}
