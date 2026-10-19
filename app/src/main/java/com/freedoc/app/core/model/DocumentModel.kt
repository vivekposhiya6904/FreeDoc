package com.freedoc.app.core.model

/**
 * Baseline internal representation for document text and structure.
 *
 * NOTE: This is an initial conceptual model for Phase 0/1 experiments.
 * It is deliberately kept minimal and will evolve into the full internal
 * document model as specified in docs/ARCHITECTURE.md.
 */
data class SimpleDocument(
    val title: String = "",
    val paragraphs: List<SimpleParagraph> = emptyList()
) {
    /**
     * Returns the full plain text content of all paragraphs separated by newlines.
     */
    fun toPlainText(): String = paragraphs.joinToString("\n") { it.toPlainText() }
}

data class SimpleParagraph(
    val runs: List<SimpleTextRun> = emptyList(),
    val headingLevel: Int = 0 // 0 = Body, 1 = Heading 1, 2 = Heading 2, etc.
) {
    fun toPlainText(): String = runs.joinToString("") { it.text }
}

data class SimpleTextRun(
    val text: String,
    val style: RunStyle = RunStyle()
)

data class RunStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSizePt: Float? = null
)
