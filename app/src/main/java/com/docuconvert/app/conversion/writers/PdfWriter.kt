package com.docuconvert.app.conversion.writers

import com.docuconvert.app.conversion.DocumentWriter
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF writer via PDFBox-Android.
 *
 * This is a SIMPLIFIED text-only layout engine (§26). It lays out
 * text blocks sequentially with basic word-wrap and page breaks.
 * Complex layouts (multi-column, absolute positioning, fonts, images)
 * are NOT attempted — that would require a full typesetting engine.
 */
class PdfWriter(
    private val initLoader: suspend () -> Unit
) : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.PDF

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            initLoader()
            val doc = PDDocument()
            try {
                val pageSize = PDRectangle.A4
                val margin = 72f // 1 inch
                val font = PDType1Font.HELVETICA
                val fontSize = 11f
                val leading = fontSize * 1.2f

                var page = PDPage(pageSize)
                doc.addPage(page)
                var cs = PDPageContentStream(doc, page)
                cs.setFont(font, fontSize)
                cs.newLineAtOffset(margin, pageSize.height - margin)
                cs.setLeading(leading)

                fun newPage() {
                    cs.close()
                    page = PDPage(pageSize)
                    doc.addPage(page)
                    cs = PDPageContentStream(doc, page)
                    cs.setFont(font, fontSize)
                    cs.newLineAtOffset(margin, pageSize.height - margin)
                    cs.setLeading(leading)
                }

                fun writeLine(text: String, bold: Boolean = false, size: Float = fontSize, indent: Float = 0f) {
                    if (text.trim().isEmpty()) return
                    val maxWidth = pageSize.width - 2 * margin - indent
                    val testWidth = (font as PDType1Font).getStringWidth(text) / 1000f * size
                    if (testWidth <= maxWidth) {
                        cs.setFont(if (bold) PDType1Font.HELVETICA_BOLD else font, size)
                        cs.showText(text)
                        cs.newLine()
                    } else {
                        // simple word-wrap
                        val words = text.split(" ")
                        var line = ""
                        for (word in words) {
                            val testLine = if (line.isEmpty()) word else "$line $word"
                            val w = (font as PDType1Font).getStringWidth(testLine) / 1000f * size
                            if (w <= maxWidth) line = testLine else {
                                cs.setFont(if (bold) PDType1Font.HELVETICA_BOLD else font, size)
                                cs.showText(line)
                                cs.newLine()
                                line = word
                            }
                        }
                        if (line.isNotEmpty()) {
                            cs.setFont(if (bold) PDType1Font.HELVETICA_BOLD else font, size)
                            cs.showText(line)
                            cs.newLine()
                        }
                    }
                }

                content.title?.let {
                    writeLine(it, bold = true, size = 16f)
                    cs.newLine()
                }

                for (block in content.blocks) {
                    when (block) {
                        is ContentBlock.Heading -> {
                            val size = when (block.level) {
                                1 -> 18f
                                2 -> 16f
                                3 -> 14f
                                else -> 12f
                            }
                            writeLine(block.text, bold = true, size = size)
                            cs.newLine()
                        }
                        is ContentBlock.Paragraph -> writeLine(block.text, bold = block.bold)
                        is ContentBlock.ListItem -> writeLine("• ${block.text}", indent = 20f)
                        is ContentBlock.Table -> {
                            // Very simple grid: tab-separated, no borders.
                            cs.newLine()
                            for (row in block.rows) {
                                writeLine(row.joinToString(" \t "), indent = 10f)
                            }
                            cs.newLine()
                        }
                        is ContentBlock.ImagePlaceholder -> {
                            writeLine("[Image: ${block.altText}]", indent = 10f)
                        }
                        is ContentBlock.PageBreak -> newPage()
                    }
                }

                cs.close()
                doc.save(outputFile)
                Result.success(Unit)
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }
}