package com.docuconvert.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docuconvert.app.R
import com.docuconvert.app.data.AppSettings
import com.docuconvert.app.data.AppearanceMode
import com.docuconvert.app.data.ConversionHistoryEntity
import com.docuconvert.app.domain.ContentBlock
import com.docuconvert.app.domain.ConversionSupport
import com.docuconvert.app.domain.DocumentFormat
import com.docuconvert.app.domain.DocumentInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Home ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppViewModel.UiState,
    onOpenDocument: () -> Unit,
    onConvertDocument: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRecentClick: (ConversionHistoryEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(DocuIcons.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DocuSpacing.md)
                .padding(top = DocuSpacing.lg, bottom = DocuSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero: brand mark + name + tagline.
            DocuConvertLogo(size = 64.dp)
            Spacer(Modifier.height(DocuSpacing.sm))
            Text(
                stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(DocuSpacing.xxs))
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(DocuSpacing.lg))

            // Primary / secondary actions with clear hierarchy.
            Button(
                onClick = onOpenDocument,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Open a document to view or convert" }
            ) {
                Icon(DocuIcons.OpenDocument, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(DocuSpacing.xs))
                Text(stringResource(R.string.btn_open_document))
            }
            Spacer(Modifier.height(DocuSpacing.xs))
            FilledTonalButton(
                onClick = onConvertDocument,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Start a new conversion" }
            ) {
                Icon(DocuIcons.Convert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(DocuSpacing.xs))
                Text(stringResource(R.string.btn_convert_document))
            }
            Spacer(Modifier.height(DocuSpacing.md))

            OfflineStrip()

            Spacer(Modifier.height(DocuSpacing.lg))
            DocuSectionHeader(
                title = stringResource(R.string.recent_documents),
                modifier = Modifier.fillMaxWidth(),
                action = {
                    if (state.recentDocuments.isNotEmpty()) {
                        TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.see_all)) }
                    }
                }
            )
            Spacer(Modifier.height(DocuSpacing.xs))
            if (state.recentDocuments.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DocuEmptyState(
                        icon = DocuIcons.Info,
                        iconRes = DocuDrawables.Doc,
                        headline = stringResource(R.string.no_recent_documents),
                        explanation = stringResource(R.string.recent_empty_hint)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(DocuSpacing.xs)) {
                    state.recentDocuments.take(5).forEach { entry ->
                        RecentItem(entry, onClick = { onRecentClick(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentItem(entry: ConversionHistoryEntity, onClick: () -> Unit) {
    val sourceFormat = runCatching { DocumentFormat.valueOf(entry.sourceFormat) }.getOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            Modifier.padding(horizontal = DocuSpacing.sm, vertical = DocuSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
        ) {
            if (sourceFormat != null) FormatGlyph(sourceFormat)
            else DocuDrawableGlyph(resId = DocuDrawables.Doc, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.sourceName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                DocuCaption(
                    "${entry.sourceFormat} → ${entry.targetFormat} • " +
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(entry.timestampMs))
                )
            }
            if (!entry.success) {
                Text(
                    stringResource(R.string.history_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// -------------------------------------------------------------- Viewer ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    document: DocumentInfo?,
    state: AppViewModel.UiState,
    formatSize: (Long) -> String,
    onBack: () -> Unit,
    onConvert: () -> Unit,
    onShare: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        document?.displayName ?: stringResource(R.string.viewer_default_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare, enabled = document != null) {
                        Icon(DocuIcons.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            if (document != null && !state.isLoadingDocument) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DocuSpacing.md, vertical = DocuSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(DocuSpacing.xs)
                    ) {
                        FilledTonalButton(
                            onClick = onShare,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(DocuIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(DocuSpacing.xs))
                            Text(stringResource(R.string.btn_share))
                        }
                        Button(
                            onClick = onConvert,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(DocuIcons.Convert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(DocuSpacing.xs))
                            Text(stringResource(R.string.btn_convert))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoadingDocument) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (document == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                DocuEmptyState(
                    icon = DocuIcons.Info,
                    iconRes = DocuDrawables.Doc,
                    headline = stringResource(R.string.viewer_no_document),
                    explanation = state.loadError ?: stringResource(R.string.viewer_no_document_hint)
                )
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = DocuSpacing.md)
        ) {
            Spacer(Modifier.height(DocuSpacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
            ) {
                FormatGlyph(document.format)
                Column(Modifier.weight(1f)) {
                    DocuFilename(document.displayName, style = MaterialTheme.typography.titleSmall)
                    DocuCaption("${document.format.displayName} • ${formatSize(document.sizeBytes)}")
                }
            }
            if (document.signatureMismatch) {
                Spacer(Modifier.height(DocuSpacing.xs))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        stringResource(R.string.viewer_sig_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(DocuSpacing.sm)
                    )
                }
            }
            state.loadError?.let {
                Spacer(Modifier.height(DocuSpacing.xs))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(DocuSpacing.sm))
            val content = state.documentContent
            if (content != null) {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(DocuSpacing.sm),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = DocuSpacing.md
                        )
                    ) {
                        content.title?.let { title ->
                            item {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        items(content.blocks.size) { idx ->
                            DocumentBlockView(content.blocks[idx])
                        }
                    }
                }
            } else {
                DocuEmptyState(
                    icon = DocuIcons.Info,
                    iconRes = DocuDrawables.Doc,
                    headline = stringResource(R.string.viewer_no_preview),
                    explanation = stringResource(R.string.viewer_no_preview_hint)
                )
            }
        }
    }
}

@Composable
private fun DocumentBlockView(block: ContentBlock) {
    when (block) {
        is ContentBlock.Heading -> Text(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.titleLarge
                2 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
        )
        is ContentBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
        is ContentBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(DocuSpacing.xs)) {
            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(block.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        is ContentBlock.Table -> Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(Modifier.padding(DocuSpacing.sm)) {
                block.rows.take(20).forEach { row ->
                    Text(row.joinToString("  |  "), style = MaterialTheme.typography.bodySmall)
                }
                if (block.rows.size > 20) {
                    Text(
                        "… ${block.rows.size - 20} more rows",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is ContentBlock.ImagePlaceholder -> Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                "[Image: ${block.altText}]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DocuSpacing.sm, vertical = DocuSpacing.xs)
            )
        }
        is ContentBlock.PageBreak -> androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// -------------------------------------------------------------- Convert ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    document: DocumentInfo?,
    state: AppViewModel.UiState,
    formatSize: (Long) -> String,
    onBack: () -> Unit,
    onSelectTarget: (DocumentFormat) -> Unit,
    onConvert: () -> Unit,
    onCancel: () -> Unit,
    onOpenResult: () -> Unit,
    onShareResult: () -> Unit,
    onSaveResult: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.convert_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (document == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                DocuEmptyState(
                    icon = DocuIcons.Convert,
                    iconRes = DocuDrawables.Doc,
                    headline = stringResource(R.string.convert_title),
                    explanation = stringResource(R.string.convert_subtitle)
                )
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = DocuSpacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(DocuSpacing.sm))
            FileSummaryCard(
                name = document.displayName,
                meta = "${document.format.displayName} • ${formatSize(document.sizeBytes)}",
                leading = { FormatGlyph(document.format) }
            )
            Spacer(Modifier.height(DocuSpacing.md))
            DocuSectionHeader(title = stringResource(R.string.convert_to))
            Spacer(Modifier.height(DocuSpacing.xs))
            // Targets come ONLY from CapabilityMatrix — never hardcoded.
            Column(verticalArrangement = Arrangement.spacedBy(DocuSpacing.xs)) {
                state.targets.forEach { cap ->
                    TargetRow(
                        target = cap.target,
                        note = if (cap.support == ConversionSupport.SIMPLIFIED) {
                            cap.note ?: stringResource(R.string.target_simplified)
                        } else {
                            stringResource(R.string.target_high_compat)
                        },
                        selected = state.selectedTarget == cap.target,
                        onClick = { onSelectTarget(cap.target) }
                    )
                }
            }
            Spacer(Modifier.height(DocuSpacing.md))
            val progress = state.conversionProgress
            if (progress != null) {
                ConversionProgressCard(
                    sourceName = document.displayName,
                    targetName = state.selectedTarget?.name.orEmpty(),
                    progress = progress,
                    onCancel = onCancel
                )
            } else {
                Button(
                    onClick = onConvert,
                    enabled = state.selectedTarget != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(stringResource(R.string.btn_convert_action))
                }
            }
            state.conversionError?.let { err ->
                Spacer(Modifier.height(DocuSpacing.sm))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(DocuSpacing.md)) {
                        Text(
                            stringResource(R.string.error_unable_to_convert),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(DocuSpacing.xxs))
                        Text(
                            err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(DocuSpacing.xs))
                        Text(
                            stringResource(R.string.error_possible_reasons),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "• ${stringResource(R.string.error_unsupported_structure)}\n" +
                                "• ${stringResource(R.string.error_corrupted_file)}\n" +
                                "• ${stringResource(R.string.error_unsupported_conversion)}\n" +
                                "• ${stringResource(R.string.error_insufficient_storage)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            state.conversionResultFile?.let { file ->
                Spacer(Modifier.height(DocuSpacing.sm))
                ConversionSuccessCard(
                    fileName = file.name,
                    onOpenResult = onOpenResult,
                    onShareResult = onShareResult,
                    onSaveResult = onSaveResult
                )
            }
            Spacer(Modifier.height(DocuSpacing.md))
        }
    }
}

@Composable
private fun TargetRow(
    target: DocumentFormat,
    note: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Convert to ${target.name}. $note" }
    ) {
        Row(
            Modifier.padding(horizontal = DocuSpacing.sm, vertical = DocuSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
        ) {
            FormatGlyph(target)
            Column(Modifier.weight(1f)) {
                Text(target.name, style = MaterialTheme.typography.bodyLarge)
                DocuCaption(note)
            }
            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@Composable
private fun ConversionProgressCard(
    sourceName: String,
    targetName: String,
    progress: Float,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(DocuSpacing.md)) {
            Text(stringResource(R.string.converting), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(DocuSpacing.xxs))
            DocuCaption("$sourceName → $targetName")
            Spacer(Modifier.height(DocuSpacing.sm))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DocuSpacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.btn_cancel)) }
            }
        }
    }
}

@Composable
private fun ConversionSuccessCard(
    fileName: String,
    onOpenResult: () -> Unit,
    onShareResult: () -> Unit,
    onSaveResult: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(DocuSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DocuSpacing.xs)
            ) {
                DocuGlyph(
                    icon = DocuIcons.Success,
                    contentDescription = null,
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.conversion_complete),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    DocuFilename(fileName, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(DocuSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(DocuSpacing.xs)) {
                Button(onClick = onOpenResult, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.btn_open_result))
                }
                FilledTonalButton(onClick = onSaveResult, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.btn_save_as))
                }
                FilledTonalButton(onClick = onShareResult, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.btn_share))
                }
            }
        }
    }
}

