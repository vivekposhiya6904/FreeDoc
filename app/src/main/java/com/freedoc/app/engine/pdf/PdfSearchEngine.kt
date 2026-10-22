package com.freedoc.app.engine.pdf

/**
 * Geometric bounding box of a text occurrence on a PDF page in typographic points (1/72 inch).
 * Coordinate system: origin at top-left of page, X increases to the right, Y increases downwards.
 */
data class HighlightRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

/**
 * Represents a single search match in an opened PDF document.
 */
data class SearchMatch(
    val documentSessionId: String = "",
    val pageIndex: Int,
    val matchIndexInDocument: Int,
    val totalMatchesInDocument: Int,
    val startIndexOnPage: Int = 0,
    val endIndexOnPage: Int = 0,
    val matchedText: String = "",
    val snippet: String = "",
    val highlightRects: List<HighlightRect> = emptyList()
) {
    val displayPageNumber: Int get() = pageIndex + 1
}

/**
 * Engine for performing case-insensitive text search and result indexing across PDF pages.
 *
 * Supports:
 * - Case-insensitive matching by default ("meaning", "Meaning", "MEANING")
 * - Leading and trailing whitespace stripping
 * - Query words spanning across line endings or extra whitespace within a page
 * - Multiple occurrences on the same page
 * - Occurrences across multiple pages
 * - Contextual snippet generation with ellipsis
 */
object PdfSearchEngine {

    /**
     * Searches [query] across [pages] (mapping 0-based page index to extracted page text).
     *
     * If [pageContents] is supplied, exact geometric [HighlightRect] bounding boxes are computed
     * for each match, grouping characters by line for multi-line and multi-word queries.
     *
     * Matching is case-insensitive. Empty or whitespace queries immediately return an empty list.
     */
    fun search(
        query: String,
        pageContents: Map<Int, PdfTextExtractor.PageTextContent>,
        documentSessionId: String = ""
    ): List<SearchMatch> = search(
        query = query,
        pages = pageContents.mapValues { it.value.text },
        pageContents = pageContents,
        documentSessionId = documentSessionId
    )

    fun search(
        query: String,
        pages: Map<Int, String>,
        pageContents: Map<Int, PdfTextExtractor.PageTextContent> = emptyMap(),
        documentSessionId: String = ""
    ): List<SearchMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || pages.isEmpty()) return emptyList()

        // Build pattern that allows flexible whitespace between query words
        val words = trimmed.split(Regex("""\s+"""))
        val regexPattern = words.joinToString(separator = """\s+""") { Regex.escape(it) }
        val regex = Regex(regexPattern, setOf(RegexOption.IGNORE_CASE))

        val allMatches = mutableListOf<SearchMatch>()
        val sortedPages = pages.keys.sorted()

        for (pageIndex in sortedPages) {
            val pageText = pages[pageIndex] ?: continue
            val content = pageContents[pageIndex]
            val matchResults = regex.findAll(pageText).toList()

            for (match in matchResults) {
                val range = match.range
                val matchedText = match.value

                val snippetStart = (range.first - 30).coerceAtLeast(0)
                val snippetEnd = (range.last + 1 + 30).coerceAtMost(pageText.length)
                var snippet = pageText.substring(snippetStart, snippetEnd)
                    .replace('\n', ' ')
                    .replace(Regex("""\s+"""), " ")
                    .trim()

                if (snippetStart > 0) snippet = "…$snippet"
                if (snippetEnd < pageText.length) snippet = "$snippet…"

                // Resolve line-grouped highlight rectangles from character bounds
                val highlightRects = mutableListOf<HighlightRect>()
                if (content != null && content.charBounds.isNotEmpty()) {
                    val boundsSlice = (range.first..range.last)
                        .mapNotNull { idx -> if (idx in content.charBounds.indices) content.charBounds[idx] else null }

                    if (boundsSlice.isNotEmpty()) {
                        val lines = mutableListOf<MutableList<HighlightRect>>()
                        for (rect in boundsSlice) {
                            val currentLine = lines.lastOrNull()
                            if (currentLine != null && kotlin.math.abs(currentLine.last().top - rect.top) < (rect.height * 0.5f).coerceAtLeast(3f)) {
                                currentLine.add(rect)
                            } else {
                                lines.add(mutableListOf(rect))
                            }
                        }

                        for (lineRects in lines) {
                            highlightRects.add(
                                HighlightRect(
                                    left = lineRects.minOf { it.left },
                                    top = lineRects.minOf { it.top },
                                    right = lineRects.maxOf { it.right },
                                    bottom = lineRects.maxOf { it.bottom }
                                )
                            )
                        }
                    }
                }

                allMatches.add(
                    SearchMatch(
                        documentSessionId = documentSessionId,
                        pageIndex = pageIndex,
                        matchIndexInDocument = allMatches.size,
                        totalMatchesInDocument = 0,
                        startIndexOnPage = range.first,
                        endIndexOnPage = range.last + 1,
                        matchedText = matchedText,
                        snippet = snippet,
                        highlightRects = highlightRects
                    )
                )
            }
        }

        val total = allMatches.size
        return allMatches.mapIndexed { index, match ->
            match.copy(
                matchIndexInDocument = index,
                totalMatchesInDocument = total
            )
        }
    }
}
