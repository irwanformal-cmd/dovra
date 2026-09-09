package com.docuconvert.app.conversion.writers

import com.docuconvert.app.conversion.DocumentWriter
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import com.docuconvert.app.domain.WorkbookData
import com.docuconvert.app.domain.PresentationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.ParagraphAlignment

/**
 * DOCX writer via Apache POI XWPF.
 * Rebuilds headings, paragraphs, lists, tables from DocumentContent.
 * Image placeholders become empty paragraphs with "[Image]" text.
 */
class DocxWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.DOCX

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val doc = XWPFDocument()
            try {
                content.title?.let {
                    val h = doc.createParagraph()
                    h.setAlignment(ParagraphAlignment.LEFT)
                    val r = h.createRun()
                    r.setBold(true)
                    r.setFontSize(16)
                    r.setText(it)
                }
                for (block in content.blocks) {
                    when (block) {
                        is ContentBlock.Heading -> {
                            val p = doc.createParagraph()
                            val r = p.createRun()
                            r.setBold(true)
                            r.setFontSize(when (block.level) { 1 -> 20; 2 -> 16; 3 -> 14; else -> 12 })
                            r.setText(block.text)
                        }
                        is ContentBlock.Paragraph -> {
                            val p = doc.createParagraph()
                            val r = p.createRun()
                            if (block.bold) r.setBold(true)
                            if (block.italic) r.setItalic(true)
                            r.setText(block.text)
                        }
                        is ContentBlock.ListItem -> {
                            val p = doc.createParagraph()
                            p.setIndentationLeft(720)
                            val r = p.createRun()
                            r.setText("• ${block.text}")
                        }
                        is ContentBlock.Table -> {
                            if (block.rows.isNotEmpty()) {
                                val cols = block.rows.maxByOrNull { it.size }?.size ?: 1
                                val table = doc.createTable(block.rows.size, cols)
                                block.rows.forEachIndexed { ri, row ->
                                    val tr = table.getRow(ri)
                                    row.forEachIndexed { ci, cell ->
                                        tr.getCell(ci).setText(cell)
                                    }
                                }
                            }
                        }
                        is ContentBlock.ImagePlaceholder -> {
                            val p = doc.createParagraph()
                            val r = p.createRun()
                            r.setItalic(true)
                            r.setText("[Image: ${block.altText}]")
                        }
                        is ContentBlock.PageBreak -> doc.createParagraph().createRun().addBreak()
                    }
                }
                FileOutputStream(outputFile).use { doc.write(it) }
                Result.success(Unit)
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }

    override suspend fun writeWorkbook(
        workbook: WorkbookData,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> =
        Result.failure(ConversionError.UnsupportedConversion)

    override suspend fun writePresentation(
        presentation: PresentationData,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> =
        Result.failure(ConversionError.UnsupportedConversion)
}