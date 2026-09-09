package com.docuconvert.app.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docuconvert.app.R

/**
 * Central icon mapping — one place to swap iconography.
 *
 * Intent-named icons backed by local vector drawables (converted from
 * Material Symbols rounded paths — no new dependencies) with
 * material-icons-core fallbacks where no custom asset exists yet.
 * Names describe INTENT so a future asset swap touches only this file.
 */
object DocuIcons {
    val Home: ImageVector get() = Icons.Filled.Home
    val Convert: ImageVector get() = Icons.Filled.Refresh
    val Settings: ImageVector get() = Icons.Filled.Settings

    val OpenDocument: ImageVector get() = Icons.Filled.Add
    val Share: ImageVector get() = Icons.Filled.Share
    val Sheet: ImageVector get() = Icons.Filled.List
    val Slides: ImageVector get() = Icons.Filled.List

    val Success: ImageVector get() = Icons.Filled.Check
    val Error: ImageVector get() = Icons.Filled.Close
    val Save: ImageVector get() = Icons.Filled.Check
    val Info: ImageVector get() = Icons.Filled.Info
}

/** Custom local drawables (Material Symbols rounded paths, no new deps). */
object DocuDrawables {
    val Doc: Int get() = R.drawable.dc_doc
    val Folder: Int get() = R.drawable.dc_folder
    val Chevron: Int get() = R.drawable.dc_chevron
    val History: Int get() = R.drawable.dc_history
    val Heart: Int get() = R.drawable.dc_heart
}

/** Tinted glyph chip backed by a drawable resource. */
@Composable
fun DocuDrawableGlyph(
    resId: Int,
    contentDescription: String?,
    container: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
    content: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            painter = androidx.compose.ui.res.painterResource(resId),
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
    }
}

private typealias Modifier = androidx.compose.ui.Modifier
private typealias Alignment = androidx.compose.ui.Alignment
