package com.jasrtv.ui.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jasrtv.data.remote.RemoteKey
import com.jasrtv.ui.RemoteControlButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private enum class FunctionIcon { BACK, HOME, MICROPHONE, MUTE, VOLUME, CHANNEL }

@Composable
internal fun RemoteFunctionButtons(
    enabled: Boolean,
    onKey: (RemoteKey) -> Boolean
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(FunctionIcon.BACK, "Atrás", enabled) { onKey(RemoteKey.BACK) }
            IconButton(FunctionIcon.HOME, "Inicio", enabled) { onKey(RemoteKey.HOME) }
            IconButton(FunctionIcon.MUTE, "Silenciar o reactivar el sonido", enabled) { onKey(RemoteKey.MUTE) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            VerticalControl("Volumen", FunctionIcon.VOLUME, RemoteKey.VOLUME_UP, RemoteKey.VOLUME_DOWN,
                enabled, repeatInterval = 150, onKey = onKey)
            VerticalControl("Canal", FunctionIcon.CHANNEL, RemoteKey.CHANNEL_UP, RemoteKey.CHANNEL_DOWN,
                enabled, repeatInterval = 350, onKey = onKey)
        }
    }
}

@Composable
internal fun BixbyButton(enabled: Boolean, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    IconButton(FunctionIcon.MICROPHONE, if (active) "Terminar Bixby" else "Bixby",
        enabled, modifier = modifier, active = active, onClick = onClick)
}

@Composable
private fun VerticalControl(
    label: String,
    icon: FunctionIcon,
    up: RemoteKey,
    down: RemoteKey,
    enabled: Boolean,
    repeatInterval: Long,
    onKey: (RemoteKey) -> Boolean
) {
    val shape = RoundedCornerShape(28.dp)
    val colors = MaterialTheme.colorScheme
    val ink = colors.onSurface.copy(alpha = if (enabled) 1f else 0.45f)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            Modifier.width(64.dp)
                .background(Brush.verticalGradient(listOf(colors.surfaceVariant, colors.background)), shape)
                .border(1.5.dp, Brush.verticalGradient(
                    listOf(colors.primary, colors.outlineVariant, colors.primary)
                ), shape),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerticalControlKey("+", "Subir ${label.lowercase()}", enabled, ink, repeatInterval) { onKey(up) }
            RemoteIcon(icon, ink)
            VerticalControlKey("−", "Bajar ${label.lowercase()}", enabled, ink, repeatInterval) { onKey(down) }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalControlKey(
    symbol: String, label: String, enabled: Boolean, color: Color, repeatInterval: Long, onClick: () -> Boolean
) {
    val interactions = remember { MutableInteractionSource() }
    val sendKey by rememberUpdatedState(onClick)
    val holdTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
    LaunchedEffect(interactions, enabled, holdTimeout, repeatInterval) {
        if (!enabled) return@LaunchedEffect
        interactions.interactions.collectLatest { interaction ->
            if (interaction is PressInteraction.Press) {
                // The long-click sends the first step. Release or cancellation stops this loop.
                delay(holdTimeout + repeatInterval)
                while (sendKey()) delay(repeatInterval)
            }
        }
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape)
            .combinedClickable(
                interactionSource = interactions,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = "Repetir acción",
                onLongClick = { onClick() },
                onClick = { onClick() }
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = color, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun IconButton(
    icon: FunctionIcon,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    RemoteControlButton(
        onClick = onClick, enabled = enabled, shape = CircleShape, highlighted = active,
        modifier = modifier.size(52.dp).semantics { contentDescription = label }
    ) {
        RemoteIcon(icon, LocalContentColor.current)
    }
}

@Composable
private fun RemoteIcon(icon: FunctionIcon, color: Color) {
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
                FunctionIcon.MUTE, FunctionIcon.VOLUME -> drawPath(Path().apply {
                    moveTo(3f, 9f)
                    lineTo(7f, 9f)
                    lineTo(12f, 5f)
                    lineTo(12f, 19f)
                    lineTo(7f, 15f)
                    lineTo(3f, 15f)
                    close()
                    if (icon == FunctionIcon.VOLUME) {
                        moveTo(15f, 9f)
                        quadraticTo(18f, 12f, 15f, 15f)
                        moveTo(18f, 6f)
                        quadraticTo(23f, 12f, 18f, 18f)
                    } else {
                        moveTo(16f, 10f)
                        lineTo(21f, 15f)
                        moveTo(21f, 10f)
                        lineTo(16f, 15f)
                    }
                }, color, style = stroke)
                FunctionIcon.CHANNEL -> {
                    drawRoundRect(color, Offset(3f, 7f), Size(18f, 13f), CornerRadius(2f), style = stroke)
                    drawPath(Path().apply {
                        moveTo(8f, 3f)
                        lineTo(12f, 7f)
                        lineTo(16f, 3f)
                    }, color, style = stroke)
                }
            }
        }
    }
}
