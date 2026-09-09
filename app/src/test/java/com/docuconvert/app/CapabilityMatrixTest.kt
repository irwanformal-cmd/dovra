package com.docuconvert.app

import com.docuconvert.app.domain.CapabilityMatrix
import com.docuconvert.app.domain.ConversionSupport
import com.docuconvert.app.domain.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityMatrixTest {

    @Test
    fun `docx declares priority conversions`() {
        val targets = CapabilityMatrix.supportedTargets(DocumentFormat.DOCX).map { it.target }
        assertTrue(targets.contains(DocumentFormat.PDF))
        assertTrue(targets.contains(DocumentFormat.TXT))
        assertTrue(targets.contains(DocumentFormat.HTML))
        assertTrue(targets.contains(DocumentFormat.RTF))
        assertTrue(targets.contains(DocumentFormat.ODT))
    }

    @Test
    fun `docx to pdf is simplified not full`() {
        assertEquals(
            ConversionSupport.SIMPLIFIED,
            CapabilityMatrix.supportLevel(DocumentFormat.DOCX, DocumentFormat.PDF)
        )
    }

    @Test
    fun `xlsx to csv is full fidelity`() {
        assertEquals(
            ConversionSupport.FULL,
            CapabilityMatrix.supportLevel(DocumentFormat.XLSX, DocumentFormat.CSV)
        )
    }

    @Test
    fun `unsupported pair reports not supported`() {
        assertFalse(CapabilityMatrix.isSupported(DocumentFormat.TXT, DocumentFormat.XLSX))
        assertEquals(
            ConversionSupport.NOT_SUPPORTED,
            CapabilityMatrix.supportLevel(DocumentFormat.TXT, DocumentFormat.XLSX)
        )
    }

    @Test
    fun `djvu is not a registered format`() {
        assertEquals(null, DocumentFormat.fromExtension("djvu"))
    }

    @Test
    fun `every declared pair has distinct source and target`() {
        for (source in DocumentFormat.entries) {
            for (cap in CapabilityMatrix.supportedTargets(source)) {
                assertTrue("${cap.source} -> ${cap.target}", cap.source != cap.target)
            }
        }
    }

    @Test
    fun `pdf to txt and pdf to html declared`() {
        assertTrue(CapabilityMatrix.isSupported(DocumentFormat.PDF, DocumentFormat.TXT))
        assertTrue(CapabilityMatrix.isSupported(DocumentFormat.PDF, DocumentFormat.HTML))
    }

    @Test
    fun `all formats are openable`() {
        assertEquals(
            DocumentFormat.entries.size,
            CapabilityMatrix.openableFormats().size
        )
    }
}
