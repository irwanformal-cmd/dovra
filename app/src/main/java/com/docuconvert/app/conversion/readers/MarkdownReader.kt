package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.commonmark.parser.Parser
import org.commonmark.node.Node
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph
import org.commonmark.node.ListItem
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Text
import com.docuconvert.app.domain.Result
import com.docuconvert.app.domain.ContentBlock

/** Reads Markdown and converts to structured blocks via CommonMark. */
class MarkdownReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.MD

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                input?.let { stream ->
                    val text = stream.bufferedReader().readText()
                    val parser = Parser.builder().build()
                    val document = parser.parse(text)
                    val blocks = mutableListOf<com.docuconvert.app.domain.ContentBlock>()

                    // Walk top-level blocks only (keeps nesting simple and honest)
                    var child = document.firstChild
                    while (child != null) {
                        when (child) {
                            is Heading -> blocks.add(
                                com.docuconvert.app.domain.ContentBlock.Heading(extractText(child), child.level)
                            )
                            is Paragraph -> {
                                val t = extractText(child)
                                if (t.isNotBlank()) blocks.add(com.docuconvert.app.domain.ContentBlock.Paragraph(t))
                            }
                            is org.commonmark.node.BulletList -> {
                                var li = child.firstChild
                                while (li != null) {
                                    if (li is ListItem) blocks.add(
                                        com.docuconvert.app.domain.ContentBlock.ListItem(extractText(li), false)
                                    )
                                    li = li.next
                                }
                            }
                            is org.commonmark.node.OrderedList -> {
                                var idx = 0
                                var li = child.firstChild
                                while (li != null) {
                                    if (li is ListItem) {
                                        idx++
                                        blocks.add(
                                            com.docuconvert.app.domain.ContentBlock.ListItem(extractText(li), true, idx)
                                        )
                                    }
                                    li = li.next
                                }
                            }
                            is FencedCodeBlock, is IndentedCodeBlock -> blocks.add(
                                com.docuconvert.app.domain.ContentBlock.Paragraph(extractText(child))
                            )
                            is ThematicBreak -> blocks.add(com.docuconvert.app.domain.ContentBlock.PageBreak)
                        }
                        child = child.next
                    }

                    Result.success(DocumentContent(null, blocks))
                } ?: Result.failure(ConversionError.CorruptedFile)
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun extractText(node: Node): String {
        val sb = StringBuilder()
        fun recurse(n: Node) {
            if (n is Text) sb.append(n.literal)
            var c = n.firstChild
            while (c != null) {
                recurse(c)
                c = c.next
            }
        }
        recurse(node)
        return sb.toString().trim()
    }
}
