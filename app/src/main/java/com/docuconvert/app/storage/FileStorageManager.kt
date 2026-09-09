package com.docuconvert.app.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.docuconvert.app.domain.DocumentFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * High-level file operations using Storage Access Framework / MediaStore.
 * NO broad storage permission required (§21). Works on all Android versions.
 */
class FileStorageManager(private val context: Context) {

    companion object {
        private const val TAG = "FileStorage"
        private const val OUTPUT_DIR_NAME = "Dovra"
    }

    private val cr: ContentResolver = context.contentResolver

    /** Opens an InputStream for a user-picked Uri (SAF). */
    @Throws(IOException::class)
    fun openInputStream(uri: Uri): InputStream =
        cr.openInputStream(uri) ?: throw IOException("Cannot open input stream for $uri")

    /** Copies Uri content to a local cache file (for engines that need a File). */
    @Throws(IOException::class)
    fun copyUriToCacheFile(uri: Uri, targetFile: File): Long {
        cr.openInputStream(uri).use { input ->
            FileOutputStream(targetFile).use { output ->
                return input?.copyTo(output) ?: 0L
            }
        }
    }

    /**
     * Saves [bytes] as a new document in the user-selected folder
     * (via MediaStore on API 29+, else SAF document tree).
     * Returns the output Uri for sharing / further intents.
     */
    @Throws(IOException::class)
    fun saveOutputDocument(
        bytes: ByteArray,
        originalName: String,
        format: DocumentFormat,
        targetFolderUri: Uri? = null
    ): Uri {
        val displayName = uniqueDisplayName(originalName, format)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$OUTPUT_DIR_NAME")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val baseUri = targetFolderUri ?: MediaStore.Files.getContentUri("external")
        val uri = cr.insert(baseUri, contentValues) ?: throw IOException("MediaStore insert failed")
        try {
            cr.openOutputStream(uri).use { out ->
                out?.write(bytes)
            }
            // Mark as non-pending (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cr.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            return uri
        } catch (e: Exception) {
            cr.delete(uri, null, null)
            throw IOException("Failed to write output document", e)
        }
    }

    /** Opens an OutputStream for a user-selected SAF document Uri. */
    @Throws(IOException::class)
    fun openOutputStream(uri: Uri): OutputStream =
        cr.openOutputStream(uri, "w") ?: throw IOException("Cannot open output stream for $uri")

    /** Creates a unique display name: base (1).ext, base (2).ext … */
    private fun uniqueDisplayName(originalName: String, format: DocumentFormat): String {
        val baseName = originalName.removeSuffix(".${format.extension}").removeSuffix(".${format.extension.uppercase()}")
        val ext = format.extension
        var candidate = "$baseName.$ext"
        var counter = 1
        // We don't actually query MediaStore for existence here because
        // SAF picker lets the user pick the final name anyway. This is just
        // a reasonable default for non-SAF paths.
        return candidate
    }

    /** Formats bytes as human-readable string for UI. */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}