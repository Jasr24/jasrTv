package com.jasrtv.ui.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jasrtv.data.discovery.TvDevice

@Composable
fun DevicesScreen(
    state: DevicesUiState,
    onSearch: () -> Unit,
    onSelect: (TvDevice) -> Unit,
    deviceByAddress: (String) -> TvDevice?
) {
    var showAddress by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val newDevices = state.devices.filter { found ->
        state.remembered.none { saved ->
            saved.id == found.id ||
                (saved.address == found.address && (saved.id == saved.address || found.id == found.address))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 24.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                DevicesIcon(DevicesGlyph.TV, colors.primary, Modifier.size(38.dp))
                Text("Televisores", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (state.remembered.isNotEmpty()) {
            item { DeviceSectionTitle("Mis televisores") }
            items(state.remembered, key = { "saved:${it.id}" }) { saved ->
                val found = state.devices.firstOrNull { it.id == saved.id }
                val target = found ?: saved
                val addressReassigned = found == null && state.devices.any {
                    it.address == saved.address && it.id != saved.id && it.id != it.address
                } && saved.id != saved.address
                val status = when {
                    addressReassigned -> DeviceStatus.REASSIGNED
                    target.address in state.reachable.orEmpty() -> DeviceStatus.AVAILABLE
                    state.searching || state.reachable == null -> DeviceStatus.CHECKING
                    else -> DeviceStatus.UNREACHABLE
                }
                TvCard(target, onSelect, status, enabled = !addressReassigned)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSearch, enabled = !state.searching, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                    ActionLabel(DevicesGlyph.SEARCH, "Buscar de nuevo")
                }
                OutlinedButton(onClick = { showAddress = true }, modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
                    ActionLabel(DevicesGlyph.ADD, "Agregar por IP", colors.onSurface)
                }
            }
        }
        if (state.searching) {
            item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Buscando televisores…", Modifier.padding(top = 8.dp))
            }
        } else if (state.searchFailed) {
            item { Text("Falló la búsqueda. Revisa el Wi-Fi y vuelve a intentar.") }
        } else if (state.devices.isEmpty() && state.remembered.isEmpty()) {
            item { Text("No encontramos televisores. Si el Samsung está encendido, prueba Agregar por IP.") }
        }
        if (newDevices.isNotEmpty() && state.remembered.isNotEmpty()) {
            item { DeviceSectionTitle("Otros televisores") }
        }
        items(newDevices, key = { "found:${it.id}" }) { device ->
            TvCard(device, onSelect)
        }
    }
    if (showAddress) {
        var address by remember { mutableStateOf("") }
        var invalid by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddress = false },
            title = { Text("Agregar por IP") },
            text = {
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        invalid = false
                    },
                    label = { Text("Dirección IPv4 del TV") },
                    placeholder = { Text("192.168.1.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { if (invalid) Text("Escribe una dirección IPv4 privada válida.") },
                    singleLine = true,
                    isError = invalid
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val device = deviceByAddress(address)
                    if (device == null) {
                        invalid = true
                    } else {
                        showAddress = false
                        onSelect(device)
                    }
                }) { Text("Conectar") }
            },
            dismissButton = { TextButton(onClick = { showAddress = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DeviceSectionTitle(title: String) {
    Text(title, Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
}

private enum class DeviceStatus(val label: String) {
    AVAILABLE("Disponible en la red"),
    CHECKING("Comprobando conexión…"),
    UNREACHABLE("Sin respuesta en la red"),
    REASSIGNED("La última IP pertenece a otro equipo")
}

@Composable
private fun TvCard(device: TvDevice, onSelect: (TvDevice) -> Unit, status: DeviceStatus? = null, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val edge = if (enabled) colors.primary else colors.outline
    val shape = RoundedCornerShape(20.dp)
    Surface(onClick = { onSelect(device) }, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = shape, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.surfaceVariant, colors.surface)))
                .border(1.dp, edge, shape).padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val badge = RoundedCornerShape(13.dp)
            Box(Modifier.size(48.dp)
                .background(Brush.verticalGradient(listOf(colors.surfaceVariant, colors.background)), badge)
                .border(1.dp, colors.outline, badge), contentAlignment = Alignment.Center) {
                DevicesIcon(DevicesGlyph.TV, colors.onSurface, Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(listOf(device.model, device.address).filter(String::isNotBlank).joinToString(" · "),
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (status != null) {
                    val statusColor = when (status) {
                        DeviceStatus.AVAILABLE -> colors.primary
                        DeviceStatus.CHECKING -> colors.onSurfaceVariant
                        DeviceStatus.UNREACHABLE, DeviceStatus.REASSIGNED -> Color(0xFFFFBF69)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                        Text(status.label, color = statusColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (enabled) {
                Box(Modifier.size(28.dp).background(colors.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    DevicesIcon(DevicesGlyph.CHEVRON, colors.onSurface, Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionLabel(glyph: DevicesGlyph, label: String, color: Color = LocalContentColor.current) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        DevicesIcon(glyph, color, Modifier.size(18.dp))
        Text(label, color = color)
    }
}

private enum class DevicesGlyph { TV, SEARCH, ADD, CHEVRON }

@Composable
private fun DevicesIcon(glyph: DevicesGlyph, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.width / 24f
        scale(unit, unit, pivot = Offset.Zero) {
            val width = 2f
            when (glyph) {
                DevicesGlyph.TV -> {
                    drawRoundRect(color, Offset(2f, 4f), Size(20f, 13f), CornerRadius(2.5f), style = Stroke(width))
                    drawLine(color, Offset(12f, 17f), Offset(12f, 21f), width, StrokeCap.Round)
                    drawLine(color, Offset(8f, 21f), Offset(16f, 21f), width, StrokeCap.Round)
                }
                DevicesGlyph.SEARCH -> {
                    drawCircle(color, 6f, Offset(10f, 10f), style = Stroke(width))
                    drawLine(color, Offset(14.5f, 14.5f), Offset(21f, 21f), width, StrokeCap.Round)
                }
                DevicesGlyph.ADD -> {
                    drawLine(color, Offset(4f, 12f), Offset(20f, 12f), width, StrokeCap.Round)
                    drawLine(color, Offset(12f, 4f), Offset(12f, 20f), width, StrokeCap.Round)
                }
                DevicesGlyph.CHEVRON -> {
                    drawLine(color, Offset(9f, 5f), Offset(16f, 12f), width, StrokeCap.Round)
                    drawLine(color, Offset(16f, 12f), Offset(9f, 19f), width, StrokeCap.Round)
                }
            }
        }
    }
}
