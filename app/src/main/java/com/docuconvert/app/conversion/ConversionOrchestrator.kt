package com.docuconvert.app.conversion

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.readers.CsvReader
import com.docuconvert.app.conversion.readers.DocReader
import com.docuconvert.app.conversion.readers.DocxReader
import com.docuconvert.app.conversion.readers.EpubReader
import com.docuconvert.app.conversion.readers.HtmlReader
import com.docuconvert.app.conversion.readers.MarkdownReader
import com.docuconvert.app.conversion.readers.OdpReader
import com.docuconvert.app.conversion.readers.OdsReader
import com.docuconvert.app.conversion.readers.OdtReader
import com.docuconvert.app.conversion.readers.PdfReader
import com.docuconvert.app.conversion.readers.PlainTextReader
import com.docuconvert.app.conversion.readers.PptReader
import com.docuconvert.app.conversion.readers.PptxReader
import com.docuconvert.app.conversion.readers.RtfReader
import com.docuconvert.app.conversion.readers.SpreadsheetReader
import com.docuconvert.app.conversion.readers.XlsReader
import com.docuconvert.app.conversion.readers.XlsxReader
import com.docuconvert.app.conversion.readers.XmlReader
import com.docuconvert.app.conversion.writers.CsvWriter
import com.docuconvert.app.conversion.writers.DocxWriter
import com.docuconvert.app.conversion.writers.HtmlWriter
import com.docuconvert.app.conversion.writers.MarkdownWriter
import com.docuconvert.app.conversion.writers.OdtWriter
import com.docuconvert.app.conversion.writers.PdfWriter
import com.docuconvert.app.conversion.writers.PlainTextWriter
import com.docuconvert.app.conversion.writers.RtfWriter
import com.docuconvert.app.conversion.writers.XmlWriter
import com.docuconvert.app.conversion.writers.XlsxWriter
import com.docuconvert.app.domain.CapabilityMatrix
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.ConversionResult
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default orchestrator: reader → model → writer, with progress,
 * timeout, cancellation and typed error mapping (§8, §12).
 *
 * Conversion timeout: 2 minutes per file (configurable for tests).
 */
class ConversionOrchestrator(
    private val initPdfLoader: suspend () -> Unit = {},
    private val timeoutMs: Long = 120_000L
) : ConversionEngineRegistry {

    private val readers: Map<DocumentFormat, DocumentReader> = mapOf(
        DocumentFormat.TXT to PlainTextReader(DocumentFormat.TXT),
        DocumentFormat.MD to MarkdownReader(),
        DocumentFormat.HTML to HtmlReader(),
        DocumentFormat.XML to XmlReader(),
        DocumentFormat.RTF to RtfReader(),
        DocumentFormat.DOC to DocReader(),
        DocumentFormat.DOCX to DocxReader(),
        DocumentFormat.ODT to OdtReader(),
        DocumentFormat.XLSX to XlsxReader(),
        DocumentFormat.XLS to XlsReader(),
        DocumentFormat.ODS to OdsReader(),
        DocumentFormat.CSV to CsvReader(DocumentFormat.CSV),
        DocumentFormat.TSV to CsvReader(DocumentFormat.TSV),
        DocumentFormat.PPTX to PptxReader(),
        DocumentFormat.PPT to PptReader(),
        DocumentFormat.ODP to OdpReader(),
        DocumentFormat.PDF to PdfReader(initPdfLoader),
        DocumentFormat.EPUB to EpubReader()
    )

    private val genericWriters: Map<DocumentFormat, DocumentWriter> = mapOf(
        DocumentFormat.TXT to PlainTextWriter(DocumentFormat.TXT),
        DocumentFormat.MD to MarkdownWriter(),
        DocumentFormat.HTML to HtmlWriter(),
        DocumentFormat.XML to XmlWriter(),
        DocumentFormat.RTF to RtfWriter(),
        DocumentFormat.DOCX to DocxWriter(),
        DocumentFormat.ODT to OdtWriter(),
        DocumentFormat.CSV to CsvWriter(DocumentFormat.CSV),
        DocumentFormat.TSV to CsvWriter(DocumentFormat.TSV),
        DocumentFormat.XLSX to XlsxWriter(DocumentFormat.XLSX),
        DocumentFormat.XLS to XlsxWriter(DocumentFormat.XLS),
        DocumentFormat.PDF to PdfWriter(initPdfLoader)
    )

    private val engines: List<ConversionEngine> by lazy {
        CapabilityMatrix.let { matrix ->
            buildList {
                for (source in DocumentFormat.entries) {
                    for (cap in matrix.supportedTargets(source)) {
                        val reader = readers[source] ?: continue
                        val writer = genericWriters[cap.target] ?: continue
                        add(ReaderWriterEngine(source, cap.target, reader, writer, timeoutMs))
                    }
                }
            }
        }
    }

    override fun getEngine(source: DocumentFormat, target: DocumentFormat): ConversionEngine? =
        engines.firstOrNull { it.sourceFormat == source && it.targetFormat == target }

    override val allEngines: List<ConversionEngine> get() = engines

    /** Reader lookup for the viewer (open without converting). */
    fun getReader(format: DocumentFormat): DocumentReader? = readers[format]
}

/**
 * Generic engine: read source → structured model → write target.
 * Progress: 0–50% reading, 50–100% writing.
 */
internal class ReaderWriterEngine(
    override val sourceFormat: DocumentFormat,
    override val targetFormat: DocumentFormat,
    private val reader: DocumentReader,
    private val writer: DocumentWriter,
    private val timeoutMs: Long
) : ConversionEngine {

    override suspend fun convert(
        sourceUri: Uri,
        outputFile: File,
        cr: ContentResolver,
        scope: CoroutineScope,
        progress: (Float) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                progress(0.05f)
                val readResult = reader.read(sourceUri, cr, this)
                val content: DocumentContent = when (readResult) {
                    is Result.Success -> readResult.value
                    is Result.Failure -> return@withTimeout ConversionResult.Failure(readResult.error)
                }
                progress(0.5f)
                val writeResult = writer.write(content, outputFile, this)
                when (writeResult) {
                    is Result.Success -> {
                        progress(1.0f)
                        ConversionResult.Success(outputFile, targetFormat)
                    }
                    is Result.Failure -> ConversionResult.Failure(writeResult.error)
                }
            }
        } catch (e: TimeoutCancellationException) {
            outputFile.delete()
            ConversionResult.Failure(ConversionError.Timeout)
        } catch (e: CancellationException) {
            outputFile.delete()
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            val msg = e.message.orEmpty().lowercase()
            ConversionResult.Failure(
                when {
                    "enospc" in msg || "no space" in msg -> ConversionError.InsufficientStorage
                    "permission" in msg || "denied" in msg -> ConversionError.PermissionDenied
                    else -> ConversionError.EngineFailure(e.message.orEmpty())
                }
            )
        }
    }
}