// -------------------------------------------------------------- History ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<ConversionHistoryEntity>,
    onBack: () -> Unit,
    onDelete: (ConversionHistoryEntity) -> Unit,
    onClear: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        OutlinedButton(onClick = onClear) { Text(stringResource(R.string.btn_clear)) }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                DocuEmptyState(
                    icon = DocuIcons.Info,
                    iconRes = DocuDrawables.History,
                    headline = stringResource(R.string.history_empty),
                    explanation = stringResource(R.string.history_empty_hint)
                )
            }
            return@Scaffold
        }
        // Group by day: Today / Yesterday / Earlier (labels resolved outside Lazy scope).
        val todayLabel = stringResource(R.string.history_today)
        val yesterdayLabel = stringResource(R.string.history_yesterday)
        val groups = entries.groupBy { dayLabel(it.timestampMs, todayLabel, yesterdayLabel) }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = DocuSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DocuSpacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = DocuSpacing.sm)
        ) {
            groups.forEach { (day, dayEntries) ->
                item(key = "header-$day") {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = DocuSpacing.sm,
                            bottom = DocuSpacing.xxs
                        )
                    )
                }
                items(dayEntries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, onDelete = onDelete)
                }
            }
        }
    }
}

private fun dayLabel(timestampMs: Long, todayLabel: String, yesterdayLabel: String): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    return when {
        cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> todayLabel
        cal.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> yesterdayLabel
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestampMs))
    }
}

