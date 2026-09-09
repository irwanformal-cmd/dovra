package com.docuconvert.app.storage

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages temporary files for conversions. All temp files live in
 * [baseDir] (app-private cache) so they are auto-cleared on app uninstall
 * and never pollute user-visible storage.
 *
 * Cleanup strategy (§15):
 *  - deleteOnExit for each created file
 *  - bulk sweep on app start (orphaned files from crashed runs)
 *  - bulk sweep on TRIM_MEMORY_UI_HIDDEN
 *  - caller may call [cleanup(file)] explicitly on success/failure
 */
class TemporaryFileManager(private val baseDir: File) {

    companion object {
        private const val TAG = "TempFileManager"
    }

    init {
        baseDir.mkdirs()
    }

    /** Creates a uniquely-named empty file in the temp dir. */
    @Throws(IOException::class)
    fun createTempFile(prefix: String = "docuconv_", suffix: String = ".tmp"): File {
        baseDir.mkdirs()
        return File.createTempFile(prefix, suffix, baseDir)
    }

    /** Creates a temp file with an extension that hints at the target format. */
    @Throws(IOException::class)
    fun createTempFileForFormat(prefix: String, format: com.docuconvert.app.domain.DocumentFormat): File =
        createTempFile(prefix, ".${format.extension}")

    /** Immediately deletes [file] if it exists and is inside our temp dir. */
    fun cleanup(file: File?) {
        file?.let {
            if (isInsideTempDir(it)) {
                try {
                    if (it.delete()) Log.d(TAG, "Deleted temp file: ${it.name}")
                    else Log.w(TAG, "Failed to delete temp file: ${it.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting temp file ${it.name}", e)
                }
            } else {
                Log.w(TAG, "Refused to delete file outside temp dir: $it")
            }
        }
    }

    /** Deletes all files in temp dir older than [maxAgeMs] (default 24 h). */
    fun cleanupOrphanedTempFiles(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        val deleted = AtomicBoolean(false)
        baseDir.listFiles()?.forEach { f ->
            if (now - f.lastModified() > maxAgeMs) {
                if (f.delete()) deleted.set(true)
            }
        }
        if (deleted.get()) Log.d(TAG, "Cleaned up orphaned temp files")
    }

    private fun isInsideTempDir(file: File): Boolean {
        try {
            val canonBase = baseDir.canonicalPath
            val canonFile = file.canonicalPath
            return canonFile.startsWith(canonBase + File.separator)
        } catch (e: IOException) {
            return false
        }
    }
}

/** Context extension for easy access. */
fun Context.getTemporaryFileManager(): TemporaryFileManager =
    (applicationContext as com.docuconvert.app.DocuConvertApp).temporaryFileManager