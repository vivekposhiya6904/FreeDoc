package com.freedoc.app.engine.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfErrorTypeTest {

    @Test
    fun testPdfExceptionCarriesErrorTypeAndMessage() {
        val cause = java.io.IOException("Disk read error")
        val exception = PdfException(
            errorType = PdfErrorType.IO_ERROR,
            message = "Failed to read file from storage",
            cause = cause
        )

        assertEquals(PdfErrorType.IO_ERROR, exception.errorType)
        assertEquals("Failed to read file from storage", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testPdfErrorInfoDataStructure() {
        val errorInfo = PdfErrorInfo(
            errorType = PdfErrorType.EMPTY_FILE,
            title = "Empty Document",
            message = "The selected file is empty."
        )

        assertEquals(PdfErrorType.EMPTY_FILE, errorInfo.errorType)
        assertEquals("Empty Document", errorInfo.title)
        assertEquals("The selected file is empty.", errorInfo.message)
    }

    @Test
    fun testAllErrorTypesAreDistinct() {
        val types = PdfErrorType.entries
        assertEquals("PdfErrorType enum should define 8 categorized failure modes", 8, types.size)
        assertNotNull(PdfErrorType.valueOf("EMPTY_FILE"))
        assertNotNull(PdfErrorType.valueOf("INVALID_FORMAT"))
        assertNotNull(PdfErrorType.valueOf("PASSWORD_PROTECTED"))
        assertNotNull(PdfErrorType.valueOf("PERMISSION_DENIED"))
        assertNotNull(PdfErrorType.valueOf("NOT_SEEKABLE"))
        assertNotNull(PdfErrorType.valueOf("OUT_OF_MEMORY"))
        assertNotNull(PdfErrorType.valueOf("IO_ERROR"))
        assertNotNull(PdfErrorType.valueOf("UNKNOWN"))
    }
}
