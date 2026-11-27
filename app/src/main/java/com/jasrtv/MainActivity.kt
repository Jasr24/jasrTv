package com.jasrtv

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasrtv.ui.AppBrand
import com.jasrtv.ui.JasrTvTheme
import com.jasrtv.ui.RemoteBackdrop
import com.jasrtv.ui.RemoteTheme
import com.jasrtv.ui.ThemePicker
import com.jasrtv.ui.devices.DevicesScreen
import com.jasrtv.ui.devices.DevicesViewModel
import com.jasrtv.ui.remote.RemoteScreen
import com.jasrtv.ui.remote.RemoteHeader
import com.jasrtv.ui.remote.RemoteViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        val appearance = getSharedPreferences("appearance", Context.MODE_PRIVATE)
        setContent {
            var theme by remember {
                val saved = appearance.getString("theme", null)
                mutableStateOf(RemoteTheme.entries.firstOrNull { it.name == saved } ?: RemoteTheme.VIOLET)
            }
            var showTheme by rememberSaveable { mutableStateOf(false) }
            JasrTvTheme(theme) {
                val devices: DevicesViewModel = viewModel()
                val remote: RemoteViewModel = viewModel()
                val selected by remote.selected.collectAsStateWithLifecycle()
                val visibleDevice = selected
                val connection by remote.connection.collectAsStateWithLifecycle()
                BackHandler(enabled = visibleDevice != null, onBack = remote::changeTv)
                Scaffold(topBar = {
                    if (visibleDevice == null) {
                        TopAppBar(title = { AppBrand { showTheme = true } })
                    } else {
                        RemoteHeader(visibleDevice, connection, remote::reconnect, remote::pairAgain,
                            onChooseTheme = { showTheme = true })
                    }
                }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        RemoteBackdrop()
                        if (visibleDevice == null) {
                            LaunchedEffect(devices) { devices.refreshRemembered() }
                            val state by devices.state.collectAsStateWithLifecycle()
                            DevicesScreen(state, devices::search, remote::select, devices::deviceByAddress)
                        } else {
                            val voiceActive by remote.voiceActive.collectAsStateWithLifecycle()
                            val powerMessage by remote.powerMessage.collectAsStateWithLifecycle()
                            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { remote.stopVoice() }
                            RemoteScreen(visibleDevice, connection, remote::sendKey, remote::sendText, remote::launchApp,
                                voiceActive, remote::toggleVoice,
                                powerMessage, remote::powerOff, remote::wakeTv)
                        }
                    }
                }
                if (showTheme) {
                    ThemePicker(theme, onSelect = {
                        theme = it
                        appearance.edit().putString("theme", it.name).apply()
                    }, onDismiss = { showTheme = false })
                }
            }
        }
    }
}
