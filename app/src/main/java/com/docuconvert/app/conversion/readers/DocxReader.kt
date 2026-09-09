package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable

/**
 * DOCX reader via Apache POI (XWPF). Extracts headings, paragraphs,
 * lists, tables and image placeholders. Macros are never executed —
 * POI only reads document.xml content, never vbaProject.bin.
 */
class DocxReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.DOCX

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                XWPFDocument(input).use { doc ->
                    val blocks = mutableListOf<ContentBlock>()
                    val bodyElements = doc.bodyElements
                    for (el in bodyElements) {
                        when (el) {
                            is XWPFParagraph -> paragraphToBlock(el)?.let { blocks.add(it) }
                            is XWPFTable -> blocks.add(tableToBlock(el))
                            else -> { /* ignore drawings anchored outside paragraphs */ }
                        }
                    }
                    val title = runCatching {
                        doc.properties?.coreProperties?.title
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    Result.success(DocumentContent(title, blocks))
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun paragraphToBlock(p: XWPFParagraph): ContentBlock? {
        val text = p.text?.trim().orEmpty()
        val hasImage = p.runs.any { it.embeddedPictures.isNotEmpty() }
        if (text.isEmpty() && !hasImage) return null
        val style = p.style?.uppercase().orEmpty()
        if (style.startsWith("HEADING")) {
            val level = style.removePrefix("HEADING").trim().toIntOrNull() ?: 1
            return ContentBlock.Heading(text.ifBlank { " " }, level.coerceIn(1, 6))
        }
        if (p.numID != null) {
            return ContentBlock.ListItem(text, ordered = false)
        }
        if (hasImage && text.isEmpty()) {
            return ContentBlock.ImagePlaceholder("Image")
        }
        val bold = p.runs.any { it.isBold }
        val italic = p.runs.any { it.isItalic }
        var result: ContentBlock = ContentBlock.Paragraph(text, bold, italic)
        if (hasImage) {
            // Keep both text and a placeholder marker.
            return ContentBlock.Paragraph(if (text.isEmpty()) "[Image]" else "$text [Image]", bold, italic)
        }
        return result
    }

    private fun tableToBlock(table: XWPFTable): ContentBlock {
        val rows = table.rows.map { row ->
            row.tableCells.map { cell -> cell.text?.trim().orEmpty() }
        }
        return ContentBlock.Table(rows.ifEmpty { listOf(listOf("")) }, headerRow = true)
    }
}
