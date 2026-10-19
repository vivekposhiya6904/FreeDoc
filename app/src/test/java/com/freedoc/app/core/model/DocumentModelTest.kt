package com.freedoc.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentModelTest {

    @Test
    fun testToPlainTextCombinesRunsAndParagraphs() {
        val paragraph1 = SimpleParagraph(
            runs = listOf(
                SimpleTextRun("Hello, ", RunStyle(isBold = true)),
                SimpleTextRun("World!", RunStyle(isItalic = true))
            ),
            headingLevel = 1
        )
        val paragraph2 = SimpleParagraph(
            runs = listOf(
                SimpleTextRun("FreeDoc offline document test.")
            ),
            headingLevel = 0
        )

        val document = SimpleDocument(
            title = "Test Document",
            paragraphs = listOf(paragraph1, paragraph2)
        )

        assertEquals("Hello, World!", paragraph1.toPlainText())
        assertEquals("FreeDoc offline document test.", paragraph2.toPlainText())

        val fullText = document.toPlainText()
        val expected = "Hello, World!\nFreeDoc offline document test."
        assertEquals(expected, fullText)
    }

    @Test
    fun testEmptyDocumentReturnsEmptyString() {
        val document = SimpleDocument()
        assertTrue(document.toPlainText().isEmpty())
    }
}
