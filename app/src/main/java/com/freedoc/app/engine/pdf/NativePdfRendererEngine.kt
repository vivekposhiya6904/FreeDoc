package com.freedoc.app.engine.pdf

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.freedoc.app.core.model.DocumentMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Native implementation of [PdfDocument] backed by Android's [PdfRenderer].
 *
 * Designed to enforce strict resource management:
 * - Thread-safe access via internal synchronization lock.
 * - Single-page rendering off the main thread.
 * - Immediate closure of [PdfRenderer.Page] instances in finally blocks.
 * - Non-destructive file handling (source files are never altered).
 * - Automatic cleanup of [ParcelFileDescriptor] and temporary cache files.
 */
class NativePdfDocument internal constructor(
    override val uri: Uri,
    override val metadata: DocumentMetadata,
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    internal var tempFile: File? = null
) : PdfDocument {

    override val pageCount: Int
        get() = renderer.pageCount

    private val renderLock = Any()
    @Volatile
    private var isClosed = false
    private val dimensionCache = mutableMapOf<Int, PageDimensions>()

    override fun getPageDimensions(pageIndex: Int): PageDimensions {
        synchronized(renderLock) {
            check(!isClosed) { "Document is already closed." }
            if (pageIndex !in 0 until pageCount) {
                throw IndexOutOfBoundsException("Page index $pageIndex is out of range [0, $pageCount).")
            }
            return dimensionCache.getOrPut(pageIndex) {
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIndex)
                    PageDimensions(page.width, page.height)
                } finally {
                    page?.close()
                }
            }
        }
    }

    override suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(renderLock) {
            check(!isClosed) { "Document is already closed." }
            if (pageIndex !in 0 until pageCount) {
                throw IndexOutOfBoundsException("Page index $pageIndex is out of range [0, $pageCount).")
            }

            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val pageWidth = page.width
                val pageHeight = page.height

                if (pageWidth <= 0 || pageHeight <= 0) {
                    throw IllegalStateException("Invalid PDF page dimensions: ${pageWidth}x${pageHeight}")
                }

                // Compute scaling factor targeting standard screen resolution without excessive memory consumption
                val scale = (targetWidth.toFloat() / pageWidth.toFloat()).coerceIn(1.0f, 3.0f)
                val bitmapWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
                val bitmapHeight = (pageHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                // Erase to white as PDF pages default to transparent backgrounds
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                // Guaranteed cleanup of the open page to allow subsequent page opens
                page?.close()
            }
        }
    }

    override fun close() {
        synchronized(renderLock) {
            if (isClosed) return
            isClosed = true

            try {
                renderer.close()
            } catch (_: Exception) {
            }

            try {
                pfd.close()
            } catch (_: Exception) {
            }

            try {
                tempFile?.delete()
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * Factory for creating [NativePdfDocument] instances from Android URIs.
 */
object NativePdfRendererEngine {

    /**
     * Opens a PDF document identified by [uri] using Android's native [PdfRenderer].
     *
     * @param context Application context used to access ContentResolver.
     * @param uri Scoped content or file URI from Storage Access Framework.
     * @return [Result] wrapping [PdfDocument] on success, or a categorized exception on failure.
     */
    suspend fun open(context: Context, uri: Uri): Result<PdfDocument> = withContext(Dispatchers.IO) {
        val metadata = extractMetadata(context.contentResolver, uri)

        if (metadata.sizeBytes == 0L) {
            return@withContext Result.failure(
                PdfException(
                    PdfErrorType.EMPTY_FILE,
                    "The selected document contains 0 bytes."
                )
            )
        }

        var pfd: ParcelFileDescriptor? = null
        var tempFile: File? = null

        try {
            // Attempt to open the file descriptor directly
            pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (se: SecurityException) {
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.PERMISSION_DENIED,
                        "Storage permission denied or URI permission expired.",
                        se
                    )
                )
            } catch (fnf: FileNotFoundException) {
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.IO_ERROR,
                        "Document file could not be found at the specified URI.",
                        fnf
                    )
                )
            }

            if (pfd == null) {
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.IO_ERROR,
                        "Unable to obtain file descriptor for document."
                    )
                )
            }

            // Verify file descriptor is seekable; if not, stage in cache
            val renderer = try {
                PdfRenderer(pfd)
            } catch (iae: IllegalArgumentException) {
                // Descriptor is not seekable (e.g. pipe or remote content provider stream)
                // Safely copy to private application cache file
                val cached = copyToCache(context, uri)
                tempFile = cached
                pfd.close()
                pfd = ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd)
            } catch (se: SecurityException) {
                pfd.close()
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.PASSWORD_PROTECTED,
                        "This document is password protected. Password decryption is not supported in this version.",
                        se
                    )
                )
            } catch (ioe: IOException) {
                pfd.close()
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.INVALID_FORMAT,
                        "The selected file is not a valid PDF or is corrupted.",
                        ioe
                    )
                )
            }

            if (renderer.pageCount <= 0) {
                renderer.close()
                pfd.close()
                tempFile?.delete()
                return@withContext Result.failure(
                    PdfException(
                        PdfErrorType.EMPTY_FILE,
                        "Document contains no readable pages."
                    )
                )
            }

            val finalMetadata = metadata.copy(pageCount = renderer.pageCount)
            Result.success(NativePdfDocument(uri, finalMetadata, pfd, renderer, tempFile))

        } catch (oom: OutOfMemoryError) {
            pfd?.close()
            tempFile?.delete()
            Result.failure(
                PdfException(
                    PdfErrorType.OUT_OF_MEMORY,
                    "Insufficient memory to process document.",
                    oom
                )
            )
        } catch (e: Exception) {
            pfd?.close()
            tempFile?.delete()
            Result.failure(
                PdfException(
                    PdfErrorType.UNKNOWN,
                    "Failed to open document: ${e.localizedMessage ?: e.javaClass.simpleName}",
                    e
                )
            )
        }
    }

    private fun extractMetadata(contentResolver: ContentResolver, uri: Uri): DocumentMetadata {
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
        var sizeBytes = -1L

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameCol != -1) {
                            val name = cursor.getString(nameCol)
                            if (!name.isNullOrBlank()) {
                                displayName = name
                            }
                        }

                        val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeCol != -1 && !cursor.isNull(sizeCol)) {
                            sizeBytes = cursor.getLong(sizeCol)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back gracefully to defaults
            }
        }

        return DocumentMetadata(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes
        )
    }

    private fun copyToCache(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("freedoc_saf_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open input stream for URI: $uri")
        return tempFile
    }
}

/**
 * Exception class carrying [PdfErrorType] for categorized failure handling.
 */
class PdfException(
    val errorType: PdfErrorType,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
