package com.docuconvert.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docuconvert.app.domain.DocumentFormat

/** Consistent spacing scale: 4 / 8 / 12 / 16 / 24 / 32. */
object DocuSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Small tinted glyph chip used for file-type / status affordances. */
@Composable
fun DocuGlyph(
    icon: ImageVector,
    contentDescription: String?,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** File-type icon for a format (custom doc drawable; sheet/slides stay core). */
@Composable
fun FormatGlyph(format: DocumentFormat, modifier: Modifier = Modifier) {
    when (format) {
        DocumentFormat.PDF, DocumentFormat.EPUB,
        DocumentFormat.DOC, DocumentFormat.DOCX, DocumentFormat.ODT,
        DocumentFormat.TXT, DocumentFormat.MD, DocumentFormat.RTF,
        DocumentFormat.HTML, DocumentFormat.XML -> DocuDrawableGlyph(
            resId = DocuDrawables.Doc,
            contentDescription = format.displayName,
            modifier = modifier
        )
        DocumentFormat.XLS, DocumentFormat.XLSX, DocumentFormat.ODS,
        DocumentFormat.CSV, DocumentFormat.TSV -> DocuGlyph(
            icon = DocuIcons.Sheet,
            contentDescription = format.displayName,
            modifier = modifier
        )
        DocumentFormat.PPT, DocumentFormat.PPTX, DocumentFormat.ODP -> DocuGlyph(
            icon = DocuIcons.Slides,
            contentDescription = format.displayName,
            modifier = modifier
        )
    }
}

/** Intentional empty state: glyph + headline + explanation + optional action. */
@Composable
fun DocuEmptyState(
    icon: ImageVector,
    headline: String,
    explanation: String,
    modifier: Modifier = Modifier,
    /** Drawable resource override — when set, drawn instead of [icon]. */
    iconRes: Int? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DocuSpacing.lg, vertical = DocuSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(DocuSpacing.md))
        Text(headline, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(DocuSpacing.xxs))
        Text(
            explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            Spacer(Modifier.height(DocuSpacing.md))
            action()
        }
    }
}

/** Compact section header: title + optional trailing action. */
@Composable
fun DocuSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (action != null) action()
    }
}

/** Metadata line with ellipsis (filename-safe). */
@Composable
fun ColumnScope.DocuFilename(
    name: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    Text(
        name,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Small supporting caption line. */
@Composable
fun DocuCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Subtle privacy strip — honest offline claim, no banner. */
@Composable
fun OfflineStrip(modifier: Modifier = Modifier) {
    val subtitle = stringResource(com.docuconvert.app.R.string.offline_subtitle)
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Offline: $subtitle" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocuSpacing.md, vertical = DocuSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
        ) {
            Icon(
                imageVector = DocuIcons.Success,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(com.docuconvert.app.R.string.offline_title),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** File summary card used by Convert (icon + name + meta). */
@Composable
fun FileSummaryCard(
    name: String,
    meta: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DocuSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocuSpacing.sm)
        ) {
            leading()
            Column(Modifier.weight(1f)) {
                DocuFilename(name)
                Spacer(Modifier.height(2.dp))
                DocuCaption(meta)
            }
        }
    }
}
