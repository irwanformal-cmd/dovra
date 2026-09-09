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
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

/** Reads CSV and TSV into a single Table block. */
class CsvReader(private val format: DocumentFormat) : DocumentReader {

    init {
        require(format == DocumentFormat.CSV || format == DocumentFormat.TSV)
    }

    override val supportedFormat: DocumentFormat = format

    override suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@withContext Result.failure(ConversionError.CorruptedFile)
                val csvFormat = if (format == DocumentFormat.TSV) CSVFormat.TDF else CSVFormat.DEFAULT
                val rows = mutableListOf<List<String>>()
                input.bufferedReader().use { reader ->
                    CSVParser(reader, csvFormat).use { parser ->
                        for (record in parser) {
                            rows.add(record.toList())
                        }
                    }
                }
                if (rows.isEmpty()) {
                    return@withContext Result.success(DocumentContent(null, emptyList()))
                }
                Result.success(
                    DocumentContent(
                        title = null,
                        blocks = listOf(ContentBlock.Table(rows, headerRow = true))
                    )
                )
            }
        } catch (e: OutOfMemoryError) {
            Result.failure(ConversionError.InsufficientStorage)
        } catch (e: Exception) {
            Result.failure(ConversionError.CorruptedFile)
        }
    }
}
