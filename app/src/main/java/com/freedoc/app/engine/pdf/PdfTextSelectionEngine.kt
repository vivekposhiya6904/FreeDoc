package com.freedoc.app.engine.pdf

/**
 * Geometric bounds and character value of an individual selected glyph on a PDF page.
 */
data class SelectedChar(
    val char: Char,
    val bounds: HighlightRect
)

/**
 * Encapsulates active user text selection on a specific PDF page.
 *
 * @param pageIndex 0-based page index where text is selected.
 * @param startIndex 0-based start character offset in page text (inclusive).
 * @param endIndex 0-based end character offset in page text (exclusive).
 * @param text The exact selected text content.
 * @param lineRects Line-grouped bounding rectangles in typographic points for drawing the blue background.
 * @param selectedChars Character glyphs and exact bounds for reference or auxiliary operations.
 * @param startHandleRect Exact character bounding box for placing the start cursor and handle pin.
 * @param endHandleRect Exact character bounding box for placing the end cursor and handle pin.
 */
data class PdfSelection(
    val pageIndex: Int,
    val startIndex: Int,
    val endIndex: Int,
    val text: String,
    val lineRects: List<HighlightRect>,
    val selectedChars: List<SelectedChar> = emptyList(),
    val startHandleRect: HighlightRect? = null,
    val endHandleRect: HighlightRect? = null
) {
    val charCount: Int get() = text.length
    val isMultiLine: Boolean get() = lineRects.size > 1
}

/**
 * Engine responsible for hit-testing touch coordinates to PDF character geometry,
 * detecting word boundaries, computing multi-line selection rectangles, and managing
 * selection expansion and boundary adjustments.
 */
object PdfTextSelectionEngine {

    /**
     * Hit-tests a point ([x], [y]) in typographic points against extracted page character geometry.
     *
     * @return 0-based character index if a glyph is found within tolerance, or -1 otherwise.
     */
    private data class VisualChar(val index: Int, val rect: HighlightRect)

    private class VisualLine(
        val chars: MutableList<VisualChar>,
        var top: Float,
        var bottom: Float,
        var left: Float,
        var right: Float
    )

    /**
     * Hit-tests a point ([x], [y]) in typographic points against extracted page character geometry.
     * Uses a line-first geometric approach:
     * 1. Direct hit-test inside character bounding box.
     * 2. Identifies the nearest visual text line by vertical distance first.
     * 3. Within that line, resolves [x] to:
     *    - The first character if [x] is to the left of the line.
     *    - The last character if [x] is horizontally beyond the line (fixes short final line selection!).
     *    - The closest character horizontally if [x] is within the line's span.
     *
     * @return 0-based character index if found within tolerance, or -1 otherwise.
     */
    fun hitTestChar(
        content: PdfTextExtractor.PageTextContent,
        x: Float,
        y: Float,
        verticalTolerance: Float = 12f,
        horizontalTolerance: Float = 24f
    ): Int {
        if (content.text.isEmpty() || content.charBounds.isEmpty()) return -1

        // 1. Direct hit test within bounding box
        for (i in content.text.indices) {
            val rect = content.charBounds.getOrNull(i) ?: continue
            if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                return i
            }
        }

        // 2. Build visual lines from extracted non-null character bounds
        val visualLines = mutableListOf<VisualLine>()
        var currentLine: VisualLine? = null

        for (i in content.text.indices) {
            val rect = content.charBounds.getOrNull(i) ?: continue
            val cur = currentLine
            if (cur != null) {
                val lastRect = cur.chars.last().rect
                val verticalOverlap = minOf(lastRect.bottom, rect.bottom) - maxOf(lastRect.top, rect.top)
                val minHeight = minOf(lastRect.height, rect.height).coerceAtLeast(4f)
                val isSameLine = verticalOverlap > (minHeight * 0.40f)

                // Large horizontal gap indicates separate column or distinct tab block
                val maxIntraLineGap = maxOf(lastRect.height * 2.5f, 35f)
                val isSameSegment = (rect.left - lastRect.right) <= maxIntraLineGap

                if (isSameLine && isSameSegment) {
                    cur.chars.add(VisualChar(i, rect))
                    cur.top = minOf(cur.top, rect.top)
                    cur.bottom = maxOf(cur.bottom, rect.bottom)
                    cur.left = minOf(cur.left, rect.left)
                    cur.right = maxOf(cur.right, rect.right)
                } else {
                    val newLine = VisualLine(
                        chars = mutableListOf(VisualChar(i, rect)),
                        top = rect.top,
                        bottom = rect.bottom,
                        left = rect.left,
                        right = rect.right
                    )
                    visualLines.add(newLine)
                    currentLine = newLine
                }
            } else {
                val newLine = VisualLine(
                    chars = mutableListOf(VisualChar(i, rect)),
                    top = rect.top,
                    bottom = rect.bottom,
                    left = rect.left,
                    right = rect.right
                )
                visualLines.add(newLine)
                currentLine = newLine
            }
        }

