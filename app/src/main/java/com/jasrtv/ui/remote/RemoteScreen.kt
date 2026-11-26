package com.jasrtv.ui.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.remote.ConnectionState
import com.jasrtv.data.remote.RemoteKey
import com.jasrtv.data.remote.StreamingApp
import com.jasrtv.ui.RemoteControlButton

@Composable
fun RemoteScreen(
    device: TvDevice,
    connection: ConnectionState,
    onKey: (RemoteKey) -> Boolean,
    onText: (String) -> Boolean,
    onLaunchApp: (StreamingApp) -> Unit,
    voiceActive: Boolean,
    onVoice: () -> Unit,
    powerMessage: String?,
    onPowerOff: () -> Unit,
    onWake: (String?) -> Boolean
) {
    val connected = connection == ConnectionState.CONNECTED
    val loading = connection in setOf(ConnectionState.CONNECTING, ConnectionState.WAITING_FOR_TV)
    var showMac by remember(device.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (connection == ConnectionState.WAITING_FOR_TV) {
            Text("Si aparece un aviso, permite el control en el TV.", style = MaterialTheme.typography.bodySmall)
        }
        if (connection == ConnectionState.CERTIFICATE_CHANGED) {
            Text("Cambió la identidad del TV. Verifica el equipo antes de pulsar el botón rojo.",
                style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerButton(
                enabled = !loading,
                label = if (connected) "Apagar TV" else "Encender TV",
                modifier = Modifier,
                onClick = {
                    if (connected) onPowerOff() else if (!onWake(null)) showMac = true
                }
            )
            BixbyButton(connected, voiceActive, Modifier, onVoice)
        }
        DirectionPad(connected) { onKey(it) }
        RemoteFunctionButtons(connected) { onKey(it) }
        powerMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (voiceActive) Text("Si se abre Bixby, habla hacia el micrófono del TV.",
            style = MaterialTheme.typography.bodySmall)
        StreamingAppShortcuts(connected, onLaunchApp)
        if (connected) RemoteTextInput(onText, onDown = { onKey(RemoteKey.DOWN) })
    }
    if (showMac) {
        var mac by remember { mutableStateOf("") }
        var invalid by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showMac = false },
            title = { Text("Dirección MAC del TV") },
            text = {
                OutlinedTextField(
                    value = mac,
                    onValueChange = {
                        mac = it
                        invalid = false
                    },
                    label = { Text("MAC del adaptador de red del TV") },
                    placeholder = { Text("A0:B1:C2:D3:E4:F5") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = { if (invalid) Text("Escribe una dirección MAC válida.") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onWake(mac)) showMac = false else invalid = true
                }) { Text("Guardar y encender") }
            },
            dismissButton = { TextButton(onClick = { showMac = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PowerButton(enabled: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    val red = if (enabled) Color(0xFFFF5D64) else Color(0xFF965A5E)
    RemoteControlButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(52.dp).semantics { contentDescription = label },
        shape = CircleShape,
        accent = red
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 2.dp.toPx()
            val diameter = size.width * 0.68f
            drawArc(
                color = red,
                startAngle = -50f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset((size.width - diameter) / 2, size.height * 0.24f),
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = red,
                start = Offset(center.x, size.height * 0.10f),
                end = Offset(center.x, size.height * 0.52f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DirectionPad(enabled: Boolean, onKey: (RemoteKey) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DirectionButton("↑", "Arriba", enabled) { onKey(RemoteKey.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DirectionButton("←", "Izquierda", enabled) { onKey(RemoteKey.LEFT) }
            RemoteControlButton(onClick = { onKey(RemoteKey.OK) }, enabled = enabled,
                modifier = Modifier.size(width = 80.dp, height = 58.dp),
                shape = RoundedCornerShape(29.dp), highlighted = true) {
                Text("OK", style = MaterialTheme.typography.titleLarge)
            }
            DirectionButton("→", "Derecha", enabled) { onKey(RemoteKey.RIGHT) }
        }
        DirectionButton("↓", "Abajo", enabled) { onKey(RemoteKey.DOWN) }
    }
}

@Composable
private fun DirectionButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    RemoteControlButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(58.dp).semantics { contentDescription = label },
        shape = CircleShape) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}

