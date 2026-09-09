package com.docuconvert.app.presentation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuconvert.app.conversion.ConversionOrchestrator
import com.docuconvert.app.conversion.FormatDetector
import com.docuconvert.app.data.ConversionHistoryDao
import com.docuconvert.app.data.ConversionHistoryEntity
import com.docuconvert.app.data.SettingsRepository
import com.docuconvert.app.domain.CapabilityMatrix
import com.docuconvert.app.domain.ConversionCapability
import com.docuconvert.app.domain.ConversionError
import com.docuconvert.app.domain.ConversionResult
import com.docuconvert.app.domain.DocumentContent
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.DocumentInfo
import com.docuconvert.app.domain.Result
import com.docuconvert.app.storage.TemporaryFileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Single screen-state holder for the whole app (MVVM).
 * Navigation state + document state + conversion state in one place.
 */
class AppViewModel(
    private val orchestrator: ConversionOrchestrator,
    private val historyDao: ConversionHistoryDao,
    private val settingsRepo: SettingsRepository,
    private val tempManager: TemporaryFileManager
) : ViewModel() {

    data class UiState(
        val document: DocumentInfo? = null,
        val documentContent: DocumentContent? = null,
        val isLoadingDocument: Boolean = false,
        val loadError: String? = null,
        val targets: List<ConversionCapability> = emptyList(),
        val selectedTarget: DocumentFormat? = null,
        val conversionProgress: Float? = null,
        val conversionResultFile: File? = null,
        val conversionResultTarget: DocumentFormat? = null,
        val conversionError: String? = null,
        val recentDocuments: List<ConversionHistoryEntity> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val settings = settingsRepo.settings

    private var conversionJob: Job? = null

    init {
        viewModelScope.launch {
            historyDao.observeAll().collect { history ->
                _uiState.update { it.copy(recentDocuments = history.take(10)) }
            }
        }
    }

    // ---- settings passthrough ----

    fun setAppearance(mode: com.docuconvert.app.data.AppearanceMode) {
        viewModelScope.launch { settingsRepo.setAppearance(mode) }
    }

    fun setKeepHistory(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setKeepHistory(enabled) }
    }

    fun setAutoOpenResult(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoOpenResult(enabled) }
    }

    fun setDefaultOutputFolder(uri: String?) {
        viewModelScope.launch { settingsRepo.setDefaultOutputFolder(uri) }
    }

    /** Called after SAF picker returns a Uri. Detects format, loads preview content. */
    fun openDocument(context: Context, uri: Uri, onOpened: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDocument = true, loadError = null,
                    document = null, documentContent = null,
                    conversionProgress = null, conversionResultFile = null,
                    conversionError = null, selectedTarget = null
                )
            }
            val detection = FormatDetector.detect(context, uri)
            val format = detection.format
            if (format == null) {
                _uiState.update {
                    it.copy(
                        isLoadingDocument = false,
                        loadError = "Unsupported file type: ${detection.displayName}"
                    )
                }
                return@launch
            }
            val info = DocumentInfo(
                uri = uri,
                displayName = detection.displayName,
                format = format,
                mimeType = detection.mimeType,
                sizeBytes = detection.sizeBytes,
                signatureMismatch = detection.signatureMismatch
            )
            val reader = orchestrator.getReader(format)
            val content: DocumentContent? = if (reader != null) {
                when (val r = reader.read(uri, context.contentResolver, this)) {
                    is Result.Success -> r.value
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoadingDocument = false,
                                document = info,
                                loadError = "Could not open this file. It may be corrupted."
                            )
                        }
                        return@launch
                    }
                }
            } else null

            _uiState.update {
                it.copy(
                    isLoadingDocument = false,
                    document = info,
                    documentContent = content,
                    targets = CapabilityMatrix.supportedTargets(format)
                )
            }
            onOpened?.invoke()
        }
    }

    fun selectTarget(target: DocumentFormat) {
        _uiState.update { it.copy(selectedTarget = target, conversionError = null) }
    }

    /** Starts conversion of the loaded document to [target] (or selected target). */
    fun startConversion(context: Context, target: DocumentFormat? = null) {
        val state = _uiState.value
        val doc = state.document ?: return
        val dest = target ?: state.selectedTarget ?: return
        if (!CapabilityMatrix.isSupported(doc.format, dest)) {
            _uiState.update { it.copy(conversionError = "This conversion is not supported.") }
            return
        }
        val engine = orchestrator.getEngine(doc.format, dest) ?: run {
            _uiState.update { it.copy(conversionError = "This conversion is not supported.") }
            return
        }
        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            _uiState.update { it.copy(conversionProgress = 0f, conversionError = null, conversionResultFile = null) }
            val cr: ContentResolver = context.contentResolver
            val tmp = runCatching {
                tempManager.createTempFileForFormat("conv_", dest)
            }.getOrElse {
                _uiState.update { it.copy(conversionProgress = null, conversionError = "Not enough storage space.") }
                return@launch
            }
            val outputName = uniqueOutputName(doc.displayName, dest)
            val finalFile = File(tmp.parentFile, outputName)
            val result = engine.convert(doc.uri, tmp, cr, this) { p ->
                _uiState.update { it.copy(conversionProgress = p) }
            }
            when (result) {
                is ConversionResult.Success -> {
                    runCatching { tmp.renameTo(finalFile) }
                    val out = if (finalFile.exists()) finalFile else tmp
                    _uiState.update {
                        it.copy(
                            conversionProgress = null,
                            conversionResultFile = out,
                            conversionResultTarget = dest
                        )
                    }
                    viewModelScope.launch {
                        runCatching {
                            historyDao.insert(
                                ConversionHistoryEntity(
                                    sourceName = doc.displayName,
                                    sourceFormat = doc.format.name,
                                    targetFormat = dest.name,
                                    resultName = out.name,
                                    sizeBytes = out.length(),
                                    timestampMs = System.currentTimeMillis(),
                                    success = true
                                )
                            )
                        }
                    }
                }
                is ConversionResult.Failure -> {
                    tempManager.cleanup(tmp)
                    _uiState.update {
                        it.copy(
                            conversionProgress = null,
                            conversionError = userMessage(result.error)
                        )
                    }
                    viewModelScope.launch {
                        runCatching {
                            historyDao.insert(
                                ConversionHistoryEntity(
                                    sourceName = doc.displayName,
                                    sourceFormat = doc.format.name,
                                    targetFormat = dest.name,
                                    resultName = null,
                                    sizeBytes = 0,
                                    timestampMs = System.currentTimeMillis(),
                                    success = false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        conversionJob = null
        _uiState.update { it.copy(conversionProgress = null) }
    }

    /** One-shot navigation requests consumed by AppNavHost. */
    private val _navEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navEvents: kotlinx.coroutines.flow.SharedFlow<String> = _navEvents

    fun requestNavigate(route: String) {
        _navEvents.tryEmit(route)
    }

    /** Copies the finished conversion result [source] into the user-chosen SAF [destUri]. */
    fun exportResultToUri(context: Context, destUri: Uri, source: File) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                } ?: throw java.io.IOException("Cannot open destination")
            }.onFailure {
                _uiState.update { s ->
                    s.copy(conversionError = "Could not save the file to the chosen location.")
                }
            }
        }
    }

    fun deleteHistory(entry: ConversionHistoryEntity) {
        viewModelScope.launch { runCatching { historyDao.delete(entry) } }
    }

    fun clearHistory() {
        viewModelScope.launch { runCatching { historyDao.clearAll() } }
    }

    private fun uniqueOutputName(sourceName: String, target: DocumentFormat): String {
        val base = sourceName.substringBeforeLast('.', sourceName)
            .takeIf { it.isNotBlank() } ?: "converted"
        // Sanitize: no path separators, control chars, or traversal.
        val safe = base.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().take(100)
            .ifBlank { "converted" }
        return "$safe.${target.extension}"
    }

    private fun userMessage(error: ConversionError): String = when (error) {
        is ConversionError.UnsupportedConversion -> "This conversion is not supported."
        is ConversionError.CorruptedFile -> "The file appears to be corrupted or invalid."
        is ConversionError.InsufficientStorage -> "Not enough storage space to complete the conversion."
        is ConversionError.PermissionDenied -> "Permission was denied. Please try selecting the file again."
        is ConversionError.Timeout -> "Conversion took too long and was stopped."
        is ConversionError.Cancelled -> "Conversion was cancelled."
        is ConversionError.EngineFailure -> "Unable to convert this document."
    }
}
