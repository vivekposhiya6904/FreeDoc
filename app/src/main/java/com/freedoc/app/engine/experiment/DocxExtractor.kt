package com.freedoc.app.engine.experiment

import com.freedoc.app.core.model.RunStyle
import com.freedoc.app.core.model.SimpleDocument
import com.freedoc.app.core.model.SimpleParagraph
import com.freedoc.app.core.model.SimpleTextRun
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Proof-of-concept DOCX extractor for Phase 0 experiment.
 *
 * Scans a DOCX OpenXML archive for `word/document.xml`, parses paragraphs
 * and text runs, and maps them into [SimpleDocument].
 *
 * SECURITY NOTE:
 * External entity resolution and DTD loading are disabled to prevent XXE attacks.
 * Zip entries are processed purely in-memory to prevent Zip Slip vulnerabilities.
 *
 * LIMITATION DISCLAIMER:
 * This extractor focuses on basic semantic text extraction. It does NOT reproduce
 * complex Word layouts, tables, drawings, macros, or full desktop fidelity.
 */
class DocxExtractor {

    /**
     * Extracts structured text from a DOCX input stream.
     */
    fun extract(inputStream: InputStream): SimpleDocument {
        val documentXmlBytes = findEntryBytes(inputStream, "word/document.xml")
            ?: throw IllegalArgumentException("Invalid DOCX file: 'word/document.xml' not found in archive.")

        return parseDocumentXml(documentXmlBytes)
    }

    private fun findEntryBytes(inputStream: InputStream, targetEntryPath: String): ByteArray? {
        val zipStream = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipStream.nextEntry
        while (entry != null) {
            if (entry.name.equals(targetEntryPath, ignoreCase = true)) {
                return zipStream.readBytes()
            }
            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }
        return null
    }

    private fun parseDocumentXml(xmlBytes: ByteArray): SimpleDocument {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // Disallow XXE and external entity resolution
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            try {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {
                // Some parsers may not support this specific feature flag
            }
            try {
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (_: Exception) {
            }
            isXIncludeAware = false
            isExpandEntityReferences = false
        }

        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(xmlBytes))
        doc.documentElement.normalize()

        val paragraphs = mutableListOf<SimpleParagraph>()
        val pNodes = doc.getElementsByTagNameNS("*", "p")

        for (i in 0 until pNodes.length) {
            val pElement = pNodes.item(i) as? Element ?: continue
            val paragraph = parseParagraph(pElement)
            if (paragraph.runs.isNotEmpty()) {
                paragraphs.add(paragraph)
            }
        }

        return SimpleDocument(paragraphs = paragraphs)
    }

    private fun parseParagraph(pElement: Element): SimpleParagraph {
        val runs = mutableListOf<SimpleTextRun>()
        var headingLevel = 0

        val childNodes = pElement.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                val element = child as Element
                val localName = element.localName ?: element.nodeName.substringAfterLast(":")

                when (localName) {
                    "pPr" -> {
                        headingLevel = detectHeadingLevel(element)
                    }
                    "r" -> {
                        val run = parseRun(element)
                        if (run != null && run.text.isNotEmpty()) {
                            runs.add(run)
                        }
                    }
                }
            }
        }

        return SimpleParagraph(runs = runs, headingLevel = headingLevel)
    }

    private fun detectHeadingLevel(pPrElement: Element): Int {
        val pStyleList = pPrElement.getElementsByTagNameNS("*", "pStyle")
        if (pStyleList.length > 0) {
            val styleVal = (pStyleList.item(0) as? Element)?.getAttribute("w:val") ?: ""
            if (styleVal.startsWith("Heading", ignoreCase = true)) {
                return styleVal.removePrefix("Heading").toIntOrNull() ?: 1
            }
        }
        return 0
    }

    private fun parseRun(rElement: Element): SimpleTextRun? {
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var fontSizePt: Float? = null
        val textBuilder = StringBuilder()

        val childNodes = rElement.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                val element = child as Element
                val localName = element.localName ?: element.nodeName.substringAfterLast(":")

                when (localName) {
                    "rPr" -> {
                        val rPrChildren = element.childNodes
                        for (j in 0 until rPrChildren.length) {
                            val rPrChild = rPrChildren.item(j)
                            if (rPrChild.nodeType == Node.ELEMENT_NODE) {
                                val prop = rPrChild as Element
                                val propName = prop.localName ?: prop.nodeName.substringAfterLast(":")
                                when (propName) {
                                    "b" -> isBold = prop.getAttribute("w:val") != "0" && prop.getAttribute("w:val") != "false"
                                    "i" -> isItalic = prop.getAttribute("w:val") != "0" && prop.getAttribute("w:val") != "false"
                                    "u" -> isUnderline = prop.getAttribute("w:val") != "none"
                                    "sz" -> {
                                        // Size in half-points (e.g. 24 = 12pt)
                                        val halfPoints = prop.getAttribute("w:val").toFloatOrNull()
                                        if (halfPoints != null) {
                                            fontSizePt = halfPoints / 2.0f
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "t" -> {
                        textBuilder.append(element.textContent)
                    }
                    "tab" -> {
                        textBuilder.append("    ")
                    }
                    "br" -> {
                        textBuilder.append("\n")
                    }
                }
            }
        }

        val text = textBuilder.toString()
        if (text.isEmpty()) return null

        return SimpleTextRun(
            text = text,
            style = RunStyle(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                fontSizePt = fontSizePt
            )
        )
    }
}
