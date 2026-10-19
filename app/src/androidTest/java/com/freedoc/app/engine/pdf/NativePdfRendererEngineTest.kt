package com.freedoc.app.engine.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freedoc.app.core.model.SimpleDocument
import com.freedoc.app.core.model.SimpleParagraph
import com.freedoc.app.core.model.SimpleTextRun
import com.freedoc.app.engine.experiment.SimplePdfGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * On-device / Emulator instrumented tests for [NativePdfRendererEngine].
 *
 * Verifies real Android framework integration with [android.graphics.pdf.PdfRenderer]:
 * - Actual rasterization of PDF pages into ARGB_8888 Bitmaps.
 * - Accurate page-count and metadata resolution.
 * - Graceful failure handling on empty (0-byte) and malformed files.
 * - Resource cleanup and access prevention after document closure.
 */
@RunWith(AndroidJUnit4::class)
class NativePdfRendererEngineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testOpenAndRenderValidPdfDocument() = runBlocking {
        // 1. Generate an actual valid PDF-1.4 file using SimplePdfGenerator
        val sampleDoc = SimpleDocument(
            title = "Test PDF",
            paragraphs = listOf(
                SimpleParagraph(
                    runs = listOf(SimpleTextRun("FreeDoc Native PdfRenderer Verification")),
                    headingLevel = 1
                ),
                SimpleParagraph(
                    runs = listOf(SimpleTextRun("Verifying real page rasterization on Android."))
                )
            )
        )

        val tempPdfFile = File(context.cacheDir, "test_valid_doc.pdf")
        FileOutputStream(tempPdfFile).use { out ->
            SimplePdfGenerator().generate(sampleDoc, out)
        }
        assertTrue("Test PDF file must exist and have content", tempPdfFile.exists() && tempPdfFile.length() > 0)

        // 2. Open via NativePdfRendererEngine
        val openResult = NativePdfRendererEngine.open(context, Uri.fromFile(tempPdfFile))
        assertTrue("Opening valid PDF must succeed", openResult.isSuccess)

        val doc = openResult.getOrThrow()
        try {
            assertEquals("Page count must be 1", 1, doc.pageCount)
            assertEquals("test_valid_doc.pdf", doc.metadata.displayName)
            assertTrue("File size must be greater than 0", doc.metadata.sizeBytes > 0)

            // 3. Render page 0
            val bitmap = doc.renderPage(0, targetWidth = 1080)
            assertNotNull("Rendered bitmap must not be null", bitmap)
            assertTrue("Bitmap width must be positive", bitmap.width > 0)
            assertTrue("Bitmap height must be positive", bitmap.height > 0)
            assertEquals("Bitmap config must be ARGB_8888", Bitmap.Config.ARGB_8888, bitmap.config)

            // 4. Test out-of-bounds page index
            try {
                doc.renderPage(99)
                fail("Requesting out-of-bounds page index must throw IndexOutOfBoundsException")
            } catch (e: IndexOutOfBoundsException) {
                // Expected
            }
        } finally {
            // 5. Verify resource cleanup
            doc.close()
            tempPdfFile.delete()
        }

        // 6. Verify operations on closed document are rejected
        try {
            doc.renderPage(0)
            fail("Calling renderPage on a closed document must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun testOpenEmptyFileFailsWithEmptyFileError() = runBlocking {
        val emptyFile = File(context.cacheDir, "empty_test_file.pdf")
        emptyFile.createNewFile()
        assertEquals(0L, emptyFile.length())

        try {
            val result = NativePdfRendererEngine.open(context, Uri.fromFile(emptyFile))
            assertTrue("Opening empty file must fail", result.isFailure)

            val exception = result.exceptionOrNull()
            assertTrue("Exception must be PdfException", exception is PdfException)
            assertEquals(
                "Error type must be EMPTY_FILE",
                PdfErrorType.EMPTY_FILE,
                (exception as PdfException).errorType
            )
        } finally {
            emptyFile.delete()
        }
    }

    @Test
    fun testOpenMalformedFileFailsWithInvalidFormatError() = runBlocking {
        val malformedFile = File(context.cacheDir, "corrupted.pdf")
        malformedFile.writeText("This is plain text and definitely not a PDF document format.")

        try {
            val result = NativePdfRendererEngine.open(context, Uri.fromFile(malformedFile))
            assertTrue("Opening corrupted file must fail", result.isFailure)

            val exception = result.exceptionOrNull()
            assertTrue("Exception must be PdfException", exception is PdfException)
            assertEquals(
                "Error type must be INVALID_FORMAT",
                PdfErrorType.INVALID_FORMAT,
                (exception as PdfException).errorType
            )
        } finally {
            malformedFile.delete()
        }
    }
}
