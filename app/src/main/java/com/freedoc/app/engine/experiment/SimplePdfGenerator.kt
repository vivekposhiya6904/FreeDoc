package com.freedoc.app.engine.experiment

import com.freedoc.app.core.model.SimpleDocument
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Proof-of-concept minimalist PDF generator for Phase 0 experiment.
 *
 * Emits a valid, compliant PDF-1.4 document containing extracted text.
 * Uses standard standard 14 Type-1 fonts (Helvetica, Helvetica-Bold)
 * without requiring any native or heavy third-party dependencies.
 *
 * LIMITATION DISCLAIMER:
 * This generator produces sequential text lines on a standard A4 page.
 * It is a proof-of-concept pipeline component for verifying end-to-end
 * text extraction and serialization, NOT a high-fidelity page layout engine.
 */
class SimplePdfGenerator {

    /**
     * Renders the given [document] into a valid PDF stream.
     */
    fun generate(document: SimpleDocument, outputStream: OutputStream) {
        val streamContent = buildContentStream(document)
        val streamBytes = streamContent.toByteArray(StandardCharsets.ISO_8859_1)

        val buffer = ByteArrayOutputStream()
        val offsets = mutableListOf<Long>()

        // Helper to write ASCII strings and track byte offsets
        fun writeAscii(str: String) {
            buffer.write(str.toByteArray(StandardCharsets.ISO_8859_1))
        }

        fun writeObject(objNum: Int, body: (ByteArrayOutputStream) -> Unit) {
            offsets.add(buffer.size().toLong())
            writeAscii("$objNum 0 obj\n")
            body(buffer)
            writeAscii("\nendobj\n")
        }

        // 1. PDF Header
        writeAscii("%PDF-1.4\n")
        // Binary marker to prevent corruption in ASCII-mode transfers
        buffer.write(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        // 2. Objects
        // Object 1: Catalog
        writeObject(1) { out ->
            out.write("<< /Type /Catalog /Pages 2 0 R >>".toByteArray(StandardCharsets.ISO_8859_1))
        }

        // Object 2: Pages
        writeObject(2) { out ->
            out.write("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".toByteArray(StandardCharsets.ISO_8859_1))
        }

        // Object 3: Page (A4: 595.28 x 841.89 points)
        writeObject(3) { out ->
            val pageDict = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                    "/Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>"
            out.write(pageDict.toByteArray(StandardCharsets.ISO_8859_1))
        }

        // Object 4: Content Stream
        writeObject(4) { out ->
            val dict = "<< /Length ${streamBytes.size} >>\nstream\n"
            out.write(dict.toByteArray(StandardCharsets.ISO_8859_1))
            out.write(streamBytes)
            out.write("\nendstream".toByteArray(StandardCharsets.ISO_8859_1))
        }

        // Object 5: Regular Font (Helvetica)
        writeObject(5) { out ->
            out.write("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".toByteArray(StandardCharsets.ISO_8859_1))
        }

        // Object 6: Bold Font (Helvetica-Bold)
        writeObject(6) { out ->
            out.write("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".toByteArray(StandardCharsets.ISO_8859_1))
        }

        // 3. Cross-Reference Table
        val startXRef = buffer.size().toLong()
        writeAscii("xref\n")
        writeAscii("0 7\n")
        writeAscii("0000000000 65535 f \n")
        for (offset in offsets) {
            writeAscii(String.format("%010d 00000 n \n", offset))
        }

        // 4. Trailer
        writeAscii("trailer\n")
        writeAscii("<< /Size 7 /Root 1 0 R >>\n")
        writeAscii("startxref\n")
        writeAscii("$startXRef\n")
        writeAscii("%%EOF\n")

        buffer.writeTo(outputStream)
    }

    private fun buildContentStream(document: SimpleDocument): String {
        val sb = StringBuilder()
        sb.append("BT\n")

        // Initial cursor at top-left with 50pt margins (A4 height is 842)
        sb.append("50 780 Td\n")
        sb.append("18 TL\n") // Leading (line spacing) 18pt

        var currentFont = ""

        for (paragraph in document.paragraphs) {
            val isHeading = paragraph.headingLevel > 0
            val fontSize = if (isHeading) 16 else 12

            for (run in paragraph.runs) {
                val fontName = if (run.style.isBold || isHeading) "/F2" else "/F1"
                if (fontName != currentFont) {
                    sb.append("$fontName $fontSize Tf\n")
                    currentFont = fontName
                }

                val sanitized = escapePdfString(run.text)
                sb.append("($sanitized) Tj\n")
            }
            // Move to next line for paragraph break
            sb.append("T*\n")
        }

        sb.append("ET\n")
        return sb.toString()
    }

    private fun escapePdfString(input: String): String {
        return buildString {
            for (char in input) {
                when (char) {
                    '\\' -> append("\\\\")
                    '(' -> append("\\(")
                    ')' -> append("\\)")
                    '\r' -> {}
                    '\n' -> append(" ")
                    else -> {
                        // Accept standard printable ASCII and WinAnsi characters
                        if (char.code in 32..126) {
                            append(char)
                        } else if (char.code in 128..255) {
                            // Octal escape for WinAnsi extended range
                            append(String.format("\\%03o", char.code))
                        } else {
                            // Fallback for unsupported complex Unicode in basic Type-1
                            append("?")
                        }
                    }
                }
            }
        }
    }
}
