package com.docuconvert.app.domain

/**
 * Typed conversion failures. UI maps these to user-friendly messages —
 * NEVER show stack traces or exception text to the user (§12).
 */
sealed class ConversionError(val userMessage: String) {
    data object UnsupportedConversion : ConversionError("This conversion is not supported.")
    data object CorruptedFile : ConversionError("The file appears to be corrupted or invalid.")
    data object InsufficientStorage : ConversionError("Not enough storage space to complete the conversion.")
    data object PermissionDenied : ConversionError("Permission was denied. Please try selecting the file again.")
    data object Timeout : ConversionError("Conversion took too long and was stopped.")
    data object Cancelled : ConversionError("Conversion was cancelled.")
    data class EngineFailure(val reason: String) : ConversionError("Unable to convert this document.")
}

/** Result of one conversion job. Success carries the output file location. */
sealed interface ConversionResult {
    data class Success(val outputFile: java.io.File, val target: DocumentFormat) : ConversionResult
    data class Failure(val error: ConversionError) : ConversionResult
}
