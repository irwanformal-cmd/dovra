package com.docuconvert.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Appearance modes for settings screen (§17). */
enum class AppearanceMode { SYSTEM, LIGHT, DARK }

/** Immutable snapshot of user settings. */
data class AppSettings(
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val defaultOutputFolder: String? = null,
    val keepHistory: Boolean = true,
    val autoOpenResult: Boolean = false
)

/**
 * DataStore-backed settings repository. No document content ever
 * passes through here — only UI preferences.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_APPEARANCE = stringPreferencesKey("appearance")
        private val KEY_OUTPUT_FOLDER = stringPreferencesKey("default_output_folder")
        private val KEY_KEEP_HISTORY = booleanPreferencesKey("keep_history")
        private val KEY_AUTO_OPEN = booleanPreferencesKey("auto_open_result")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            appearance = runCatching {
                AppearanceMode.valueOf(prefs[KEY_APPEARANCE] ?: AppearanceMode.SYSTEM.name)
            }.getOrDefault(AppearanceMode.SYSTEM),
            defaultOutputFolder = prefs[KEY_OUTPUT_FOLDER],
            keepHistory = prefs[KEY_KEEP_HISTORY] ?: true,
            autoOpenResult = prefs[KEY_AUTO_OPEN] ?: false
        )
    }

    suspend fun setAppearance(mode: AppearanceMode) {
        dataStore.edit { it[KEY_APPEARANCE] = mode.name }
    }

    suspend fun setDefaultOutputFolder(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(KEY_OUTPUT_FOLDER) else it[KEY_OUTPUT_FOLDER] = uri
        }
    }

    suspend fun setKeepHistory(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_HISTORY] = enabled }
    }

    suspend fun setAutoOpenResult(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_OPEN] = enabled }
    }
}
