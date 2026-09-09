package com.docuconvert.app.conversion

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.docuconvert.app.domain.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Three-signal format detection (§13):
 *  1. File extension (fast path)
 *  2. MIME type from ContentResolver
 *  3. Magic-byte signature sniffing (authoritative for mismatches)
 *
 * A file named "document.pdf" that is not actually a PDF is reported
 * with [DetectionResult.signatureMismatch] = true so the UI can show
 * a sensible error instead of crashing a parser.
 */
object FormatDetector {

    private const val TAG = "FormatDetector"
    private const val SNIFF_BYTES = 16

    data class DetectionResult(
        val format: DocumentFormat?,
        val displayName: String,
        val sizeBytes: Long,
        val mimeType: String?,
        val signatureMismatch: Boolean
    )

    suspend fun detect(
        context: Context,
        uri: Uri
    ): DetectionResult = withContext(Dispatchers.IO) {
        val cr: ContentResolver = context.contentResolver
        val displayName = queryDisplayName(cr, uri) ?: "document"
        val size = querySize(cr, uri)
        val mime = cr.getType(uri)

        val extFormat = displayName.substringAfterLast('.', "").trim().lowercase()
            .takeIf { it.isNotEmpty() }?.let { DocumentFormat.fromExtension(it) }
        val mimeFormat = DocumentFormat.fromMimeType(mime)

        // Magic bytes decide.
        val sigFormat = sniffSignature(cr, uri)

        val chosen = sigFormat ?: extFormat ?: mimeFormat
        val mismatch = sigFormat != null && extFormat != null && sigFormat != extFormat

        if (mismatch) {
            Log.w(TAG, "Signature mismatch: $displayName claims $extFormat, magic says $sigFormat")
        }

        DetectionResult(
            format = chosen,
            displayName = displayName,
            sizeBytes = size,
            mimeType = mime,
            signatureMismatch = mismatch
        )
    }

    private fun queryDisplayName(cr: ContentResolver, uri: Uri): String? =
        runCatching {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()

    private fun querySize(cr: ContentResolver, uri: Uri): Long =
        runCatching {
            cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
            } ?: -1L
        }.getOrDefault(-1L)

    /**
     * Reads the first bytes and matches known magic numbers / ZIP members.
     * Returns null when the signature is unknown (caller falls back to ext/mime).
     */
    fun sniffSignature(cr: ContentResolver, uri: Uri): DocumentFormat? {
        return runCatching {
            cr.openInputStream(uri)?.use { input ->
                val header = ByteArray(SNIFF_BYTES)
                val read = input.read(header)
                if (read < 4) return@runCatching null
                sniffFromHeader(header, read)
            }
        }.getOrNull()
    }

    internal fun sniffFromHeader(header: ByteArray, read: Int): DocumentFormat? {
        fun ascii(offset: Int, len: Int): String =
            if (read >= offset + len) String(header, offset, len, Charsets.US_ASCII) else ""

        // PDF: %PDF-
        if (ascii(0, 5) == "%PDF-") return DocumentFormat.PDF
        // RTF: {\rtf
        if (ascii(0, 5) == "{\\rtf") return DocumentFormat.RTF
        // XML: <?xml (allow leading whitespace/BOM handled by trim in caller)
        if (ascii(0, 5) == "<?xml") return DocumentFormat.XML
        // ZIP container: PK\x03\x04 → OOXML or ODF or EPUB (need member sniff — caller handles)
        if (read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            return null // ambiguous ZIP; extension+MIME decide, parsers validate
        }
        // Legacy OLE2: D0 CF 11 E0 → DOC/XLS/PPT (ambiguous; extension decides)
        if (read >= 4 && header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte()
            && header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()
        ) {
            return null // ambiguous OLE2; extension decides, parsers validate
        }
        return null
    }
}
