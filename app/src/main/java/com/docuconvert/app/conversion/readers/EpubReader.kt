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
import nl.siegmann.epublib.epub.EpubReader as LibEpubReader
import org.jsoup.Jsoup

/**
 * EPUB reader via epublib. Emits headings/paragraphs/lists per spine item,
 * with a PageBreak (chapter break) between spine resources so the viewer
 * can offer chapter navigation.
 */
class EpubReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.EPUB

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val book = LibEpubReader().readEpub(input)
                val blocks = mutableListOf<ContentBlock>()
                val title = book.title?.takeIf { it.isNotBlank() }
                val spine = book.spine.spineReferences
                spine.forEachIndexed { idx, ref ->
                    val res = ref.resource
                    val html = res.data?.toString(Charsets.UTF_8) ?: ""
                    if (html.isBlank()) return@forEachIndexed
                    val doc = Jsoup.parse(html)
                    val chapterTitle = doc.select("h1,h2").firstOrNull()?.text()
                    if (!chapterTitle.isNullOrBlank()) {
                        blocks.add(ContentBlock.Heading(chapterTitle, 1))
                    }
                    doc.select("h1,h2,h3,p,li").forEach { el ->
                        val t = el.text().trim()
                        if (t.isBlank()) return@forEach
                        when (el.tagName().lowercase()) {
                            "h1" -> if (t != chapterTitle) blocks.add(ContentBlock.Heading(t, 1))
                            "h2" -> if (t != chapterTitle) blocks.add(ContentBlock.Heading(t, 2))
                            "h3" -> blocks.add(ContentBlock.Heading(t, 3))
                            "li" -> blocks.add(ContentBlock.ListItem(t, false))
                            else -> blocks.add(ContentBlock.Paragraph(t))
                        }
                    }
                    if (idx < spine.size - 1) blocks.add(ContentBlock.PageBreak)
                }
                Result.success(DocumentContent(title, blocks))
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}
