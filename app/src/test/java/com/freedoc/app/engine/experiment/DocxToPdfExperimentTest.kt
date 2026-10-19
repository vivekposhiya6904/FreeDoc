package com.freedoc.app.engine.experiment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end unit test validating the Phase 0 Document-Engine experiment:
 * 1. Read a simple synthetic DOCX file.
 * 2. Extract paragraphs and formatting information.
 * 3. Represent in the internal SimpleDocument model.
 * 4. Generate a simple PDF from extracted content.
 * 5. Verify the generated PDF contains valid structure and text.
 * 6. Execute entirely offline.
 */
class DocxToPdfExperimentTest {

    @Test
    fun testDocxExtractionAndPdfGenerationWorkflow() {
        // Step 1: Create a synthetic in-memory DOCX file
        val docxBytes = createSyntheticDocx()
        assertTrue("Synthetic DOCX archive must have valid bytes", docxBytes.isNotEmpty())

        // Step 2 & 3: Extract paragraphs and represent in SimpleDocument
        val extractor = DocxExtractor()
        val document = extractor.extract(ByteArrayInputStream(docxBytes))

        assertNotNull(document)
        assertEquals("Should extract 3 paragraphs", 3, document.paragraphs.size)

        // Verify Heading Paragraph
        val headingPara = document.paragraphs[0]
        assertEquals("Heading level should be 1", 1, headingPara.headingLevel)
        assertEquals("FreeDoc Architecture Proof of Concept", headingPara.toPlainText())

        // Verify Second Paragraph (Mixed formatting)
        val mixedPara = document.paragraphs[1]
        assertEquals("Second paragraph should have 2 runs", 2, mixedPara.runs.size)
        assertTrue("First run should be bold", mixedPara.runs[0].style.isBold)
        assertEquals("Bold Heading: ", mixedPara.runs[0].text)
        assertEquals("This is standard body text.", mixedPara.runs[1].text)

        // Verify Third Paragraph (Italic)
        val italicPara = document.paragraphs[2]
        assertTrue("Run should be italic", italicPara.runs[0].style.isItalic)
        assertEquals("Privacy-first offline document processing.", italicPara.toPlainText())

        // Step 4: Generate a PDF from extracted content
        val pdfGenerator = SimplePdfGenerator()
        val pdfOutputStream = ByteArrayOutputStream()
        pdfGenerator.generate(document, pdfOutputStream)
        val pdfBytes = pdfOutputStream.toByteArray()

        // Step 5: Verify that the generated PDF structure and text are valid
        assertTrue("PDF output should not be empty", pdfBytes.isNotEmpty())
        val pdfString = String(pdfBytes, StandardCharsets.ISO_8859_1)

        // Header check
        assertTrue("Must start with PDF header", pdfString.startsWith("%PDF-1.4"))

        // Standard objects check
        assertTrue("Must contain Catalog", pdfString.contains("/Type /Catalog"))
        assertTrue("Must contain Pages tree", pdfString.contains("/Type /Pages"))
        assertTrue("Must contain Page definition", pdfString.contains("/Type /Page"))
        assertTrue("Must contain Content stream", pdfString.contains("stream") && pdfString.contains("endstream"))
        assertTrue("Must contain Fonts", pdfString.contains("/BaseFont /Helvetica"))

        // Cross-reference table and trailer check
        assertTrue("Must contain xref table", pdfString.contains("xref"))
        assertTrue("Must contain trailer dictionary", pdfString.contains("trailer"))
        assertTrue("Must contain EOF marker", pdfString.contains("%%EOF"))

        // Text content check inside stream
        assertTrue("PDF must contain heading text", pdfString.contains("FreeDoc Architecture Proof of Concept"))
        assertTrue("PDF must contain body text", pdfString.contains("This is standard body text."))
        assertTrue("PDF must contain privacy text", pdfString.contains("Privacy-first offline document processing."))
    }

    /**
     * Helper to construct a standard OpenXML DOCX archive in memory.
     * Contains minimal [Content_Types].xml, _rels/.rels, and word/document.xml.
     */
    private fun createSyntheticDocx(): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // [Content_Types].xml
            val contentTypesXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent()
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // word/document.xml
            val documentXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                    <w:body>
                        <w:p>
                            <w:pPr>
                                <w:pStyle w:val="Heading1"/>
                            </w:pPr>
                            <w:r>
                                <w:t>FreeDoc Architecture Proof of Concept</w:t>
                            </w:r>
                        </w:p>
                        <w:p>
                            <w:r>
                                <w:rPr>
                                    <w:b/>
                                </w:rPr>
                                <w:t>Bold Heading: </w:t>
                            </w:r>
                            <w:r>
                                <w:t>This is standard body text.</w:t>
                            </w:r>
                        </w:p>
                        <w:p>
                            <w:r>
                                <w:rPr>
                                    <w:i/>
                                </w:rPr>
                                <w:t>Privacy-first offline document processing.</w:t>
                            </w:r>
                        </w:p>
                    </w:body>
                </w:document>
            """.trimIndent()
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(documentXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
        return bos.toByteArray()
    }
}
