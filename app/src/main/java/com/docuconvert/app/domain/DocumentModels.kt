package com.docuconvert.app.domain

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.docuconvert.app.domain.ContentBlock

/**
 * A document the user picked via SAF. Holds only metadata + Uri —
 * NEVER the file bytes (streaming only, §32 performance).
 */
@Parcelize
data class DocumentInfo(
    val uri: Uri,
    val displayName: String,
    val format: DocumentFormat,
    val mimeType: String?,
    val sizeBytes: Long,
    /** True when magic-byte sniffing disagrees with the extension. */
    val signatureMismatch: Boolean = false
) : Parcelable

/** Structured text model every reader produces and every writer consumes. */
data class DocumentContent(
    val title: String?,
    val blocks: List<ContentBlock>,
    val metadata: Map<String, String> = emptyMap()
) {
    /** Plain-text rendering used by TXT writers and search indexing. */
    fun toPlainText(): String = buildString {
        title?.let { appendLine(it).appendLine() }
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Heading -> appendLine(block.text).appendLine()
                is ContentBlock.Paragraph -> appendLine(block.text).appendLine()
                is ContentBlock.ListItem -> appendLine("• ${block.text}")
                is ContentBlock.Table -> {
                    block.rows.forEach { row -> appendLine(row.joinToString("\t")) }
                    appendLine()
                }
                is ContentBlock.ImagePlaceholder -> appendLine("[Image: ${block.altText}]")
                is ContentBlock.PageBreak -> appendLine("---")
            }
        }
    }

    val isEmpty: Boolean get() = title.isNullOrBlank() && blocks.isEmpty()
}

/** Smallest renderable unit. Rich enough for HTML/PDF rebuild, simple enough for every engine. */
sealed interface ContentBlock {
    data class Heading(val text: String, val level: Int = 1) : ContentBlock
    data class Paragraph(val text: String, val bold: Boolean = false, val italic: Boolean = false) : ContentBlock
    data class ListItem(val text: String, val ordered: Boolean = false, val index: Int = 0) : ContentBlock
    data class Table(val rows: List<List<String>>, val headerRow: Boolean = true) : ContentBlock
    data class ImagePlaceholder(val altText: String) : ContentBlock
    data object PageBreak : ContentBlock
}

/** Tabular model for spreadsheet readers/writers. */
data class SheetData(
    val name: String,
    val rows: List<List<String>>
)

data class WorkbookData(
    val sheets: List<SheetData>
)

/** Slide model for presentation readers. */
data class SlideData(
    val index: Int,
    val title: String?,
    val bullets: List<String>,
    val notes: String?
)

data class PresentationData(
    val title: String?,
    val slides: List<SlideData>
)
