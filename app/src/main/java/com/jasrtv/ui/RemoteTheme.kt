package com.jasrtv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal enum class RemoteTheme(val label: String, val accent: Color, val fill: Color) {
    VIOLET("Morado", Color(0xFFCC91FF), Color(0xFF8015CC)),
    RED("Rojo", Color(0xFFFF5766), Color(0xFFBD1028)),
    ORANGE("Naranja", Color(0xFFFFAA48), Color(0xFFA54004)),
    GREEN("Verde", Color(0xFF4EF0A1), Color(0xFF087148))
}

@Composable
internal fun JasrTvTheme(theme: RemoteTheme, content: @Composable () -> Unit) {
    val base = Color(0xFF09080C)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = theme.accent,
            onPrimary = Color(0xFF1A101C),
            primaryContainer = theme.fill,
            onPrimaryContainer = Color(0xFFFFF6FF),
            background = base,
            surface = lerp(base, theme.accent, 0.07f),
            surfaceVariant = lerp(base, theme.accent, 0.15f),
            onSurface = Color(0xFFFFF8FF),
            onSurfaceVariant = Color(0xFFCEC3D1),
            outline = lerp(base, theme.accent, 0.55f),
            outlineVariant = lerp(base, theme.accent, 0.27f)
        ),
        content = content
    )
}

@Composable
internal fun RemoteBackdrop() {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        drawRect(colors.background)
        drawRect(Brush.radialGradient(
            listOf(colors.primaryContainer.copy(alpha = 0.43f), Color.Transparent),
            Offset(width, height * 0.19f), width * 0.88f
        ))
        drawRect(Brush.radialGradient(
            listOf(colors.primaryContainer.copy(alpha = 0.31f), Color.Transparent),
            Offset(0f, height * 0.68f), width
        ))
        val sweep = Path().apply {
            moveTo(-width * 0.03f, height * 0.57f)
            cubicTo(width * 0.28f, height * 0.34f, width * 0.82f, height * 0.46f,
                width * 1.03f, height * 0.15f)
            lineTo(width * 1.03f, height * 0.20f)
            cubicTo(width * 0.68f, height * 0.52f, width * 0.23f, height * 0.42f,
                -width * 0.03f, height * 0.62f)
            close()
        }
        drawPath(sweep, Brush.horizontalGradient(
            listOf(colors.primary.copy(alpha = 0.30f), Color.Transparent, colors.primary.copy(alpha = 0.35f))
        ))
        val horizon = Path().apply {
            moveTo(-width * 0.05f, height)
            cubicTo(width * 0.27f, height * 0.82f, width * 0.72f, height * 1.04f,
                width * 1.05f, height * 0.77f)
        }
        drawPath(horizon, colors.primary.copy(alpha = 0.18f), style = Stroke(2.dp.toPx()))
    }
}

@Composable
internal fun RemoteControlButton(
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val finish = if (highlighted) colors.primaryContainer else colors.background
    val start = lerp(finish, accent, if (highlighted) 0.18f else 0.23f)
    val edge = accent.copy(alpha = if (enabled) 0.85f else 0.35f)
    Surface(onClick = onClick, enabled = enabled, modifier = modifier, shape = shape,
        color = Color.Transparent, contentColor = colors.onSurface) {
        Box(Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(start, finish)))
            .border(1.5.dp, Brush.verticalGradient(listOf(edge, edge.copy(alpha = edge.alpha * 0.32f), edge)), shape),
            contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface.copy(alpha = if (enabled) 1f else 0.45f)) {
                content()
            }
        }
    }
}

@Composable
internal fun ThemePicker(selected: RemoteTheme, onSelect: (RemoteTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color del control") },
        text = {
            Column {
                for (theme in RemoteTheme.entries) {
                    Row(Modifier.fillMaxWidth().selectable(
                        selected = theme == selected, role = Role.RadioButton,
                        onClick = { onSelect(theme) }
                    ).padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == selected, onClick = null)
                        Box(Modifier.size(18.dp).background(theme.accent, CircleShape))
                        Text(theme.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}
