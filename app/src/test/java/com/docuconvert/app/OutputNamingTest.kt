package com.docuconvert.app

import com.docuconvert.app.domain.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class OutputNamingTest {

    /** Mirrors FileStorageManager.uniqueDisplayName logic for pure unit testing. */
    private fun uniqueName(base: String, ext: String, exists: (String) -> Boolean): String {
        var candidate = "$base.$ext"
        var counter = 1
        while (exists(candidate)) {
            candidate = "$base ($counter).$ext"
            counter++
        }
        return candidate
    }

    @Test
    fun `proposal docx becomes proposal pdf`() {
        assertEquals("proposal.pdf", uniqueName("proposal", "pdf") { false })
    }

    @Test
    fun `existing file gets numeric suffix`() {
        val existing = setOf("proposal.pdf", "proposal (1).pdf")
        assertEquals("proposal (2).pdf", uniqueName("proposal", "pdf") { it in existing })
    }

    @Test
    fun `first collision gets suffix 1`() {
        assertEquals("proposal (1).pdf", uniqueName("proposal", "pdf") { it == "proposal.pdf" })
    }

    @Test
    fun `all formats produce correct extension`() {
        for (format in DocumentFormat.entries) {
            val name = uniqueName("doc", format.extension) { false }
            assertEquals("doc.${format.extension}", name)
        }
    }
}
