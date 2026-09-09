package com.docuconvert.app.conversion

import android.content.ContentResolver
import android.net.Uri
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.ConversionResult
import kotlinx.coroutines.CoroutineScope

/**
 * High-level conversion orchestrator. The UI never talks to readers/writers
 * directly — it only calls [ConversionEngine.convert].
 *
 * Implementations combine a [DocumentReader] + [DocumentWriter] (or
 * special-case logic like PDF rendering) and handle progress, cancellation,
 * temp files, and error mapping.
 */
interface ConversionEngine {
    /** Source format this engine reads. */
    val sourceFormat: DocumentFormat

    /** Target format this engine writes. */
    val targetFormat: DocumentFormat

    /**
     * Performs the full conversion.
     *
     * @param sourceUri SAF Uri of the input document.
     * @param outputFile Local file to write result to.
     * @param cr ContentResolver for opening source stream.
     * @param scope CoroutineScope for async work (structured concurrency).
     * @param progress Callback 0.0–1.0; may be called from any thread.
     * @return Success with output file, or Failure with typed error.
     */
    suspend fun convert(
        sourceUri: Uri,
        outputFile: java.io.File,
        cr: ContentResolver,
        scope: CoroutineScope,
        progress: (Float) -> Unit
    ): ConversionResult
}

/**
 * Registry that knows which engine handles a given source→target pair.
 * Built once at startup from CapabilityMatrix (§3).
 */
interface ConversionEngineRegistry {
    /** Returns engine for this pair, or null if unsupported. */
    fun getEngine(source: DocumentFormat, target: DocumentFormat): ConversionEngine?

    /** All engines registered (for tests / diagnostics). */
    val allEngines: List<ConversionEngine>
}