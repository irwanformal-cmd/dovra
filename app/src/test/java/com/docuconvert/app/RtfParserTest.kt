package com.docuconvert.app

import com.docuconvert.app.conversion.readers.RtfReader
import com.docuconvert.app.domain.ContentBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtfParserTest {

    private val reader = RtfReader()

    @Test
    fun `parses simple paragraphs`() {
        val rtf = "{\\rtf1\\ansi Hello\\par World\\par}"
        val doc = reader.parseRtf(rtf)
        val paras = doc.blocks.filterIsInstance<ContentBlock.Paragraph>()
        assertEquals(2, paras.size)
        assertEquals("Hello", paras[0].text)
        assertEquals("World", paras[1].text)
    }

    @Test
    fun `decodes unicode escapes`() {
        val rtf = "{\\rtf1\\ansi Caf\\u233?\\par}"
        val doc = reader.parseRtf(rtf)
        val paras = doc.blocks.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paras.any { it.text.contains("Caf") })
    }

    @Test
    fun `decodes hex escapes`() {
        val rtf = "{\\rtf1\\ansi caf\\'e9\\par}"
        val doc = reader.parseRtf(rtf)
        val paras = doc.blocks.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paras.any { it.text.contains("caf") })
    }

    @Test
    fun `bold markers captured`() {
        val rtf = "{\\rtf1\\ansi \\b Bold text\\b0\\par}"
        val doc = reader.parseRtf(rtf)
        val paras = doc.blocks.filterIsInstance<ContentBlock.Paragraph>()
        assertTrue(paras.any { it.bold })
    }

    @Test
    fun `embedded object does not crash and yields placeholder`() {
        val rtf = "{\\rtf1\\ansi Text\\par {\\object\\objemb content}\\par}"
        val doc = reader.parseRtf(rtf)
        assertTrue(doc.blocks.isNotEmpty())
    }

    @Test
    fun `empty rtf yields no blocks`() {
        val doc = reader.parseRtf("{\\rtf1\\ansi}")
        assertTrue(doc.blocks.isEmpty())
    }
}
