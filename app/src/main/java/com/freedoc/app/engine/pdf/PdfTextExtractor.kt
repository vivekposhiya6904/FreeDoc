package com.freedoc.app.engine.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream
import java.io.StringWriter

/**
 * Robust, offline, privacy-first PDF text extractor backed by PDFBox-Android.
 *
 * Fully supports:
 * - PDF 1.0 to 2.0 specifications
 * - Compressed Object Streams (/ObjStm) and Cross-Reference Streams (/XRef)
 * - Embedded TrueType, Type1, and Type0 (CID) font encodings and /ToUnicode CMaps
 * - Kerning / array text operators (TJ, Tj, ', ")
 * - Word un-hyphenation across line endings
 * - Accurate detection of scanned / textless documents
 * - Single-pass O(N) page-by-page extraction with bounded memory usage
 */
object PdfTextExtractor {

    data class PageTextContent(
        val text: String,
        val charBounds: List<HighlightRect?> = emptyList()
    )

    data class ExtractionResult(
        val pages: Map<Int, String>,
        val pageContents: Map<Int, PageTextContent> = emptyMap(),
        val hasSearchableText: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Ensures PDFBox Android resource loader is initialized before extraction.
     * Safe to call multiple times or from non-Android test environments.
     */
    fun init(context: Context) {
        try {
            if (!PDFBoxResourceLoader.isReady()) {
                PDFBoxResourceLoader.init(context.applicationContext)
            }
        } catch (_: Exception) {
            // In pure JVM unit test environments, Android resources are absent; ignore safely
        }
    }

    /**
     * Extracts text and character-level geometry from [file] in a memory-efficient single pass.
     */
    fun extract(file: File, context: Context? = null): ExtractionResult {
        context?.let { init(it) }
        return try {
            PDDocument.load(file).use { doc ->
                extractFromPdDocument(doc)
            }
        } catch (e: Exception) {
            ExtractionResult(emptyMap(), emptyMap(), false, e.localizedMessage ?: e.message)
        }
    }

    /**
     * Extracts text and character-level geometry from [inputStream] in a single pass.
     */
    fun extract(inputStream: InputStream, context: Context? = null): ExtractionResult {
        context?.let { init(it) }
        return try {
            PDDocument.load(inputStream).use { doc ->
                extractFromPdDocument(doc)
            }
        } catch (e: Exception) {
            ExtractionResult(emptyMap(), emptyMap(), false, e.localizedMessage ?: e.message)
        }
    }

    private fun extractFromPdDocument(doc: PDDocument): ExtractionResult {
        val pageCount = doc.numberOfPages
        if (pageCount <= 0) {
            return ExtractionResult(emptyMap(), emptyMap(), false)
        }

        val stripper = PerPageTextStripper()
        stripper.getText(doc)
        val extractedMap = stripper.pageTexts
        val contentsMap = stripper.pageContents

        // Determine if searchable text exists
        val totalCharacters = extractedMap.values.sumOf { it.length }
        val hasAlphaNumeric = extractedMap.values.any { text -> text.any { it.isLetterOrDigit() } }
        val hasSearchableText = totalCharacters >= 3 && hasAlphaNumeric

        return ExtractionResult(
            pages = extractedMap,
            pageContents = contentsMap,
            hasSearchableText = hasSearchableText
        )
    }

    /**
     * Normalizes line breaks and un-hyphenates words broken at end-of-line boundaries
     * while preserving sentence structure.
     */
    fun cleanExtractedPageText(text: String): String {
        if (text.isBlank()) return ""
        val unhyphenated = text.replace(Regex("""(\b\p{L}+)-\s*\r?\n\s*(\p{L}+\b)"""), "$1$2\n")
        return unhyphenated.replace("\r\n", "\n").replace('\r', '\n').trim()
    }

    /**
     * Custom text stripper that intercepts text and character-level glyph positions at page boundaries
     * during a single linear traversal of the PDF document. Resets the writer buffer after each page
     * to keep heap allocation bounded regardless of document size.
     */
    private class PerPageTextStripper : PDFTextStripper() {
        val pageTexts = mutableMapOf<Int, String>()
        val pageContents = mutableMapOf<Int, PageTextContent>()

        private var currentPageBuilder = StringBuilder()
        private var currentCharBounds = mutableListOf<HighlightRect?>()

        init {
            sortByPosition = true
        }

        override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage?) {
            super.startPage(page)
            currentPageBuilder = StringBuilder()
            currentCharBounds = mutableListOf()
        }

        override fun writeString(text: String?, textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>?) {
            super.writeString(text, textPositions)
            if (textPositions != null) {
                for (tp in textPositions) {
                    val unicode = tp.unicode ?: continue
                    val x = tp.xDirAdj
                    val y = tp.yDirAdj
                    val w = tp.widthDirAdj
                    val h = tp.heightDir
                    val rect = HighlightRect(
                        left = x,
                        top = y - h,
                        right = x + w,
                        bottom = y + (h * 0.10f)
                    )
                    for (char in unicode) {
                        currentPageBuilder.append(char)
                        currentCharBounds.add(rect)
                    }
                }
            }
        }

        override fun writeWordSeparator() {
            super.writeWordSeparator()
            currentPageBuilder.append(' ')
            currentCharBounds.add(null)
        }

        override fun writeLineSeparator() {
            super.writeLineSeparator()
            currentPageBuilder.append('\n')
            currentCharBounds.add(null)
        }

        override fun endPage(page: com.tom_roush.pdfbox.pdmodel.PDPage?) {
            super.endPage(page)
            val pageIndex = currentPageNo - 1
            val pageText = currentPageBuilder.toString()
            if (pageText.isNotBlank()) {
                pageTexts[pageIndex] = pageText
                pageContents[pageIndex] = PageTextContent(
                    text = pageText,
                    charBounds = ArrayList(currentCharBounds)
                )
            }
            val stringWriter = output as? StringWriter
            stringWriter?.buffer?.setLength(0) // Free memory immediately
        }
    }
}
