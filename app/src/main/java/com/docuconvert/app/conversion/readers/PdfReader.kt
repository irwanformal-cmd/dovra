package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.Result
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PDF text extractor via PDFBox-Android (TomRoush).
 * Page-aware: emits a PageBreak between pages so downstream writers
 * (HTML/MD) can preserve pagination markers.
 */
class PdfReader(
    private val initLoader: suspend () -> Unit
) : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.PDF

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            initLoader()
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                // Copy to memory-bounded buffer: PDDocument needs a seekable stream.
                val bytes = input.readBytes()
                if (bytes.size < 5 || String(bytes.take(5).toByteArray()) != "%PDF-") {
                    return@withContext Result.failure(ConversionError.CorruptedFile)
                }
                PDDocument.load(bytes).use { doc ->
                    if (doc.isEncrypted) {
                        return@withContext Result.failure(ConversionError.CorruptedFile)
                    }
                    val stripper = PDFTextStripper()
                    val blocks = mutableListOf<ContentBlock>()
                    val title = doc.documentInformation?.title?.takeIf { it.isNotBlank() }
                    for (pageNum in 1..doc.numberOfPages) {
                        stripper.startPage = pageNum
                        stripper.endPage = pageNum
                        val pageText = stripper.getText(doc).trim()
                        if (pageText.isNotEmpty()) {
                            // Heuristic: short first line of first page = heading.
                            val lines = pageText.split("\n").map { it.trimEnd() }
                            lines.forEach { line ->
                                if (line.isNotBlank()) blocks.add(ContentBlock.Paragraph(line))
                            }
                        }
                        if (pageNum < doc.numberOfPages) blocks.add(ContentBlock.PageBreak)
                    }
                    Result.success(DocumentContent(title, blocks))
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if ("password" in msg.lowercase() || "encrypted" in msg.lowercase()) {
                Result.failure(ConversionError.CorruptedFile)
            } else {
                Result.failure(ConversionError.CorruptedFile)
            }
        }
    }
}
