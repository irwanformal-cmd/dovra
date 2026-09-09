package com.docuconvert.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Open-source attributions (§22). Keep in sync with app/build.gradle.kts.
 * GPL/AGPL libraries are deliberately absent — nothing here is copyleft.
 */
data class OssEntry(
    val name: String,
    val version: String,
    val license: String,
    val url: String
)

val OSS_ENTRIES: List<OssEntry> = listOf(
    OssEntry("AndroidX Core KTX", "1.16.0", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("AndroidX Lifecycle", "2.8.7", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("AndroidX Activity Compose", "1.10.1", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("Jetpack Compose + Material 3 (BOM 2024.12.01)", "2024.12.01", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("Navigation Compose", "2.8.7", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("Room", "2.8.4", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("DataStore Preferences", "1.1.7", "Apache-2.0", "https://github.com/androidx/androidx"),
    OssEntry("Kotlinx Coroutines", "1.10.1", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    OssEntry("Kotlinx Serialization JSON", "1.7.3", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    OssEntry("Apache POI (poi, poi-ooxml, poi-scratchpad)", "5.5.1", "Apache-2.0", "https://poi.apache.org/"),
    OssEntry("Apache Commons CSV", "1.14.1", "Apache-2.0", "https://commons.apache.org/proper/commons-csv/"),
    OssEntry("ODFDOM Java (ODF Toolkit)", "0.9.0", "Apache-2.0", "https://odftoolkit.org/"),
    OssEntry("PDFBox-Android (TomRoush fork)", "2.0.27.0", "Apache-2.0", "https://github.com/TomRoush/PdfBox-Android"),
    OssEntry("rtfparserkit", "1.16.0", "Apache-2.0", "https://github.com/joniles/rtfparserkit"),
    OssEntry("epublib-core (positiondev fork)", "3.1", "LGPL-2.1 (library use, no modification)", "https://github.com/positiondev/epublib"),
    OssEntry("CommonMark Java (+ GFM tables)", "0.30.0", "BSD-2-Clause", "https://github.com/commonmark/commonmark-java"),
    OssEntry("Jsoup", "1.23.2", "MIT", "https://jsoup.org/"),
    OssEntry("Coil Compose", "2.7.0", "Apache-2.0", "https://github.com/coil-kt/coil"),
    OssEntry("JUnit4 / Espresso / core-testing", "—", "Apache-2.0 / EPL (test only, not shipped)", "https://developer.android.com/testing")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = DocuSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DocuSpacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = DocuSpacing.sm)
        ) {
            items(OSS_ENTRIES) { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(DocuSpacing.sm)
                    ) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Version ${entry.version} • ${entry.license}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            entry.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
