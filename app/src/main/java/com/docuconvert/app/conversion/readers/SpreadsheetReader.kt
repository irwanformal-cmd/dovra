package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import com.docuconvert.app.domain.ContentBlock
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.w3c.dom.Node

/** Base class for XLSX / XLS / ODS spreadsheet reading. */
abstract class SpreadsheetReader(
    override val supportedFormat: DocumentFormat
) : DocumentReader {

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val workbook = WorkbookFactory.create(input)
                workbook.close()
                val blocks = mutableListOf<com.docuconvert.app.domain.ContentBlock>()
                for (i in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(i)
                    val name = sheet.sheetName
                    val rows = mutableListOf<List<String>>()
                    for (rowNum in 0..sheet.lastRowNum) {
                        val row = sheet.getRow(rowNum)
                        if (row != null) {
                            val cells = mutableListOf<String>()
                            for (cellNum in 0..row.lastCellNum) {
                                val cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                                cells.add(cellString(cell))
                            }
                            rows.add(cells)
                        }
                    }
                    if (rows.isNotEmpty()) {
                        blocks.add(com.docuconvert.app.domain.ContentBlock.Table(rows, headerRow = true))
                        blocks.add(com.docuconvert.app.domain.ContentBlock.Paragraph("--- Sheet: $name ---"))
                    }
                }
                Result.success(DocumentContent(null, blocks))
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun cellString(cell: Cell): String = when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue
        CellType.NUMERIC -> cell.numericCellValue.toString()
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        CellType.FORMULA -> cell.cellFormula
        else -> ""
    }
}

class XlsxReader : SpreadsheetReader(DocumentFormat.XLSX)
class XlsReader : SpreadsheetReader(DocumentFormat.XLS)

/** ODS reader using ODFDOM (works like ODT but for spreadsheets). */
class OdsReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.ODS

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val pkg = OdfPackage.loadPackage(input)
                pkg.use { p ->
                    val doc = OdfSpreadsheetDocument.loadDocument(p)
                    val blocks = mutableListOf<ContentBlock>()
                    val root = doc.getContentRoot()
                    walkOdsTable(root, blocks)
                    Result.success(DocumentContent(null, blocks))
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun walkOdsTable(node: Node, out: MutableList<ContentBlock>) {
        val tableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
        when {
            node.namespaceURI == tableNs && ("table" == node.localName || "table" == node.nodeName) -> {
                val rows = mutableListOf<List<String>>()
                var child = node.firstChild
                while (child != null) {
                    if (child.namespaceURI == tableNs && ("table-row" == child.localName || "row" == child.localName)) {
                        val cells = mutableListOf<String>()
                        var cellChild = child.firstChild
                        while (cellChild != null) {
                            if (cellChild.namespaceURI == tableNs && ("table-cell" == cellChild.localName || "cell" == cellChild.localName)) {
                                cells.add(cellChild.textContent?.trim().orEmpty())
                            }
                            cellChild = cellChild.nextSibling
                        }
                        if (cells.isNotEmpty()) rows.add(cells)
                    }
                    child = child.nextSibling
                }
                if (rows.isNotEmpty()) out.add(ContentBlock.Table(rows, headerRow = true))
            }
        }
        var child = node.firstChild
        while (child != null) {
            walkOdsTable(child, out)
            child = child.nextSibling
        }
    }
}