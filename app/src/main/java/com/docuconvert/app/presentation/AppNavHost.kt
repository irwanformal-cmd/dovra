package com.docuconvert.app.presentation

import android.app.Activity
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docuconvert.app.R

@Composable
fun AppNavHost(
    viewModel: AppViewModel,
    pickDocument: ActivityResultLauncher<Array<String>>,
    pickDocumentForConvert: ActivityResultLauncher<Array<String>>,
    pickFolder: ActivityResultLauncher<Uri?>,
    saveResult: ActivityResultLauncher<android.content.Intent>,
    versionName: String
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = com.docuconvert.app.data.AppSettings())
    val context = LocalContext.current
    val activity = context as? Activity

    androidx.compose.runtime.LaunchedEffect(navController) {
        viewModel.navEvents.collect { route ->
            runCatching {
                navController.navigate(route) {
                    if (route == "home") popUpTo("home") { inclusive = false }
                    else popUpTo("home")
                    launchSingleTop = true
                }
            }
        }
    }

    val documentsMime = arrayOf(
        "application/pdf", "text/plain", "text/markdown", "text/html", "text/xml",
        "application/rtf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.oasis.opendocument.text", "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.oasis.opendocument.spreadsheet", "text/csv",
        "text/tab-separated-values", "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.oasis.opendocument.presentation", "application/epub+zip"
    )

    Scaffold(
        bottomBar = {
            DocuConvertBottomBar(navController)
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        state = state,
                        onOpenDocument = { pickDocument.launch(documentsMime) },
                        onConvertDocument = { pickDocumentForConvert.launch(documentsMime) },
                        onOpenHistory = { navController.navigate("history") },
                        onOpenSettings = { navController.navigate("settings") },
                        onRecentClick = { /* history entries have no persisted Uri; show info */ }
                    )
                }
                composable("viewer") {
                    ViewerScreen(
                        document = state.document,
                        state = state,
                        formatSize = { formatFileSize(it) },
                        onBack = { navController.popBackStack() },
                        onConvert = { navController.navigate("convert") },
                        onShare = { state.document?.let { shareDocument(context, it.uri, it.mimeType) } }
                    )
                }
                composable("convert") {
                    ConvertScreen(
                        document = state.document,
                        state = state,
                        formatSize = { formatFileSize(it) },
                        onBack = { navController.popBackStack() },
                        onSelectTarget = { viewModel.selectTarget(it) },
                        onConvert = { viewModel.startConversion(context) },
                        onCancel = { viewModel.cancelConversion() },
                        onOpenResult = {
                            state.conversionResultFile?.let { openResultFile(context, it) }
                        },
                        onShareResult = {
                            state.conversionResultFile?.let {
                                shareResultFile(context, it, state.conversionResultTarget?.mimeType)
                            }
                        },
                        onSaveResult = {
                            val file = state.conversionResultFile
                            if (file != null) {
                                saveResult.launch(createDocumentIntent(file, state.conversionResultTarget?.mimeType))
                            }
                        }
                    )
                }
                composable("history") {
                    HistoryScreen(
                        entries = state.recentDocuments,
                        onBack = { navController.popBackStack() },
                        onDelete = { viewModel.deleteHistory(it) },
                        onClear = { viewModel.clearHistory() }
                    )
                }
                composable("settings") {
                    var showSupport by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    SettingsScreen(
                        settings = settings,
                        versionName = versionName,
                        onBack = { navController.popBackStack() },
                        onAppearance = { mode ->
                            viewModel.setAppearance(mode)
                            val nightMode = when (mode) {
                                com.docuconvert.app.data.AppearanceMode.SYSTEM ->
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                com.docuconvert.app.data.AppearanceMode.LIGHT ->
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                com.docuconvert.app.data.AppearanceMode.DARK ->
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            }
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
                            activity?.recreate()
                        },
                        onKeepHistory = { viewModel.setKeepHistory(it) },
                        onAutoOpen = { viewModel.setAutoOpenResult(it) },
                        onPickOutputFolder = { pickFolder.launch(null) },
                        onPrivacyPolicy = { openUrl(context, "https://documint.app/privacy") },
                        onLicenses = { navController.navigate("licenses") },
                        onSupport = { showSupport = true }
                    )
                    if (showSupport) {
                        SupportDialog(
                            onDismiss = { showSupport = false },
                            onOpenLink = { url -> openUrl(context, url) }
                        )
                    }
                }
                composable("licenses") {
                    LicensesScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun DocuConvertBottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    // Compact single-line labels — never two rows.
    NavigationBar(tonalElevation = 2.dp) {
        bottomItem(
            selected = route == "home",
            onClick = { navController.navigate("home") { popUpTo("home") } },
            icon = { Icon(DocuIcons.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) }
        )
        bottomItem(
            selected = route == "convert",
            onClick = { navController.navigate("convert") { popUpTo("home") } },
            icon = { Icon(DocuIcons.Convert, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_convert)) }
        )
        bottomItem(
            selected = route == "history",
            onClick = { navController.navigate("history") { popUpTo("home") } },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(DocuDrawables.History),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.nav_history)) }
        )
        bottomItem(
            selected = route == "settings",
            onClick = { navController.navigate("settings") { popUpTo("home") } },
            icon = { Icon(DocuIcons.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.bottomItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = icon, label = label)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
    return String.format(java.util.Locale.getDefault(), "%.1f MB", kb / 1024.0)
}

private fun shareDocument(context: android.content.Context, uri: Uri, mime: String?) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime ?: "*/*"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri(null, uri)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "Share document"))
    }
}

private fun openResultFile(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri(null, uri)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "Open with"))
    }
}

private fun shareResultFile(context: android.content.Context, file: java.io.File, mime: String?) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime ?: "*/*"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri(null, uri)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "Share result"))
    }
}

/**
 * Builds an ACTION_CREATE_DOCUMENT intent so the user picks exactly where
 * the conversion result is saved (scoped-storage compliant, no permissions).
 */
private fun createDocumentIntent(file: java.io.File, mime: String?): android.content.Intent {
    return android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(android.content.Intent.CATEGORY_OPENABLE)
        type = mime ?: "*/*"
        putExtra(android.content.Intent.EXTRA_TITLE, file.name)
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                url.toUri()
            )
        )
    }
}
