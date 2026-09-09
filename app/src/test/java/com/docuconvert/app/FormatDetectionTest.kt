package com.docuconvert.app

import com.docuconvert.app.conversion.FormatDetector
import com.docuconvert.app.domain.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDetectionTest {

    @Test
    fun `pdf magic bytes detected`() {
        val header = "%PDF-1.7\n%...".toByteArray(Charsets.US_ASCII)
        assertEquals(DocumentFormat.PDF, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `rtf magic bytes detected`() {
        val header = "{\\rtf1\\ansi".toByteArray(Charsets.US_ASCII)
        assertEquals(DocumentFormat.RTF, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `xml magic bytes detected`() {
        val header = "<?xml version".toByteArray(Charsets.US_ASCII)
        assertEquals(DocumentFormat.XML, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `zip header is ambiguous returns null`() {
        val header = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        assertEquals(null, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `ole2 header is ambiguous returns null`() {
        val header = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
        assertEquals(null, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `plain text has no signature`() {
        val header = "Hello, world!\n".toByteArray(Charsets.US_ASCII)
        assertEquals(null, FormatDetector.sniffFromHeader(header, header.size))
    }

    @Test
    fun `extension lookup is case insensitive`() {
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromExtension("DOCX"))
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromExtension(".docx"))
        assertEquals(DocumentFormat.PDF, DocumentFormat.fromExtension("pdf"))
    }

    @Test
    fun `unknown extension returns null`() {
        assertEquals(null, DocumentFormat.fromExtension("xyz"))
        assertEquals(null, DocumentFormat.fromExtension(""))
    }

    @Test
    fun `mime lookup resolves common types`() {
        assertEquals(DocumentFormat.PDF, DocumentFormat.fromMimeType("application/pdf"))
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(null, DocumentFormat.fromMimeType("video/mp4"))
        assertEquals(null, DocumentFormat.fromMimeType(null))
    }
}
