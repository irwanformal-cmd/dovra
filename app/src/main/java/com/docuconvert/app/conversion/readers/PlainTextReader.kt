package com.docuconvert.app.conversion.readers

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.conversion.DocumentReader
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** Reads TXT, MD, CSV, TSV, XML (as plain text). */
class PlainTextReader(
    private val format: DocumentFormat
) : DocumentReader {

    override val supportedFormat: DocumentFormat = format

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                input?.let { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                    val lines = mutableListOf<String>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        lines.add(line!!)
                    }
                    val text = lines.joinToString("\n")
                    Result.success(DocumentContent(
                        title = null,
                        blocks = listOf(ContentBlock.Paragraph(text))
                    ))
                } ?: Result.failure(ConversionError.CorruptedFile)
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}