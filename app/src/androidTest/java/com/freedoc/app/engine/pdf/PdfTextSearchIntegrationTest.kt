package com.freedoc.app.engine.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freedoc.app.core.model.SimpleDocument
import com.freedoc.app.core.model.SimpleParagraph
import com.freedoc.app.core.model.SimpleTextRun
import com.freedoc.app.engine.experiment.SimplePdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * On-device / Emulator instrumented tests for PDF text extraction and search.
 *
 * Verifies real Android integration with PDFBox-Android:
 * - Proper initialization via [PdfTextExtractor.init] using real Android [Context].
 * - Full-document text extraction from a real PDF file.
 * - Accurate detection and matching of keywords (e.g. "meaning", "Meaning", "MEANING").
 * - Multi-page result collection and 1-based page number mapping.
 */
@RunWith(AndroidJUnit4::class)
class PdfTextSearchIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfTextExtractor.init(context)
    }

    @Test
    fun testRealPdfFileExtractionAndSearchForWordMeaning() {
        val sampleDoc = SimpleDocument(
            title = "Textbook Sample",
            paragraphs = listOf(
                SimpleParagraph(
                    runs = listOf(SimpleTextRun("Chapter 1: Understanding the fundamental meaning of embedded systems."))
                ),
                SimpleParagraph(
                    runs = listOf(SimpleTextRun("Every microcontroller has real-time meaning in deterministic execution."))
                )
            )
        )

        val tempFile = File(context.cacheDir, "test_search_textbook.pdf")
        try {
            FileOutputStream(tempFile).use { out ->
                SimplePdfGenerator().generate(sampleDoc, out)
            }
            assertTrue("Generated PDF must exist and have content", tempFile.exists() && tempFile.length() > 0)

            // 1. Extract text from the real file
            val extractionResult = PdfTextExtractor.extract(tempFile, context)
            assertTrue("PDF must contain searchable text", extractionResult.hasSearchableText)
            assertTrue("At least one page must be extracted", extractionResult.pages.isNotEmpty())

            // 2. Search for exact word "meaning"
            val matches = PdfSearchEngine.search("meaning", extractionResult.pages)
            assertEquals("Word 'meaning' must be found twice in document", 2, matches.size)
            assertEquals("Match must map to page index 0", 0, matches[0].pageIndex)
            assertEquals("1-based display page number must be 1", 1, matches[0].displayPageNumber)

            // 3. Verify case-insensitivity
            val upperMatches = PdfSearchEngine.search("MEANING", extractionResult.pages)
            assertEquals("Uppercase query must return same matches", 2, upperMatches.size)

            val mixedMatches = PdfSearchEngine.search("Meaning", extractionResult.pages)
            assertEquals("Mixed-case query must return same matches", 2, mixedMatches.size)

            // 4. Verify no-match handling
            val nonExistent = PdfSearchEngine.search("NonExistentSuperString", extractionResult.pages)
            assertTrue("Non-existent query must return empty list", nonExistent.isEmpty())

        } finally {
            tempFile.delete()
        }
    }
}
