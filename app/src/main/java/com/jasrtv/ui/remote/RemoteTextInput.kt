package com.jasrtv.ui.remote

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

private enum class InputMode(val label: String) {
    LIVE("En vivo"), PIN("PIN"), MANUAL("Manual")
}

@OptIn(ExperimentalLayoutApi::class)
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enviar texto por Wi‑Fi", style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in InputMode.entries) {
                FilterChip(selected = mode == option, enabled = !sending,
                    onClick = { mode = option }, label = { Text(option.label) })
            }
        }
        if (mode == InputMode.PIN) {
            PinInput(onInput = { pinQueue.trySend(it) })
            Text(feedback ?: if (sending) "Enviando caracteres…"
                else "Cada carácter se envía seguido de Abajo. El campo del celular queda vacío.",
                style = MaterialTheme.typography.bodySmall)
            return@Column
        }
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Texto") },
            singleLine = true,
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (hidden) KeyboardType.Password else KeyboardType.Text,
                autoCorrectEnabled = false
            ),
            supportingText = {
                Text(feedback ?: if (mode == InputMode.LIVE) {
                    "Abre el campo en el TV. Cada edición envía el texto completo."
                } else {
                    "Abre el campo de texto en el TV antes de enviar."
                })
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode == InputMode.MANUAL) {
                Button(enabled = text.isNotEmpty(), onClick = {
                    if (onText(text)) {
                        text = ""
                        feedback = "Comprueba si apareció en el TV."
                        focus.clearFocus()
                    }
                }) { Text("Enviar") }
            } else {
                TextButton(enabled = text.isNotEmpty(), onClick = {
                    if (onText("")) {
                        text = ""
                        feedback = "Comprueba si el campo del TV quedó vacío."
                    }
                }) { Text("Vaciar") }
            }
            TextButton(onClick = { hidden = !hidden }) {
                Text(if (hidden) "Mostrar texto" else "Ocultar texto")
            }
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
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        inputTransformation = InputTransformation {
            val input = asCharSequence().toString()
            revertAllChanges()
            if (input.isNotEmpty()) onInput(input)
        },
        decorator = { innerTextField ->
            Box(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .padding(16.dp)) {
                Text("Toca para escribir", color = MaterialTheme.colorScheme.onSurfaceVariant)
                innerTextField()
            }
        }
    )
}
