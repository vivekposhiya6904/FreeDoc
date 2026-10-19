package com.freedoc.app.engine.pdf

/**
 * Categorized error types for PDF opening and page rendering failures.
 */
enum class PdfErrorType {
    EMPTY_FILE,
    INVALID_FORMAT,
    PASSWORD_PROTECTED,
    PERMISSION_DENIED,
    NOT_SEEKABLE,
    OUT_OF_MEMORY,
    IO_ERROR,
    UNKNOWN
}

/**
 * Structured error details for PDF operations.
 */
data class PdfErrorInfo(
    val errorType: PdfErrorType,
    val title: String,
    val message: String,
    val cause: Throwable? = null
)