@Composable
private fun HistoryRow(
    entry: ConversionHistoryEntity,
    onDelete: (ConversionHistoryEntity) -> Unit
) {
    val sourceFormat = runCatching { DocumentFormat.valueOf(entry.sourceFormat) }.getOrNull()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = DocuSpacing.sm, vertical = DocuSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
        ) {
            if (sourceFormat != null) FormatGlyph(sourceFormat)
            else DocuDrawableGlyph(resId = DocuDrawables.Doc, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.sourceFormat} → ${entry.targetFormat}" +
                        if (!entry.success) " • Failed" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!entry.success) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    entry.sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestampMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onDelete(entry) }) {
                Icon(DocuIcons.Error, contentDescription = "Delete ${entry.sourceName}")
            }
        }
    }
}

// -------------------------------------------------------------- Settings ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    versionName: String,
    onBack: () -> Unit,
    onAppearance: (AppearanceMode) -> Unit,
    onKeepHistory: (Boolean) -> Unit,
    onAutoOpen: (Boolean) -> Unit,
    onPickOutputFolder: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLicenses: () -> Unit,
    onSupport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = DocuSpacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(DocuSpacing.xs))
            SettingsGroupLabel(stringResource(R.string.settings_general))
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.appearance),
                    subtitle = when (settings.appearance) {
                        AppearanceMode.SYSTEM -> stringResource(R.string.appearance_system)
                        AppearanceMode.LIGHT -> stringResource(R.string.appearance_light)
                        AppearanceMode.DARK -> stringResource(R.string.appearance_dark)
                    },
                    onClick = null
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                // Compact segmented control: System | Light | Dark.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DocuSpacing.md, vertical = DocuSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(DocuSpacing.xs)
                ) {
                    AppearanceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.appearance == mode,
                            onClick = { onAppearance(mode) },
                            label = {
                                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsRow(
                    title = stringResource(R.string.default_output_folder),
                    subtitle = settings.defaultOutputFolder ?: stringResource(R.string.settings_folder_default),
                    onClick = onPickOutputFolder,
                    trailingRes = DocuDrawables.Folder,
                    contentDescription = "Default output folder. Choose folder."
                )
            }

            Spacer(Modifier.height(DocuSpacing.md))
            SettingsGroupLabel(stringResource(R.string.settings_behavior))
            SettingsCard {
                SettingsToggleRow(
                    title = stringResource(R.string.keep_history),
                    checked = settings.keepHistory,
                    onChecked = onKeepHistory
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsToggleRow(
                    title = stringResource(R.string.auto_open_result),
                    checked = settings.autoOpenResult,
                    onChecked = onAutoOpen
                )
            }

            Spacer(Modifier.height(DocuSpacing.md))
            SettingsGroupLabel(stringResource(R.string.about))
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = null,
                    onClick = onPrivacyPolicy,
                    trailingRes = DocuDrawables.Chevron,
                    contentDescription = "Privacy Policy. Opens in browser."
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsRow(
                    title = stringResource(R.string.open_source_licenses),
                    subtitle = null,
                    onClick = onLicenses,
                    trailingRes = DocuDrawables.Chevron,
                    contentDescription = "Open Source Licenses. Opens list."
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsRow(
                    title = stringResource(R.string.support_title),
                    subtitle = stringResource(R.string.support_subtitle),
                    onClick = onSupport,
                    trailingRes = DocuDrawables.Heart,
                    contentDescription = "Support Dovra. Opens support options."
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsRow(
                    title = "${stringResource(R.string.version)} $versionName",
                    subtitle = null,
                    onClick = null
                )
            }
            Spacer(Modifier.height(DocuSpacing.md))
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = DocuSpacing.sm,
            bottom = DocuSpacing.xs
        )
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    /** Trailing affordance: chevron for navigation, folder for picker. */
    trailingRes: Int = DocuDrawables.Chevron,
    contentDescription: String? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription ?: title
            }
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        rowModifier.padding(horizontal = DocuSpacing.md, vertical = DocuSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                DocuCaption(subtitle)
            }
        }
        if (onClick != null) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(trailingRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DocuSpacing.md, vertical = DocuSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

/** Support dialog: two fixed external donation links, opened in the browser. */
@Composable
fun SupportDialog(
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = androidx.compose.ui.res.painterResource(DocuDrawables.Heart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        title = { Text(stringResource(R.string.support_title)) },
        text = { Text(stringResource(R.string.support_body)) },
        confirmButton = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DocuSpacing.xs)
            ) {
                Button(
                    onClick = { onOpenLink(SupportLinks.BUY_ME_A_COFFEE) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.support_bmc))
                }
                FilledTonalButton(
                    onClick = { onOpenLink(SupportLinks.SAWERIA) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.support_saweria))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}
