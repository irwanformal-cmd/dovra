package com.docuconvert.app.conversion

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionError
import kotlinx.coroutines.CoroutineScope
import com.docuconvert.app.domain.Result

/**
 * Reads a document from a content Uri and produces [DocumentContent].
 * Implementations must be thread-safe and never block the caller.
 */
interface DocumentReader {
    /** Format this reader handles (must match CapabilityMatrix). */
    val supportedFormat: DocumentFormat

    /**
     * Reads the document at [uri] into a structured model.
     * Must not read the entire file into memory at once — use streaming
     * where the underlying library permits (§32).
     */
    suspend fun read(
        uri: Uri,
        contentResolver: ContentResolver,
        scope: CoroutineScope
    ): Result<DocumentContent, ConversionError>
}