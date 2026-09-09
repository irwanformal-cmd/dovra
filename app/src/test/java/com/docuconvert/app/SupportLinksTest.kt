package com.docuconvert.app

import com.docuconvert.app.presentation.SupportLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the fixed external support URLs: exact, HTTPS-only, never dynamic. */
class SupportLinksTest {

    @Test
    fun `buy me a coffee url is exactly correct`() {
        assertEquals("https://buymeacoffee.com/riaksupport", SupportLinks.BUY_ME_A_COFFEE)
    }

    @Test
    fun `saweria url is exactly correct`() {
        assertEquals("https://saweria.co/riaksupport", SupportLinks.SAWERIA)
    }

    @Test
    fun `both urls are https`() {
        assertTrue(SupportLinks.BUY_ME_A_COFFEE.startsWith("https://"))
        assertTrue(SupportLinks.SAWERIA.startsWith("https://"))
    }

    @Test
    fun `urls contain no whitespace or user input`() {
        for (url in listOf(SupportLinks.BUY_ME_A_COFFEE, SupportLinks.SAWERIA)) {
            assertTrue(url.none { it.isWhitespace() })
        }
    }
}
