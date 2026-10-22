package com.freedoc.app.ui.viewer

/**
 * Result of validating user input for direct page navigation.
 */
sealed interface PageValidationResult {
    /**
     * The input is a valid 1-based page number within [1, totalPages].
     *
     * @param pageNumberOneBased 1-based human-readable page number.
     * @param targetIndexZeroBased 0-based index used for document operations.
     */
    data class Valid(
        val pageNumberOneBased: Int,
        val targetIndexZeroBased: Int
    ) : PageValidationResult

    /**
     * The input is invalid with a user-friendly error message.
     */
    data class Invalid(
        val message: String
    ) : PageValidationResult

    companion object {
        /**
         * Validates a human-entered string against [totalPages].
         */
        fun validate(input: String, totalPages: Int): PageValidationResult {
            if (input.isBlank()) {
                return Invalid("Please enter a page number.")
            }

            val trimmed = input.trim()
            val parsed = trimmed.toIntOrNull()
                ?: return Invalid("Please enter numbers only.")

            if (parsed < 1) {
                return Invalid("Page number must be 1 or greater.")
            }

            if (parsed > totalPages) {
                return Invalid("Page number cannot exceed $totalPages.")
            }

            return Valid(
                pageNumberOneBased = parsed,
                targetIndexZeroBased = parsed - 1
            )
        }
    }
}
