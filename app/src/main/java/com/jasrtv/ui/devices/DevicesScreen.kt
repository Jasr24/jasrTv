package com.jasrtv.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Modifier
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
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Televisores", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.headlineLarge)
            Text("Elige un Samsung de tu red Wi-Fi.", Modifier.padding(top = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSearch, enabled = !state.searching) { Text("Buscar de nuevo") }
                TextButton(onClick = { showAddress = true }) { Text("Agregar por IP") }
            }
        }
        if (state.searching) {
            item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Buscando televisores…", Modifier.padding(top = 8.dp))
            }
        } else if (state.searchFailed) {
            item { Text("Falló la búsqueda. Revisa el Wi-Fi y vuelve a intentar.") }
        } else if (state.devices.isEmpty()) {
            item { Text("No encontramos televisores. Enciende el Samsung y verifica que esté en la misma red.") }
        }
        items(state.devices, key = { it.id }) { device ->
            ElevatedCard(onClick = { onSelect(device) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(device.name, style = MaterialTheme.typography.titleLarge)
                    Text(listOf(device.model, device.address).filter(String::isNotBlank).joinToString(" · "))
                }
            }
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
