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
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor

/**
 * Legacy DOC (OLE2) reader via POI HWPF. Extracts paragraphs only.
 * Tables/macros/vbaProject streams are not read — only WordDocument stream text.
 */
class DocReader : DocumentReader {

    override val supportedFormat: DocumentFormat = DocumentFormat.DOC

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                HWPFDocument(input).use { doc ->
                    WordExtractor(doc).use { extractor ->
                        val blocks = mutableListOf<ContentBlock>()
                        for (para in extractor.paragraphText) {
                            val t = para.trim()
                            // Skip page-break markers and empty runs.
                            if (t.isNotEmpty() && t != "\u0007") {
                                blocks.add(ContentBlock.Paragraph(t))
                            }
                        }
                        Result.success(DocumentContent(null, blocks))
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}