        if (visualLines.isEmpty()) return -1

        // 3. Find the best visual line by vertical proximity
        var bestLine: VisualLine? = null
        var minVDist = Float.MAX_VALUE
        var minHDistForBestVDist = Float.MAX_VALUE

        for (line in visualLines) {
            val vDist = when {
                y < line.top -> line.top - y
                y > line.bottom -> y - line.bottom
                else -> 0f
            }

            val hDist = when {
                x < line.left -> line.left - x
                x > line.right -> x - line.right
                else -> 0f
            }

            // A line where y is vertically inside (vDist == 0) is always prioritized.
            // If multiple lines have vDist == 0 (e.g. columns on same line), break tie by horizontal distance.
            // If no line has vDist == 0, select the line with minimum vDist, breaking ties by hDist.
            if (vDist < minVDist - 0.01f) {
                minVDist = vDist
                minHDistForBestVDist = hDist
                bestLine = line
            } else if (kotlin.math.abs(vDist - minVDist) <= 0.01f) {
                if (hDist < minHDistForBestVDist) {
                    minHDistForBestVDist = hDist
                    bestLine = line
                }
            }
        }

        // Check vertical threshold: don't select if touch is far away in empty page margins
        val maxAllowedVDist = maxOf(verticalTolerance * 3.5f, 45f)
        if (bestLine == null || minVDist > maxAllowedVDist) {
            return -1
        }

