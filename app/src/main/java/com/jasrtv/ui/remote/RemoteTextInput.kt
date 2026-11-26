package com.jasrtv.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

private enum class InputMode(val label: String) {
    LIVE("En vivo"), PIN("PIN"), MANUAL("Manual")
}

private val Violet = Color(0xFF7532EC)
private val Lavender = Color(0xFFBB8AFF)
private val FieldBackground = Color(0xFF141326)
private val CardInk = Color(0xFFF7F3FF)
private val MutedInk = Color(0xFFC7BDD8)

@Composable
internal fun RemoteTextInput(onText: (String) -> Boolean, onDown: () -> Boolean) {
    var mode by remember { mutableStateOf(InputMode.MANUAL) }
    var text by remember(mode) { mutableStateOf("") }
    var hidden by remember(mode) { mutableStateOf(false) }
    var feedback by remember(mode) { mutableStateOf<String?>(null) }
    var sending by remember(mode) { mutableStateOf(false) }
    val pinQueue = remember(mode) { Channel<String>(Channel.UNLIMITED) }
    val sendText by rememberUpdatedState(onText)
    val sendDown by rememberUpdatedState(onDown)
    val focus = LocalFocusManager.current

    if (mode == InputMode.PIN) {
        DisposableEffect(pinQueue) { onDispose { pinQueue.cancel() } }
        LaunchedEffect(pinQueue) {
            for (batch in pinQueue) {
                sending = true
                var offset = 0
                while (offset < batch.length) {
                    val next = batch.offsetByCodePoints(offset, 1)
                    if (!sendText(batch.substring(offset, next))) {
                        pinQueue.cancel()
                        sending = false
                        feedback = "No se pudo enviar el carácter al TV."
                        return@LaunchedEffect
                    }
                    delay(150)
                    if (!sendDown()) {
                        pinQueue.cancel()
                        sending = false
                        feedback = "No se pudo enviar Abajo al TV."
                        return@LaunchedEffect
                    }
                    delay(150)
                    offset = next
                }
                sending = false
            }
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF201A35), Color(0xFF151325))), cardShape)
            .border(1.dp, Color(0xFF514073), cardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextPanelIcon(TextPanelGlyph.WIFI, Lavender)
            Text("Enviar texto por Wi‑Fi", style = MaterialTheme.typography.titleMedium, color = CardInk)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (option in InputMode.entries) {
                TextModeButton(option, mode == option, !sending, Modifier.weight(1f)) { mode = option }
            }
        }
        if (mode == InputMode.PIN) {
            PinInput(onInput = { pinQueue.trySend(it) })
            Text(feedback ?: if (sending) "Enviando caracteres…"
                else "Cada carácter se envía seguido de Abajo. El campo del celular queda vacío.",
                style = MaterialTheme.typography.bodySmall, color = MutedInk)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { updated ->
                    if (updated.length > 512 || updated == text) return@OutlinedTextField
                    feedback = null
                    if (mode == InputMode.MANUAL || onText(updated)) {
                        text = updated
                    } else {
                        feedback = "No se pudo actualizar el texto del TV."
                    }
                },
                modifier = Modifier.weight(1f).semantics { contentDescription = "Texto para el TV" },
                placeholder = { Text("Toca para escribir") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CardInk, unfocusedTextColor = CardInk,
                    focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground,
                    focusedBorderColor = Lavender, unfocusedBorderColor = Color(0xFF625080),
                    focusedPlaceholderColor = MutedInk, unfocusedPlaceholderColor = MutedInk,
                    cursorColor = Lavender
                ),
                trailingIcon = {
                    IconButton(onClick = { hidden = !hidden },
                        modifier = Modifier.semantics {
                            contentDescription = if (hidden) "Mostrar texto" else "Ocultar texto"
                        }) {
                        TextPanelIcon(if (hidden) TextPanelGlyph.EYE_OFF else TextPanelGlyph.EYE, CardInk)
                    }
                },
                singleLine = true,
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (hidden) KeyboardType.Password else KeyboardType.Text,
                    autoCorrectEnabled = false
                )
            )
            val manual = mode == InputMode.MANUAL
            if (manual || text.isNotEmpty()) {
                Button(
                    enabled = text.isNotEmpty(),
                    onClick = {
                        if (onText(if (manual) text else "")) {
                            text = ""
                            feedback = if (manual) "Comprueba si apareció en el TV."
                            else "Comprueba si el campo del TV quedó vacío."
                            if (manual) focus.clearFocus()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = if (manual) "Enviar texto" else "Vaciar texto"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = CardInk),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    TextPanelIcon(if (manual) TextPanelGlyph.SEND else TextPanelGlyph.CLEAR,
                        LocalContentColor.current)
                }
            }
        }
        feedback?.let { Text(it, color = MutedInk, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun TextModeButton(mode: InputMode, active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val icon = when (mode) {
        InputMode.LIVE -> TextPanelGlyph.PLAY
        InputMode.PIN -> TextPanelGlyph.LOCK
        InputMode.MANUAL -> TextPanelGlyph.KEYBOARD
    }
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).semantics { selected = active },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFF9C68FF) else Color(0xFF423653)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Violet else Color(0xFF272338),
            contentColor = CardInk,
            disabledContainerColor = if (active) Color(0xFF49278B) else Color(0xFF201D31),
            disabledContentColor = MutedInk
        ),
        contentPadding = PaddingValues(horizontal = 3.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextPanelIcon(icon, LocalContentColor.current, Modifier.size(15.dp))
            Text(mode.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun PinInput(onInput: (String) -> Unit) {
    val state = rememberTextFieldState()
    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Escribir PIN" },
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CardInk),
        cursorBrush = SolidColor(Lavender),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        inputTransformation = InputTransformation {
            val input = asCharSequence().toString()
            revertAllChanges()
            if (input.isNotEmpty()) onInput(input)
        },
        decorator = { innerTextField ->
            val shape = RoundedCornerShape(12.dp)
            Box(Modifier.fillMaxWidth().background(FieldBackground, shape).border(1.dp, Lavender, shape)
                .padding(16.dp)) {
                Text("Toca para escribir", color = MutedInk)
                innerTextField()
            }
        }
    )
}
