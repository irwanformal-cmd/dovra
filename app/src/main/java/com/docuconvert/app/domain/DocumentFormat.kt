package com.docuconvert.app.domain

/**
 * Supported document formats. CENTRAL REGISTRY — every format the app
 * knows about must be declared here exactly once.
 *
 * @property extension Lower-case file extension without dot.
 * @property mimeType Primary MIME type for SAF intent filters.
 * @property displayName Human-readable name shown in UI.
 */
enum class DocumentFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    // Text / markup
    TXT("txt", "text/plain", "Plain Text"),
    MD("md", "text/markdown", "Markdown"),
    HTML("html", "text/html", "HTML"),
    XML("xml", "text/xml", "XML"),
    RTF("rtf", "application/rtf", "Rich Text"),

    // Word processing
    DOC("doc", "application/msword", "Word 97-2003"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word Document"),
    ODT("odt", "application/vnd.oasis.opendocument.text", "OpenDocument Text"),

    // Spreadsheet
    XLS("xls", "application/vnd.ms-excel", "Excel 97-2003"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Workbook"),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet", "OpenDocument Spreadsheet"),
    CSV("csv", "text/csv", "CSV"),
    TSV("tsv", "text/tab-separated-values", "TSV"),

    // Presentation
    PPT("ppt", "application/vnd.ms-powerpoint", "PowerPoint 97-2003"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint Presentation"),
    ODP("odp", "application/vnd.oasis.opendocument.presentation", "OpenDocument Presentation"),

    // Fixed-layout / ebook
    PDF("pdf", "application/pdf", "PDF"),
    EPUB("epub", "application/epub+zip", "EPUB");

    companion object {
        private val byExtension: Map<String, DocumentFormat> =
            entries.associateBy { it.extension }

        private val byMime: Map<String, DocumentFormat> =
            entries.associateBy { it.mimeType }

        /** Extension-only lookup (fast path). Returns null for unknown extensions. */
        fun fromExtension(ext: String): DocumentFormat? =
            byExtension[ext.trim().lowercase().removePrefix(".")]

        /** MIME-type lookup. Returns null for unknown types. */
        fun fromMimeType(mime: String?): DocumentFormat? =
            mime?.trim()?.lowercase()?.let { byMime[it] }

        /** All extensions the app can open via SAF picker. */
        fun allExtensions(): Array<String> = entries.map { it.extension }.toTypedArray()

        /** All MIME types for SAF intent filters. */
        fun allMimeTypes(): Array<String> = entries.map { it.mimeType }.toTypedArray()
    }
}
