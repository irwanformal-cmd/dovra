package com.docuconvert.app.conversion

import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.WorkbookData
import com.docuconvert.app.domain.PresentationData
import kotlinx.coroutines.CoroutineScope
import java.io.File
import com.docuconvert.app.domain.Result

/**
 * Writes a structured document model to a File.
 * Each writer is a stateless singleton; engines pick the right writer
 * for the target format.
 */
interface DocumentWriter {
    /** Format this writer produces (must match CapabilityMatrix). */
    val targetFormat: DocumentFormat

    /**
     * Writes generic document content (text-like) to [outputFile].
     * Used for TXT/MD/HTML/XML/RTF/PDF text output.
     */
    suspend fun write(
        content: DocumentContent,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError>

    /**
     * Optional: writes spreadsheet workbook.
     * Default throws UnsupportedConversion.
     */
    suspend fun writeWorkbook(
        workbook: WorkbookData,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> =
        Result.failure(ConversionError.UnsupportedConversion)

    /**
     * Optional: writes presentation.
     * Default throws UnsupportedConversion.
     */
    suspend fun writePresentation(
        presentation: PresentationData,
        outputFile: File,
        scope: CoroutineScope
    ): Result<Unit, ConversionError> =
        Result.failure(ConversionError.UnsupportedConversion)
}