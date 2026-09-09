package com.docuconvert.app

import com.docuconvert.app.conversion.ConversionOrchestrator
import com.docuconvert.app.domain.CapabilityMatrix
import com.docuconvert.app.domain.DocumentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionRoutingTest {

    private val orchestrator = ConversionOrchestrator(initPdfLoader = {})

    @Test
    fun `every capability pair has an engine`() {
        val missing = mutableListOf<String>()
        for (source in DocumentFormat.entries) {
            for (cap in CapabilityMatrix.supportedTargets(source)) {
                if (orchestrator.getEngine(source, cap.target) == null) {
                    missing.add("$source -> ${cap.target}")
                }
            }
        }
        assertEquals("Missing engines: $missing", 0, missing.size)
    }

    @Test
    fun `every engine pair is declared in capability matrix`() {
        for (engine in orchestrator.allEngines) {
            assertNotNull(
                "Engine ${engine.sourceFormat} -> ${engine.targetFormat} not in matrix",
                CapabilityMatrix.supportedTargets(engine.sourceFormat)
                    .firstOrNull { it.target == engine.targetFormat }
            )
        }
    }

    @Test
    fun `unsupported pair has no engine`() {
        assertNull(orchestrator.getEngine(DocumentFormat.TXT, DocumentFormat.XLSX))
        assertNull(orchestrator.getEngine(DocumentFormat.PDF, DocumentFormat.DOCX))
    }

    @Test
    fun `engine count matches matrix pair count`() {
        val matrixCount = DocumentFormat.entries.sumOf { CapabilityMatrix.supportedTargets(it).size }
        assertEquals(matrixCount, orchestrator.allEngines.size)
    }

    @Test
    fun `readers exist for all formats`() {
        for (format in DocumentFormat.entries) {
            assertNotNull("No reader for $format", orchestrator.getReader(format))
        }
    }
}
