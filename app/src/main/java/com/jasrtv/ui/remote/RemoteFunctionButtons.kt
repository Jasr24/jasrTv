package com.jasrtv.ui.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jasrtv.data.remote.RemoteKey

private enum class FunctionIcon { BACK, HOME, MICROPHONE }

@Composable
internal fun RemoteFunctionButtons(
    enabled: Boolean,
    voiceActive: Boolean,
    onVoice: () -> Unit,
    onKey: (RemoteKey) -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(FunctionIcon.BACK, "Atrás", enabled) { onKey(RemoteKey.BACK) }
            IconButton(FunctionIcon.HOME, "Inicio", enabled) { onKey(RemoteKey.HOME) }
            IconButton(FunctionIcon.MICROPHONE, if (voiceActive) "Terminar Bixby" else "Bixby",
                enabled, active = voiceActive, onClick = onVoice)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KeyPair("Volumen", RemoteKey.VOLUME_DOWN, RemoteKey.VOLUME_UP,
                enabled, Modifier.weight(1f), onKey)
            KeyPair("Canal", RemoteKey.CHANNEL_DOWN, RemoteKey.CHANNEL_UP,
                enabled, Modifier.weight(1f), onKey)
        }
    }
}

@Composable
private fun KeyPair(
    label: String,
    down: RemoteKey,
    up: RemoteKey,
    enabled: Boolean,
    modifier: Modifier,
    onKey: (RemoteKey) -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for ((key, symbol, action) in listOf(Triple(down, "−", "Bajar"), Triple(up, "+", "Subir"))) {
                FilledTonalButton(onClick = { onKey(key) }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "$action ${label.lowercase()}" },
                    contentPadding = PaddingValues(0.dp)) {
                    Text(symbol, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun IconButton(
    icon: FunctionIcon,
    label: String,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colors = if (active) ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) else ButtonDefaults.filledTonalButtonColors()
    FilledTonalButton(
        onClick = onClick, enabled = enabled, shape = CircleShape, colors = colors,
        modifier = Modifier.size(52.dp).semantics { contentDescription = label },
        contentPadding = PaddingValues(0.dp)
    ) {
        val color = LocalContentColor.current
        Canvas(Modifier.size(24.dp)) {
            val unit = size.width / 24f
            val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            scale(unit, unit, pivot = Offset.Zero) {
                when (icon) {
                    FunctionIcon.BACK -> drawPath(Path().apply {
                        moveTo(9f, 6f)
                        lineTo(4f, 11f)
                        lineTo(9f, 16f)
                        moveTo(4f, 11f)
                        lineTo(15f, 11f)
                        cubicTo(21f, 11f, 21f, 19f, 15f, 19f)
                        lineTo(11f, 19f)
                    }, color, style = stroke)
                    FunctionIcon.HOME -> drawPath(Path().apply {
                        moveTo(4f, 10f)
                        lineTo(12f, 4f)
                        lineTo(20f, 10f)
                        lineTo(20f, 20f)
                        lineTo(4f, 20f)
                        close()
                        moveTo(10f, 20f)
                        lineTo(10f, 14f)
                        lineTo(14f, 14f)
                        lineTo(14f, 20f)
                    }, color, style = stroke)
                    FunctionIcon.MICROPHONE -> {
                        drawRoundRect(color, Offset(9f, 3f), Size(6f, 11f), CornerRadius(3f), style = stroke)
                        drawPath(Path().apply {
                            moveTo(6f, 11f)
                            lineTo(6f, 12f)
                            cubicTo(6f, 20f, 18f, 20f, 18f, 12f)
                            lineTo(18f, 11f)
                            moveTo(12f, 18f)
                            lineTo(12f, 21f)
                            moveTo(9f, 21f)
                            lineTo(15f, 21f)
                        }, color, style = stroke)
                    }
                }
            }
        }
    }
}
