package com.freedoc.app.core.model

import android.net.Uri
import java.util.Locale

/**
 * Encapsulates metadata for an open document.
 *
 * @param uriString The SAF or content URI string identifying the document.
 * @param displayName The human-readable file name (e.g., "report.pdf").
 * @param sizeBytes The document size in bytes, or -1 if unknown.
 * @param pageCount The total page count of the document.
 */
data class DocumentMetadata(
    val uriString: String = "",
    val displayName: String = "Document.pdf",
    val sizeBytes: Long = -1L,
    val pageCount: Int = 0
) {
    constructor(
        uri: Uri,
        displayName: String = "Document.pdf",
        sizeBytes: Long = -1L,
        pageCount: Int = 0
    ) : this(
        uriString = uri.toString(),
        displayName = displayName,
        sizeBytes = sizeBytes,
        pageCount = pageCount
    )

    val uri: Uri
        get() = Uri.parse(uriString)

    /**
     * Formats [sizeBytes] into a human-readable string (e.g., "1.4 MB", "350 KB").
     */
    fun formattedSize(): String = formatBytes(sizeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "Unknown size"
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }
    }
}
