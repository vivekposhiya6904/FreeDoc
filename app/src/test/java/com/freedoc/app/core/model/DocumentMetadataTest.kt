package com.freedoc.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DocumentMetadataTest {

    @Test
    fun testFormatBytesFormatting() {
        assertEquals("Unknown size", DocumentMetadata.formatBytes(-1L))
        assertEquals("0 B", DocumentMetadata.formatBytes(0L))
        assertEquals("512 B", DocumentMetadata.formatBytes(512L))
        assertEquals("1023 B", DocumentMetadata.formatBytes(1023L))
        assertEquals("1.0 KB", DocumentMetadata.formatBytes(1024L))
        assertEquals("1.5 KB", DocumentMetadata.formatBytes(1536L))
        assertEquals("1.0 MB", DocumentMetadata.formatBytes(1024L * 1024L))
        assertEquals("4.2 MB", DocumentMetadata.formatBytes((4.2 * 1024 * 1024).toLong()))
        assertEquals("1.00 GB", DocumentMetadata.formatBytes(1024L * 1024L * 1024L))
    }

    @Test
    fun testDocumentMetadataProperties() {
        val uriString = "content://com.android.providers.media.documents/document/1234"
        val metadata = DocumentMetadata(
            uriString = uriString,
            displayName = "Financial_Report_2026.pdf",
            sizeBytes = 2048 * 1024,
            pageCount = 15
        )

        assertEquals(uriString, metadata.uriString)
        assertEquals("Financial_Report_2026.pdf", metadata.displayName)
        assertEquals(2048L * 1024L, metadata.sizeBytes)
        assertEquals(15, metadata.pageCount)
        assertEquals("2.0 MB", metadata.formattedSize())
    }
}
