package com.docuconvert.app.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.docuconvert.app.DocuConvertApp
import com.docuconvert.app.conversion.ConversionOrchestrator
import com.docuconvert.app.data.AppSettings
import com.docuconvert.app.data.AppearanceMode
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory(this) }

    private lateinit var pickDocument: ActivityResultLauncher<Array<String>>
    private lateinit var pickDocumentForConvert: ActivityResultLauncher<Array<String>>
    private lateinit var pickFolder: ActivityResultLauncher<Uri?>
    private lateinit var createResultDocument: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(this)

        pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takeReadPermission(it)
                viewModel.openDocument(this, it) { viewModel.requestNavigate("viewer") }
            }
        }
        pickDocumentForConvert = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takeReadPermission(it)
                viewModel.openDocument(this, it) { viewModel.requestNavigate("convert") }
            }
        }
        pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                viewModel.setDefaultOutputFolder(it.toString())
            }
        }
        createResultDocument = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val destUri = result.data?.data
                val resultFile = viewModel.uiState.value.conversionResultFile
                if (destUri != null && resultFile != null) {
                    viewModel.exportResultToUri(this, destUri, resultFile)
                }
            }
        }

        // Open documents shared from other apps via ACTION_VIEW.
        handleViewIntent(intent)

        setContent {
            val settings: AppSettings by viewModel.settings.collectAsState(initial = AppSettings())
            val darkTheme: Boolean = when (settings.appearance) {
                AppearanceMode.DARK -> true
                AppearanceMode.LIGHT -> false
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }
            DocuConvertTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        viewModel = viewModel,
                        pickDocument = pickDocument,
                        pickDocumentForConvert = pickDocumentForConvert,
                        pickFolder = pickFolder,
                        saveResult = createResultDocument,
                        versionName = "1.0.0"
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    private fun takeReadPermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun handleViewIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_SEND) return
        takeReadPermission(uri)
        viewModel.openDocument(this, uri) { viewModel.requestNavigate("viewer") }
    }
}

/** Factory that wires the whole object graph (no DI lib needed for this scope). */
class AppViewModelFactory(private val activity: ComponentActivity) :
    androidx.lifecycle.ViewModelProvider.Factory {

    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val app = activity.applicationContext as DocuConvertApp
        @Suppress("UNCHECKED_CAST")
        return AppViewModel(
            orchestrator = ConversionOrchestrator(
                initPdfLoader = { PDFBoxResourceLoader.init(activity.applicationContext) }
            ),
            historyDao = app.conversionHistoryDao,
            settingsRepo = app.settingsRepository,
            tempManager = app.temporaryFileManager
        ) as T
    }
}
