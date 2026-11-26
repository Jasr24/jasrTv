package com.jasrtv.ui.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

internal enum class TextPanelGlyph { WIFI, PLAY, LOCK, KEYBOARD, EYE, EYE_OFF, SEND, CLEAR }

@Composable
internal fun TextPanelIcon(glyph: TextPanelGlyph, color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        val unit = size.width / 24f
        val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(unit, unit, pivot = Offset.Zero) {
            when (glyph) {
                TextPanelGlyph.WIFI -> {
                    for (radius in listOf(4f, 8f, 12f)) {
                        drawArc(color, 225f, 90f, false, Offset(12f - radius, 18f - radius),
                            Size(radius * 2, radius * 2), style = stroke)
                    }
                    drawCircle(color, 1.35f, Offset(12f, 18f))
                }
                TextPanelGlyph.PLAY -> drawPath(Path().apply {
                    moveTo(6f, 4f)
                    lineTo(20f, 12f)
                    lineTo(6f, 20f)
                    close()
                }, color, style = stroke)
                TextPanelGlyph.LOCK -> {
                    drawRoundRect(color, Offset(5f, 10f), Size(14f, 11f), CornerRadius(2f), style = stroke)
                    drawArc(color, 180f, 180f, false, Offset(8f, 3f), Size(8f, 12f), style = stroke)
                    drawLine(color, Offset(12f, 14f), Offset(12f, 17f), 1.8f, StrokeCap.Round)
                }
                TextPanelGlyph.KEYBOARD -> {
                    drawRoundRect(color, Offset(2f, 5f), Size(20f, 14f), CornerRadius(3f), style = stroke)
                    for (y in listOf(9f, 12f)) {
                        for (x in listOf(6f, 10f, 14f, 18f)) drawCircle(color, 0.95f, Offset(x, y))
                    }
                    drawLine(color, Offset(8f, 15f), Offset(16f, 15f), 1.8f, StrokeCap.Round)
                }
                TextPanelGlyph.EYE, TextPanelGlyph.EYE_OFF -> {
                    drawPath(Path().apply {
                        moveTo(2f, 12f)
                        cubicTo(7f, 4f, 17f, 4f, 22f, 12f)
                        cubicTo(17f, 20f, 7f, 20f, 2f, 12f)
                        close()
                    }, color, style = stroke)
                    if (glyph == TextPanelGlyph.EYE_OFF) {
                        drawLine(color, Offset(4f, 4f), Offset(20f, 20f), 2f, StrokeCap.Round)
                    } else {
                        drawCircle(color, 2.7f, Offset(12f, 12f), style = stroke)
                    }
                }
                TextPanelGlyph.SEND -> drawPath(Path().apply {
                    moveTo(3f, 10f)
                    lineTo(21f, 3f)
                    lineTo(15f, 21f)
                    lineTo(11f, 13f)
                    close()
                    moveTo(11f, 13f)
                    lineTo(21f, 3f)
                }, color, style = stroke)
                TextPanelGlyph.CLEAR -> {
                    drawLine(color, Offset(6f, 6f), Offset(18f, 18f), 2f, StrokeCap.Round)
                    drawLine(color, Offset(18f, 6f), Offset(6f, 18f), 2f, StrokeCap.Round)
                }
            }
        }
    }
}
