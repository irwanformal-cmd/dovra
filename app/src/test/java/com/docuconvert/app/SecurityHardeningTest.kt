package com.docuconvert.app

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.FormatDetector
import com.docuconvert.app.domain.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Security-focused unit tests: hostile filenames, malformed inputs and
 * signature sniffing must fail safely (no crash, no path escape).
 */
class SecurityHardeningTest {

    /** Mirrors AppViewModel.uniqueOutputName sanitization. */
    private fun safeOutputName(sourceName: String, ext: String): String {
        val base = sourceName.substringBeforeLast('.', sourceName)
            .takeIf { it.isNotBlank() } ?: "converted"
        val safe = base.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().take(100)
            .ifBlank { "converted" }
        return "$safe.$ext"
    }

    @Test
    fun `path traversal filename is neutralized`() {
        val out = safeOutputName("../../etc/passwd.pdf", "txt")
        assertEquals(".._.._etc_passwd.txt", out)
    }

    @Test
    fun `absolute path filename is neutralized`() {
        val out = safeOutputName("/sdcard/evil.docx", "pdf")
        assertEquals("_sdcard_evil.pdf", out)
    }

    @Test
    fun `windows separators are neutralized`() {
        val out = safeOutputName("..\\..\\secret.txt", "pdf")
        assertEquals(".._.._secret.pdf", out)
    }

    @Test
    fun `blank base falls back to converted`() {
        // "///" sanitizes to "___" (no separator survives); truly empty falls back.
        assertEquals("___.pdf", safeOutputName("///", "pdf"))
        assertEquals("converted.pdf", safeOutputName("", "pdf"))
    }

    @Test
    fun `very long filename is truncated`() {
        val long = "a".repeat(500) + ".docx"
        val out = safeOutputName(long, "pdf")
        assertEquals(104, out.length) // 100 + ".pdf"
    }

    @Test
    fun `sanitized name never contains separator`() {
        for (evil in listOf("a/b", "a\\b", "../x", "C:\\y", "a:b", "a*b", "a?b", "a\"b", "a<b>c")) {
            val out = safeOutputName("$evil.docx", "txt")
            assert(!('/' in out || '\\' in out)) { "separator leaked in $out" }
        }
    }

    // --- FormatDetector header sniffing: malformed / hostile headers fail safe ---

    @Test
    fun `empty header sniffs to null`() {
        assertNull(FormatDetector.sniffFromHeader(ByteArray(16), 0))
    }

    @Test
    fun `short header sniffs to null`() {
        assertNull(FormatDetector.sniffFromHeader("%PD".toByteArray(), 3))
    }

    @Test
    fun `pdf magic detected`() {
        val h = "%PDF-1.7................".toByteArray()
        assertEquals(DocumentFormat.PDF, FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `rtf magic detected`() {
        val h = "{\\rtf1\\ansi...........".toByteArray()
        assertEquals(DocumentFormat.RTF, FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `xml magic detected`() {
        val h = "<?xml version=\"1.0\"..".toByteArray()
        assertEquals(DocumentFormat.XML, FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `zip header is ambiguous not misdetected`() {
        val h = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertNull(FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `ole2 header is ambiguous not misdetected`() {
        val h = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertNull(FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `random garbage sniffs to null`() {
        val h = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        assertNull(FormatDetector.sniffFromHeader(h, h.size))
    }

    @Test
    fun `null stream sniffing fails safe`() {
        val cr = org.mockito.kotlin.mock<ContentResolver>()
        val uri = org.mockito.kotlin.mock<Uri>()
        org.mockito.kotlin.whenever(cr.openInputStream(uri)).thenReturn(null)
        assertNull(FormatDetector.sniffSignature(cr, uri))
    }
}
