package com.jasrtv.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.remote.ConnectionState
import com.jasrtv.data.remote.RemoteKey

@Composable
fun RemoteScreen(
    device: TvDevice,
    connection: ConnectionState,
    onChangeTv: () -> Unit,
    onReconnect: () -> Unit,
    onPairAgain: () -> Unit,
    onKey: (RemoteKey) -> Unit,
    onText: (String) -> Boolean
) {
    val connected = connection == ConnectionState.CONNECTED
    val loading = connection in setOf(ConnectionState.CONNECTING, ConnectionState.WAITING_FOR_TV)
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(device.name, style = MaterialTheme.typography.headlineLarge)
            Text(device.address, style = MaterialTheme.typography.bodyMedium)
        }
        Text(connection.label(), color = MaterialTheme.colorScheme.primary)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onChangeTv) { Text("Cambiar TV") }
            if (!loading && !connected) {
                if (connection == ConnectionState.CERTIFICATE_CHANGED) {
                    TextButton(onClick = onPairAgain) { Text("Volver a emparejar") }
                } else {
                    TextButton(onClick = onReconnect) { Text("Reconectar") }
                }
            }
        }
        DirectionPad(connected, onKey)
        FilledTonalButton(
            onClick = { onKey(RemoteKey.BACK) },
            enabled = connected,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Atrás") }
        if (connected) RemoteTextInput(onText)
    }
}

@Composable
private fun DirectionPad(enabled: Boolean, onKey: (RemoteKey) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DirectionButton("↑", "Arriba", enabled) { onKey(RemoteKey.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DirectionButton("←", "Izquierda", enabled) { onKey(RemoteKey.LEFT) }
            Button(onClick = { onKey(RemoteKey.OK) }, enabled = enabled,
                modifier = Modifier.size(width = 80.dp, height = 58.dp)) { Text("OK") }
            DirectionButton("→", "Derecha", enabled) { onKey(RemoteKey.RIGHT) }
        }
        DirectionButton("↓", "Abajo", enabled) { onKey(RemoteKey.DOWN) }
    }
}

@Composable
private fun DirectionButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(58.dp).semantics { contentDescription = label },
        contentPadding = PaddingValues(0.dp)) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun RemoteTextInput(onText: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }
    var queued by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Column {
        Text("Escribir en el TV", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.length <= 512) {
                    text = it
                    queued = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Texto para enviar") },
            singleLine = true,
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (hidden) KeyboardType.Password else KeyboardType.Text,
                autoCorrectEnabled = false),
            supportingText = {
                Text(if (queued) "Comprueba si apareció en el TV." else "Abre primero el campo de texto en el TV.")
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = text.isNotEmpty(), onClick = {
                if (onText(text)) {
                    text = ""
                    queued = true
                    focus.clearFocus()
                }
            }) { Text("Enviar") }
            TextButton(onClick = { hidden = !hidden }) { Text(if (hidden) "Mostrar texto" else "Ocultar texto") }
        }
    }
}

private fun ConnectionState.label(): String = when (this) {
    ConnectionState.CONNECTING -> "Conectando…"
    ConnectionState.WAITING_FOR_TV -> "Si aparece un aviso, permite el control en el TV."
    ConnectionState.CONNECTED -> "Conectado"
    ConnectionState.DISCONNECTED -> "Desconectado"
    ConnectionState.DENIED -> "El TV rechazó la conexión. Revisa los permisos del televisor."
    ConnectionState.TIMED_OUT -> "El TV tardó demasiado en responder."
    ConnectionState.CERTIFICATE_CHANGED -> "Cambió la identidad del TV. Verifica el equipo antes de emparejarlo de nuevo."
    ConnectionState.UNREACHABLE -> "No se pudo conectar. Revisa el Wi-Fi y que el TV esté encendido."
}
