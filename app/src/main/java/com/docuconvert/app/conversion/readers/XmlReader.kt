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
import org.w3c.dom.Document
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/** Reads XML, validates well-formedness, and flattens element text into paragraphs. */
class XmlReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.XML

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val factory = DocumentBuilderFactory.newInstance()
                // XXE hardening: never resolve external entities from untrusted docs.
                factory.isExpandEntityReferences = false
                runCatching {
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                }
                factory.isXIncludeAware = false
                val builder = factory.newDocumentBuilder()
                val doc: Document = builder.parse(input)
                val blocks = mutableListOf<ContentBlock>()
                val root = doc.documentElement
                val title = root?.nodeName
                collectText(root, blocks)
                Result.success(DocumentContent(title, blocks))
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun collectText(node: Node?, out: MutableList<ContentBlock>) {
        if (node == null) return
        val children = node.childNodes
        for (i in 0 until children.length) {
            val c = children.item(i)
            when (c.nodeType) {
                Node.ELEMENT_NODE -> {
                    val text = c.textContent?.trim().orEmpty()
                    // Leaf elements become paragraphs; containers recurse.
                    val hasElementChildren = (0 until c.childNodes.length).any {
                        c.childNodes.item(it).nodeType == Node.ELEMENT_NODE
                    }
                    if (!hasElementChildren && text.isNotBlank()) {
                        out.add(ContentBlock.Paragraph("${c.nodeName}: $text"))
                    } else {
                        collectText(c, out)
                    }
                }
                Node.TEXT_NODE -> {
                    val text = c.nodeValue?.trim().orEmpty()
                    if (text.isNotBlank() && out.isEmpty()) {
                        out.add(ContentBlock.Paragraph(text))
                    }
                }
            }
        }
    }
}
