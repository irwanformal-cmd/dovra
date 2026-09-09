package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/** Reads HTML and extracts structured blocks using Jsoup. */
class HtmlReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.HTML

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                input?.let { stream ->
                    val doc = Jsoup.parse(stream, "UTF-8", "")
                    val blocks = mutableListOf<ContentBlock>()

                    // Extract title from <title> or first <h1>
                    val title = doc.select("title").first()?.text()
                        ?: doc.select("h1").first()?.text()

                    // Process body content
                    val body = doc.body() ?: doc
                    body.children().forEach { el ->
                        when (el.tagName().lowercase()) {
                            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                                val level = el.tagName().last().toString().toIntOrNull() ?: 1
                                blocks.add(ContentBlock.Heading(el.text(), level))
                            }
                            "p" -> blocks.add(ContentBlock.Paragraph(el.text()))
                            "ul", "ol" -> {
                                val ordered = el.tagName() == "ol"
                                el.select("li").forEachIndexed { idx, li ->
                                    blocks.add(ContentBlock.ListItem(li.text(), ordered, idx + 1))
                                }
                            }
                            "table" -> {
                                val rows = mutableListOf<List<String>>()
                                val header = el.select("thead tr th, tr:first-child th").map { it.text() }
                                if (header.isNotEmpty()) rows.add(header)
                                el.select("tbody tr, tr").forEach { row ->
                                    val cells = row.select("td, th").map { it.text() }
                                    if (cells.isNotEmpty()) rows.add(cells)
                                }
                                if (rows.isNotEmpty()) {
                                    blocks.add(ContentBlock.Table(rows, header.isNotEmpty()))
                                }
                            }
                            "hr" -> blocks.add(ContentBlock.PageBreak)
                            "img" -> blocks.add(ContentBlock.ImagePlaceholder(el.attr("alt").ifBlank { "Image" }))
                            else -> {
                                val text = el.text()
                                if (text.isNotBlank()) blocks.add(ContentBlock.Paragraph(text))
                            }
                        }
                    }
                    Result.success(DocumentContent(title, blocks))
                } ?: Result.failure(ConversionError.CorruptedFile)
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}