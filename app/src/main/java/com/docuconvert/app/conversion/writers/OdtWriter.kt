package com.docuconvert.app.conversion.writers

import com.docuconvert.app.conversion.DocumentWriter
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.dom.OdfContentDom
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement
import org.odftoolkit.odfdom.dom.element.text.TextHElement
import org.odftoolkit.odfdom.dom.element.text.TextListElement
import org.odftoolkit.odfdom.dom.element.text.TextListItemElement
import org.odftoolkit.odfdom.dom.element.text.TextPElement
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement
import org.odftoolkit.odfdom.dom.element.table.TableTableElement
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement

/**
 * ODT writer via ODFDOM 0.9.0.
 * Creates a standards-compliant OpenDocument Text file.
 */
class OdtWriter : DocumentWriter {

    override val targetFormat: DocumentFormat = DocumentFormat.ODT

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            val doc = OdfTextDocument.newTextDocument()
            try {
                val contentDom: OdfContentDom = doc.contentDom
                val root: OfficeTextElement = doc.getContentRoot()

                content.title?.let {
                    appendHeading(contentDom, root, it, 1)
                }

                var currentList: TextListElement? = null
                for (block in content.blocks) {
                    when (block) {
                        is ContentBlock.Heading -> {
                            currentList = null
                            appendHeading(contentDom, root, block.text, block.level.coerceIn(1, 6))
                        }
                        is ContentBlock.Paragraph -> {
                            currentList = null
                            appendParagraph(contentDom, root, block.text)
                        }
                        is ContentBlock.ListItem -> {
                            if (currentList == null) {
                                currentList = contentDom.newOdfElement(TextListElement::class.java)
                                root.appendChild(currentList)
                            }
                            val item = contentDom.newOdfElement(TextListItemElement::class.java)
                            val p = contentDom.newOdfElement(TextPElement::class.java)
                            p.setTextContent(block.text)
                            item.appendChild(p)
                            currentList!!.appendChild(item)
                        }
                        is ContentBlock.Table -> {
                            currentList = null
                            appendTable(contentDom, root, block.rows)
                        }
                        is ContentBlock.ImagePlaceholder -> {
                            currentList = null
                            appendParagraph(contentDom, root, "[Image: ${block.altText}]")
                        }
                        is ContentBlock.PageBreak -> {
                            currentList = null
                            appendParagraph(contentDom, root, "")
                        }
                    }
                }

                FileOutputStream(outputFile).use { doc.save(it) }
                Result.success(Unit)
            } finally {
                doc.close()
            }
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }

    private fun appendHeading(
        dom: OdfContentDom,
        root: OfficeTextElement,
        text: String,
        level: Int
    ) {
        val h = dom.newOdfElement(TextHElement::class.java)
        h.setAttribute("text:outline-level", level.toString())
        h.setTextContent(text)
        root.appendChild(h)
    }

    private fun appendParagraph(
        dom: OdfContentDom,
        root: OfficeTextElement,
        text: String
    ) {
        val p = dom.newOdfElement(TextPElement::class.java)
        p.setTextContent(text)
        root.appendChild(p)
    }

    private fun appendTable(
        dom: OdfContentDom,
        root: OfficeTextElement,
        rows: List<List<String>>
    ) {
        if (rows.isEmpty()) return
        val table = dom.newOdfElement(TableTableElement::class.java)
        table.setAttribute("table:name", "Table1")
        for (row in rows) {
            val tr = dom.newOdfElement(TableTableRowElement::class.java)
            for (cell in row) {
                val tc = dom.newOdfElement(TableTableCellElement::class.java)
                val p = dom.newOdfElement(TextPElement::class.java)
                p.setTextContent(cell)
                tc.appendChild(p)
                tr.appendChild(tc)
            }
            table.appendChild(tr)
        }
        root.appendChild(table)
    }
}