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
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.w3c.dom.Node

/**
 * ODT reader via ODFDOM 0.9.0. Reads text:h (headings), text:p (paragraphs),
 * text:list (lists) and table:table (tables). Macros/forms/embedded
 * objects are ignored — only visible text is extracted.
 */
class OdtReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.ODT

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                OdfPackage.loadPackage(input).use { pkg ->
                    val doc = OdfTextDocument.loadDocument(pkg)
                    val blocks = mutableListOf<ContentBlock>()
                    val root = doc.getContentRoot()
                    walkOdfDom(root, blocks)
                    Result.success(DocumentContent(null, blocks))
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }

    private fun walkOdfDom(node: Node, out: MutableList<ContentBlock>) {
        val ns = node.namespaceURI
        val ln = node.localName ?: node.nodeName
        val textNs = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
        val tableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"

        when {
            ns == textNs -> {
                when (ln) {
                    "h" -> out.add(ContentBlock.Heading(node.textContent?.trim().orEmpty(), 1))
                    "p" -> {
                        val txt = node.textContent?.trim().orEmpty()
                        if (txt.isNotEmpty()) out.add(ContentBlock.Paragraph(txt))
                    }
                    "list" -> {
                        val items = mutableListOf<Node>()
                        collectListItems(node, items)
                        items.forEachIndexed { idx, item ->
                            out.add(ContentBlock.ListItem(item.textContent?.trim().orEmpty(), false, idx + 1))
                        }
                    }
                    "table" -> {
                        val rows = mutableListOf<List<String>>()
                        walkTable(node, rows)
                        if (rows.isNotEmpty()) out.add(ContentBlock.Table(rows, headerRow = true))
                    }
                }
            }
        }
        var child = node.firstChild
        while (child != null) {
            walkOdfDom(child, out)
            child = child.nextSibling
        }
    }

    private fun collectListItems(node: Node, out: MutableList<Node>) {
        val nl = node.childNodes
        for (i in 0 until nl.length) {
            val c = nl.item(i)
            val textNs = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
            if (c.namespaceURI == textNs && ("list-item" == c.localName || "item" == c.localName)) {
                out.add(c)
            }
            collectListItems(c, out)
        }
    }

    private fun walkTable(tableNode: Node, rows: MutableList<List<String>>) {
        val tableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
        var child = tableNode.firstChild
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
    }
}