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

/**
 * Writes DocumentContent as plain text (TXT, CSV, TSV).
 * Uses the existing toPlainText() on DocumentContent.
 */
class PlainTextWriter(
    override val targetFormat: DocumentFormat
) : DocumentWriter {

    override suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(outputFile).use { out ->
                out.write(content.toPlainText().toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ConversionError.EngineFailure(e.message.orEmpty()))
        }
    }
}