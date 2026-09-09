package com.docuconvert.app.conversion.writers

import com.docuconvert.app.conversion.DocumentWriter
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import com.docuconvert.app.domain.WorkbookData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * CSV / TSV writer. Flattens the first Table block found; if there is no
 * table, falls back to one-cell-per-line plain text.
 */
class CsvWriter(
    override val targetFormat: DocumentFormat
) : DocumentWriter {

    init {
        require(targetFormat == DocumentFormat.CSV || targetFormat == DocumentFormat.TSV)
    }

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val format = if (targetFormat == DocumentFormat.TSV) CSVFormat.TDF else CSVFormat.DEFAULT
            FileOutputStream(outputFile).use { fos ->
                fos.bufferedWriter(Charsets.UTF_8).use { writer ->
                    CSVPrinter(writer, format).use { printer ->
                        val table = content.blocks.filterIsInstance<ContentBlock.Table>().firstOrNull()
                        if (table != null) {
                            for (row in table.rows) printer.printRecord(row)
                        } else {
                            // Fallback: one line per block.
                            for (block in content.blocks) {
                                when (block) {
                                    is ContentBlock.Heading -> printer.printRecord(block.text)
                                    is ContentBlock.Paragraph -> printer.printRecord(block.text)
                                    is ContentBlock.ListItem -> printer.printRecord(block.text)
                                    is ContentBlock.Table -> block.rows.forEach { printer.printRecord(it) }
                                    is ContentBlock.ImagePlaceholder -> printer.printRecord("[Image: ${block.altText}]")
                                    is ContentBlock.PageBreak -> printer.printRecord("---")
                                }
                            }
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }
}

/**
 * XLSX / XLS writer via POI.
 * - XLSX target: XSSFWorkbook (OOXML).
 * - XLS target: HSSFWorkbook (legacy BIFF) so the output genuinely matches
 *   the declared legacy format instead of mislabelled OOXML bytes.
 * Writes the first Table block as Sheet1.
 */
class XlsxWriter(
    override val targetFormat: DocumentFormat = DocumentFormat.XLSX
) : DocumentWriter {

    init {
        require(targetFormat == DocumentFormat.XLSX || targetFormat == DocumentFormat.XLS) {
            "XlsxWriter supports XLSX/XLS only, got $targetFormat"
        }
    }

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        writeWorkbook(
            workbookFromContent(content),
            outputFile,
            scope
        )
    }

    override suspend fun writeWorkbook(
        workbook: WorkbookData,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            // HSSF (XLS) caps: 65,536 rows x 256 columns, 31-char sheet names.
            val wb: org.apache.poi.ss.usermodel.Workbook =
                if (targetFormat == DocumentFormat.XLS) org.apache.poi.hssf.usermodel.HSSFWorkbook()
                else XSSFWorkbook()
            try {
                for (sheetData in workbook.sheets.ifEmpty { listOf(com.docuconvert.app.domain.SheetData("Sheet1", emptyList())) }) {
                    val sheet = wb.createSheet(sheetData.name.take(31))
                    sheetData.rows.forEachIndexed { ri, row ->
                        if (targetFormat == DocumentFormat.XLS && ri >= 65536) return@forEachIndexed
                        val r = sheet.createRow(ri)
                        row.forEachIndexed { ci, cell ->
                            if (targetFormat == DocumentFormat.XLS && ci >= 256) return@forEachIndexed
                            r.createCell(ci).setCellValue(cell)
                        }
                    }
                }
                FileOutputStream(outputFile).use { wb.write(it) }
                Result.success(Unit)
            } finally {
                (wb as? java.io.Closeable)?.close()
            }
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }

    private fun workbookFromContent(content: DocumentContent): WorkbookData {
        val table = content.blocks.filterIsInstance<ContentBlock.Table>().firstOrNull()
        return if (table != null) {
            WorkbookData(listOf(com.docuconvert.app.domain.SheetData("Sheet1", table.rows)))
        } else {
            val rows = content.blocks.map { block ->
                listOf(
                    when (block) {
                        is ContentBlock.Heading -> block.text
                        is ContentBlock.Paragraph -> block.text
                        is ContentBlock.ListItem -> block.text
                        is ContentBlock.Table -> block.rows.joinToString(" | ") { it.joinToString(", ") }
                        is ContentBlock.ImagePlaceholder -> "[Image: ${block.altText}]"
                        is ContentBlock.PageBreak -> "---"
                    }
                )
            }
            WorkbookData(listOf(com.docuconvert.app.domain.SheetData("Sheet1", rows)))
        }
    }
}
