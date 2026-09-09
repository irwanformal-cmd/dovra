package com.docuconvert.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.datastore.preferences.preferencesDataStore
import com.docuconvert.app.data.ConversionHistoryDao
import com.docuconvert.app.data.DocuConvertDatabase
import com.docuconvert.app.data.SettingsRepository
import com.docuconvert.app.storage.TemporaryFileManager
import java.io.File

private val Context.settingsDataStore by preferencesDataStore("settings.preferences_pb")

class DocuConvertApp : Application() {

    companion object {
        private const val DATABASE_NAME = "docuconvert.db"
    }

    private var _database: DocuConvertDatabase? = null
    val database: DocuConvertDatabase
        get() = _database ?: synchronized(this) {
            _database ?: Room.databaseBuilder(
                applicationContext,
                DocuConvertDatabase::class.java,
                DATABASE_NAME
            ).build().also { _database = it }
        }

    private var _conversionHistoryDao: ConversionHistoryDao? = null
    val conversionHistoryDao: ConversionHistoryDao
        get() = _conversionHistoryDao ?: database.conversionHistoryDao().also { _conversionHistoryDao = it }

    private var _settingsRepository: SettingsRepository? = null
    val settingsRepository: SettingsRepository
        get() = _settingsRepository ?: SettingsRepository(
            applicationContext.settingsDataStore
        ).also { _settingsRepository = it }

    private var _temporaryFileManager: TemporaryFileManager? = null
    val temporaryFileManager: TemporaryFileManager
        get() = _temporaryFileManager ?: TemporaryFileManager(
            File(cacheDir, "temp")
        ).also { _temporaryFileManager = it }

    override fun onCreate() {
        super.onCreate()
        database // triggers lazy init
        temporaryFileManager.cleanupOrphanedTempFiles()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            temporaryFileManager.cleanupOrphanedTempFiles()
        }
    }
}