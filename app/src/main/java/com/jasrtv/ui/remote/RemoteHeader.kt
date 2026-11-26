package com.jasrtv.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.remote.ConnectionState
import com.jasrtv.ui.AppBrand

@Composable
internal fun RemoteHeader(
    device: TvDevice,
    connection: ConnectionState,
    onReconnect: () -> Unit,
    onPairAgain: () -> Unit,
    onChooseTheme: () -> Unit
) {
    val connecting = connection == ConnectionState.CONNECTING || connection == ConnectionState.WAITING_FOR_TV
    val connected = connection == ConnectionState.CONNECTED
    val color = when {
        connected -> Color(0xFF56E785)
        connecting -> Color(0xFFFFCD54)
        else -> Color(0xFFFF626B)
    }
    val description = when (connection) {
        ConnectionState.CONNECTED -> "TV conectado"
        ConnectionState.CONNECTING -> "Conectando al TV"
        ConnectionState.WAITING_FOR_TV -> "Esperando autorización del TV"
        ConnectionState.CERTIFICATE_CHANGED -> "Cambió la identidad del TV. Volver a emparejar"
        ConnectionState.DENIED -> "El TV rechazó la conexión. Reconectar"
        ConnectionState.TIMED_OUT -> "El TV tardó demasiado. Reconectar"
        ConnectionState.UNREACHABLE -> "No se pudo conectar al TV. Reconectar"
        ConnectionState.DISCONNECTED -> "TV desconectado. Reconectar"
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppBrand(onChooseTheme)
                VerticalDivider(Modifier.height(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name.removePrefix("[TV]").trimStart().ifBlank { "Samsung TV" },
                        style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(device.address, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (connected || connecting) {
                    Box(Modifier.size(48.dp).semantics { contentDescription = description },
                        contentAlignment = Alignment.Center) {
                        StatusDot(color)
                    }
                } else {
                    IconButton(
                        onClick = if (connection == ConnectionState.CERTIFICATE_CHANGED) onPairAgain else onReconnect,
                        modifier = Modifier.semantics { contentDescription = description }
                    ) { StatusDot(color) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(24.dp).background(color.copy(alpha = 0.16f), CircleShape)
        .border(1.dp, color, CircleShape),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
    }
}
