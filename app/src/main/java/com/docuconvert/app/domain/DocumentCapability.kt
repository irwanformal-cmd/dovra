package com.docuconvert.app.domain

/**
 * How well a conversion is supported. The UI MUST only show targets
 * whose support level is not [ConversionSupport.NOT_SUPPORTED].
 */
enum class ConversionSupport {
    /** Full-fidelity conversion, layout preserved as much as the format allows. */
    FULL,

    /** Conversion works but drops fidelity (e.g. DOCX→PDF = simplified text layout). */
    SIMPLIFIED,

    /** Not supported — never show a Convert button for this pair. */
    NOT_SUPPORTED
}

/** One row of the capability matrix: source → target with honesty label. */
data class ConversionCapability(
    val source: DocumentFormat,
    val target: DocumentFormat,
    val support: ConversionSupport,
    /** Short user-facing note shown next to SIMPLIFIED targets, e.g. "Text layout only". */
    val note: String? = null
)

/**
 * THE CAPABILITY MATRIX — single source of truth for what the app claims.
 *
 * Rules:
 * 1. UI builds its "Convert to" list ONLY from [supportedTargets].
 * 2. [ConversionEngineRegistry] must have an engine for every FULL/SIMPLIFIED pair.
 * 3. Anything not listed here is implicitly NOT_SUPPORTED and must never
 *    appear as a conversion option.
 */
object CapabilityMatrix {

    private val capabilities: List<ConversionCapability> = buildList {
        // ---------- PDF ----------
        add(ConversionCapability(DocumentFormat.PDF, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.PDF, DocumentFormat.HTML, ConversionSupport.SIMPLIFIED, "Text layout only"))
        add(ConversionCapability(DocumentFormat.PDF, DocumentFormat.MD, ConversionSupport.SIMPLIFIED, "Text layout only"))

        // ---------- DOCX (priority format) ----------
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Simplified layout"))
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.RTF, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.ODT, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.DOCX, DocumentFormat.MD, ConversionSupport.SIMPLIFIED, "Basic formatting"))

        // ---------- DOC (legacy, read-only source) ----------
        add(ConversionCapability(DocumentFormat.DOC, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.DOC, DocumentFormat.HTML, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.DOC, DocumentFormat.DOCX, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.DOC, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Simplified layout"))

        // ---------- ODT ----------
        add(ConversionCapability(DocumentFormat.ODT, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.ODT, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.ODT, DocumentFormat.DOCX, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.ODT, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Simplified layout"))

        // ---------- TXT / MD / HTML / XML / RTF ----------
        add(ConversionCapability(DocumentFormat.TXT, DocumentFormat.MD, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.TXT, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.TXT, DocumentFormat.PDF, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.MD, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.MD, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.MD, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Rendered text"))
        add(ConversionCapability(DocumentFormat.HTML, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.HTML, DocumentFormat.MD, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.HTML, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Rendered text"))
        add(ConversionCapability(DocumentFormat.RTF, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.RTF, DocumentFormat.HTML, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.RTF, DocumentFormat.DOCX, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.RTF, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Simplified layout"))
        add(ConversionCapability(DocumentFormat.XML, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XML, DocumentFormat.HTML, ConversionSupport.SIMPLIFIED, "Syntax highlighted"))

        // ---------- Spreadsheet ----------
        add(ConversionCapability(DocumentFormat.XLSX, DocumentFormat.CSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XLSX, DocumentFormat.TSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XLSX, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XLSX, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Table grid only"))
        add(ConversionCapability(DocumentFormat.XLSX, DocumentFormat.XLS, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.XLS, DocumentFormat.CSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XLS, DocumentFormat.TSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.XLS, DocumentFormat.XLSX, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.ODS, DocumentFormat.CSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.ODS, DocumentFormat.TSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.ODS, DocumentFormat.XLSX, ConversionSupport.SIMPLIFIED, "Basic formatting"))
        add(ConversionCapability(DocumentFormat.ODS, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.CSV, DocumentFormat.XLSX, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.CSV, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.CSV, DocumentFormat.TSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.CSV, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Table grid only"))
        add(ConversionCapability(DocumentFormat.TSV, DocumentFormat.CSV, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.TSV, DocumentFormat.XLSX, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.TSV, DocumentFormat.HTML, ConversionSupport.FULL))

        // ---------- Presentation ----------
        add(ConversionCapability(DocumentFormat.PPTX, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Slide outline only"))
        add(ConversionCapability(DocumentFormat.PPTX, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.PPTX, DocumentFormat.HTML, ConversionSupport.SIMPLIFIED, "Slide outline only"))
        add(ConversionCapability(DocumentFormat.PPT, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.PPT, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Slide outline only"))
        add(ConversionCapability(DocumentFormat.ODP, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.ODP, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Slide outline only"))

        // ---------- EPUB ----------
        add(ConversionCapability(DocumentFormat.EPUB, DocumentFormat.TXT, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.EPUB, DocumentFormat.HTML, ConversionSupport.FULL))
        add(ConversionCapability(DocumentFormat.EPUB, DocumentFormat.PDF, ConversionSupport.SIMPLIFIED, "Rendered text"))
        add(ConversionCapability(DocumentFormat.EPUB, DocumentFormat.MD, ConversionSupport.SIMPLIFIED, "Basic formatting"))
    }

    private val bySource: Map<DocumentFormat, List<ConversionCapability>> =
        capabilities.groupBy { it.source }

    /** All convertible targets for [source], excluding NOT_SUPPORTED. Never empty-check needed by UI. */
    fun supportedTargets(source: DocumentFormat): List<ConversionCapability> =
        bySource[source].orEmpty()

    /** True only if source→target is FULL or SIMPLIFIED. */
    fun isSupported(source: DocumentFormat, target: DocumentFormat): Boolean =
        bySource[source]?.any { it.target == target } == true

    /** Support level, or NOT_SUPPORTED when unlisted. */
    fun supportLevel(source: DocumentFormat, target: DocumentFormat): ConversionSupport =
        bySource[source]?.firstOrNull { it.target == target }?.support
            ?: ConversionSupport.NOT_SUPPORTED

    /** Formats the app can open for viewing (all registered formats). */
    fun openableFormats(): Set<DocumentFormat> = DocumentFormat.entries.toSet()

    /** Total declared conversion pairs (for tests / diagnostics). */
    fun pairCount(): Int = capabilities.size
}
