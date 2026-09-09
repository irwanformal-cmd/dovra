package com.docuconvert.app.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dovra brand mark: a document sheet with folded corner plus a
 * two-arrow conversion glyph — drawn in Compose (no external assets).
 *
 * @param size overall box size; artwork scales proportionally.
 * @param container brand surface the sheet sits on.
 * @param sheet document sheet color.
 * @param glyph conversion-arrows color.
 */
@Composable
fun DocuConvertLogo(
    size: Dp = 72.dp,
    container: Color = MaterialTheme.colorScheme.primary,
    sheet: Color = MaterialTheme.colorScheme.onPrimary,
    glyph: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.22f

        // Rounded container.
        drawRoundRect(
            color = container,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(radius, radius)
        )

        // Document sheet: white rounded rect with folded top-right corner.
        val mx = w * 0.24f
        val my = h * 0.16f
        val mw = w * 0.52f
        val mh = h * 0.68f
        val fold = w * 0.11f
        val sheetPath = Path().apply {
            moveTo(mx, my)
            lineTo(mx + mw - fold, my)
            lineTo(mx + mw, my + fold)
            lineTo(mx + mw, my + mh)
            lineTo(mx, my + mh)
            close()
        }
        clipPath(sheetPath) {
            drawRect(color = sheet, topLeft = Offset(mx, my), size = Size(mw, mh))
            // Fold shading.
            val foldPath = Path().apply {
                moveTo(mx + mw - fold, my)
                lineTo(mx + mw, my + fold)
                lineTo(mx + mw - fold, my + fold)
                close()
            }
            drawPath(foldPath, color = glyph.copy(alpha = 0.22f))
        }

        // Conversion arrows: two opposing horizontal arrows inside the sheet.
        val arrowColor = glyph
        val sw = w * 0.030f
        // Top arrow: left → right.
        val y1 = my + mh * 0.38f
        val x1s = mx + mw * 0.22f
        val x1e = mx + mw * 0.78f
        drawLine(arrowColor, Offset(x1s, y1), Offset(x1e, y1), strokeWidth = sw)
        val ah = w * 0.055f
        drawPath(
            Path().apply {
                moveTo(x1e, y1 - ah)
                lineTo(x1e + ah, y1)
                lineTo(x1e, y1 + ah)
                close()
            },
            color = arrowColor
        )
        // Bottom arrow: right → left.
        val y2 = my + mh * 0.62f
        val x2s = mx + mw * 0.78f
        val x2e = mx + mw * 0.22f
        drawLine(arrowColor, Offset(x2s, y2), Offset(x2e, y2), strokeWidth = sw)
        drawPath(
            Path().apply {
                moveTo(x2e, y2 - ah)
                lineTo(x2e - ah, y2)
                lineTo(x2e, y2 + ah)
                close()
            },
            color = arrowColor
        )

        // Sheet outline for contrast on light containers.
        drawPath(sheetPath, color = glyph.copy(alpha = 0.35f), style = Stroke(width = w * 0.012f))
    }
}