        // 4. Within the best visual line, resolve to the appropriate character:
        // - If x is before the start of the line, resolve to first character.
        // - If x is beyond the end of the line (e.g. short final line!), resolve to last character.
        // - Otherwise, find the character closest to x.
        val targetLine = bestLine
        return when {
            x <= targetLine.left -> targetLine.chars.minByOrNull { it.rect.left }!!.index
            x >= targetLine.right -> targetLine.chars.maxByOrNull { it.rect.right }!!.index
            else -> {
                targetLine.chars.minByOrNull { vc ->
                    val r = vc.rect
                    when {
                        x < r.left -> r.left - x
                        x > r.right -> x - r.right
                        else -> 0f
                    }
                }!!.index
            }
        }
    }

    /**
     * Finds the [start, end) index range of the word containing [charIndex].
     * Expands outwards while handling alphanumeric characters, internal contractions/hyphens,
     * and strictly trims leading/trailing punctuation and whitespace.
     */
    fun findWordRange(text: String, charIndex: Int): Pair<Int, Int>? {
        if (text.isEmpty() || charIndex !in text.indices) return null

        var targetIndex = charIndex
        // If user tapped a whitespace or newline, snap to adjacent word character if available
        if (text[targetIndex].isWhitespace() || text[targetIndex] == '\n' || text[targetIndex] == '\r') {
            if (targetIndex > 0 && isWordChar(text[targetIndex - 1])) {
                targetIndex--
            } else if (targetIndex < text.length - 1 && isWordChar(text[targetIndex + 1])) {
                targetIndex++
            } else {
                return null
            }
        }

        // If target is standalone punctuation or symbol, select just that symbol
        if (!isWordChar(text[targetIndex])) {
            return Pair(targetIndex, targetIndex + 1)
        }

        // Expand start backwards
        var start = targetIndex
        while (start > 0 && isWordCharWithContext(text, start - 1, isForward = false)) {
            start--
        }

        // Expand end forwards
        var end = targetIndex + 1
        while (end < text.length && isWordCharWithContext(text, end, isForward = true)) {
            end++
        }

        // Trim any accidental leading/trailing connector punctuation (e.g. hyphen or apostrophe at edge)
        while (start < end && isEdgePunctuation(text[start])) {
            start++
        }
        while (end > start && isEdgePunctuation(text[end - 1])) {
            end--
        }

        return if (start < end) Pair(start, end) else Pair(targetIndex, targetIndex + 1)
    }

    /**
     * Checks if character [c] is a primary word constituent (alphanumeric or underscore).
     */
    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_'
    }

    /**
     * Checks if character at [index] should be included as part of the word,
     * allowing internal apostrophes (e.g. "don't") and hyphens (e.g. "offline-first")
     * when surrounded by letters or digits.
     */
    private fun isWordCharWithContext(text: String, index: Int, isForward: Boolean): Boolean {
        val c = text[index]
        if (isWordChar(c)) return true

        // Allow internal apostrophes or hyphens only if flanked by word characters
        if (c == '\'' || c == '’' || c == '-') {
            val hasPrev = index > 0 && isWordChar(text[index - 1])
            val hasNext = index < text.length - 1 && isWordChar(text[index + 1])
            if (hasPrev && hasNext) return true
        }

        return false
    }

    private fun isEdgePunctuation(c: Char): Boolean {
        return c == '\'' || c == '’' || c == '-' || c == '_' || !c.isLetterOrDigit()
    }

    /**
     * Creates a [PdfSelection] for the given [startIndex] and [endIndex] character range on [pageIndex].
     * Groups character bounding boxes into clean line rectangles and captures handle anchors.
     */
    fun createSelection(
        pageIndex: Int,
        content: PdfTextExtractor.PageTextContent,
        startIndex: Int,
        endIndex: Int
    ): PdfSelection? {
        val start = minOf(startIndex, endIndex).coerceIn(0, content.text.length)
        val end = maxOf(startIndex, endIndex).coerceIn(0, content.text.length)
        if (start >= end) return null

        val selectedText = content.text.substring(start, end)
        val selectedChars = mutableListOf<SelectedChar>()
        val boundsSlice = mutableListOf<HighlightRect>()

        for (i in start until end) {
            val rect = content.charBounds.getOrNull(i)
            if (rect != null) {
                boundsSlice.add(rect)
                val ch = content.text[i]
                if (!ch.isWhitespace() && ch != '\n' && ch != '\r') {
                    selectedChars.add(SelectedChar(char = ch, bounds = rect))
                }
            }
        }

        if (boundsSlice.isEmpty()) return null

        // Group into visual lines using geometric vertical overlap
        val lines = mutableListOf<MutableList<HighlightRect>>()
        for (rect in boundsSlice) {
            val currentLine = lines.lastOrNull()
            if (currentLine != null) {
                val lastRect = currentLine.last()
                val verticalOverlap = minOf(lastRect.bottom, rect.bottom) - maxOf(lastRect.top, rect.top)
                val minHeight = minOf(lastRect.height, rect.height).coerceAtLeast(4f)
                val isSameLine = verticalOverlap > (minHeight * 0.40f)

                // Also check horizontal gap on the same line to avoid highlighting massive empty voids
                val maxIntraLineGap = maxOf(lastRect.height * 2.2f, 28f)
                val horizontalGap = rect.left - lastRect.right
                val hasLargeGap = horizontalGap > maxIntraLineGap

                if (isSameLine && !hasLargeGap) {
                    currentLine.add(rect)
                } else {
                    lines.add(mutableListOf(rect))
                }
            } else {
                lines.add(mutableListOf(rect))
            }
        }

        val lineRects = lines.map { line ->
            HighlightRect(
                left = line.minOf { it.left },
                top = line.minOf { it.top },
                right = line.maxOf { it.right },
                bottom = line.maxOf { it.bottom }
            )
        }

        val firstCharBound = boundsSlice.firstOrNull()
        val lastCharBound = boundsSlice.lastOrNull()

        return PdfSelection(
            pageIndex = pageIndex,
            startIndex = start,
            endIndex = end,
            text = selectedText,
            lineRects = lineRects,
            selectedChars = selectedChars,
            startHandleRect = firstCharBound,
            endHandleRect = lastCharBound
        )
    }

    /**
     * Adjusts the start or end boundary of an existing selection to [targetCharIndex].
     * Supports handle crossing and anchored dragging without invalid ranges.
     */
    fun adjustSelectionBoundary(
        currentSelection: PdfSelection,
        content: PdfTextExtractor.PageTextContent,
        targetCharIndex: Int,
        isStartHandle: Boolean,
        anchorCharIndex: Int? = null
    ): PdfSelection? {
        val safeTarget = targetCharIndex.coerceIn(0, content.text.length)
        val anchor = (anchorCharIndex ?: if (isStartHandle) currentSelection.endIndex else currentSelection.startIndex)
            .coerceIn(0, content.text.length)

        val newStart: Int
        val newEnd: Int

        if (isStartHandle) {
            if (safeTarget < anchor) {
                newStart = safeTarget
                newEnd = anchor
            } else {
                newStart = anchor
                newEnd = (safeTarget + 1).coerceAtMost(content.text.length)
            }
        } else {
            if (safeTarget >= anchor) {
                newStart = anchor
                newEnd = (safeTarget + 1).coerceAtMost(content.text.length)
            } else {
                newStart = safeTarget
                newEnd = anchor
            }
        }

        return createSelection(currentSelection.pageIndex, content, newStart, newEnd)
    }
}
