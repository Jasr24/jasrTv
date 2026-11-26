package com.jasrtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasrtv.ui.devices.DevicesScreen
import com.jasrtv.ui.devices.DevicesViewModel
import com.jasrtv.ui.remote.RemoteScreen
import com.jasrtv.ui.remote.RemoteHeader
import com.jasrtv.ui.remote.RemoteViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF88DBC7), onPrimary = Color(0xFF00382F),
                background = Color(0xFF111814), surface = Color(0xFF18211D)
            )) {
                val devices: DevicesViewModel = viewModel()
                val remote: RemoteViewModel = viewModel()
                val selected by remote.selected.collectAsStateWithLifecycle()
                val visibleDevice = selected
                val connection by remote.connection.collectAsStateWithLifecycle()
                BackHandler(enabled = visibleDevice != null, onBack = remote::changeTv)
                Scaffold(topBar = {
                    if (visibleDevice == null) {
                        TopAppBar(title = { Text("jasrTv") })
                    } else {
                        RemoteHeader(visibleDevice, connection, remote::reconnect, remote::pairAgain)
                    }
                }) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (visibleDevice == null) {
                            LaunchedEffect(devices) { devices.refreshRemembered() }
                            val state by devices.state.collectAsStateWithLifecycle()
                            DevicesScreen(state, devices::search, remote::select, devices::deviceByAddress)
                        } else {
                            val voiceActive by remote.voiceActive.collectAsStateWithLifecycle()
                            val powerMessage by remote.powerMessage.collectAsStateWithLifecycle()
                            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { remote.stopVoice() }
                            RemoteScreen(visibleDevice, connection, remote::sendKey, remote::sendText,
                                voiceActive, remote::toggleVoice,
                                powerMessage, remote::powerOff, remote::wakeTv)
                        }
                    }
                }
            }
        }
    }
}
