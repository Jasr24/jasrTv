package com.jasrtv.ui.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.discovery.PairedTvStore
import com.jasrtv.data.discovery.SamsungDiscovery
import com.jasrtv.data.power.WakeOnLan
import com.jasrtv.data.remote.ConnectionState
import com.jasrtv.data.remote.RemoteKey
import com.jasrtv.data.remote.TvRemoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TvRemoteRepository(application)
    private val savedTvs = PairedTvStore(application)
    private val discovery = SamsungDiscovery(application)
    private val mutableSelected = MutableStateFlow<TvDevice?>(null)
    val selected = mutableSelected.asStateFlow()
    val connection = repository.state
    private val mutableVoiceActive = MutableStateFlow(false)
    val voiceActive = mutableVoiceActive.asStateFlow()
    private var voiceTimeout: Job? = null
    private val mutablePowerMessage = MutableStateFlow<String?>(null)
    val powerMessage = mutablePowerMessage.asStateFlow()
    private var wakeJob: Job? = null

    init {
        viewModelScope.launch {
            connection.collectLatest { state ->
                if (state != ConnectionState.CONNECTED) {
                    stopVoice()
                } else {
                    val device = selected.value ?: return@collectLatest
                    if (device.mac == null) {
                        val refreshed = discovery.refresh(device)
                        if (refreshed.mac != null && selected.value == device) {
                            savedTvs.remember(refreshed)
                            mutableSelected.value = refreshed
                        }
                    }
                }
            }
        }
    }

    fun select(device: TvDevice) {
        stopVoice()
        clearWake()
        val savedMac = savedTvs.devices().firstOrNull { it.id == device.id }?.mac
        val selectedDevice = device.copy(mac = device.mac ?: savedMac)
        mutableSelected.value = selectedDevice
        repository.connect(selectedDevice)
    }

    fun changeTv() {
        stopVoice()
        clearWake()
        repository.disconnect()
        mutableSelected.value = null
    }

    fun reconnect() { selected.value?.let(repository::connect) }

    fun pairAgain() {
        val device = selected.value ?: return
        repository.forgetPairing(device)
        repository.connect(device)
    }

    fun sendKey(key: RemoteKey): Boolean = repository.sendKey(key)

    fun sendText(text: String): Boolean = repository.sendText(text)

    fun powerOff() {
        if (repository.sendKey(RemoteKey.POWER)) {
            mutablePowerMessage.value = "Comando de apagado enviado."
        }
    }

    fun wakeTv(macOverride: String? = null): Boolean {
        val device = selected.value ?: return false
        val mac = WakeOnLan.normalizeMac(macOverride ?: device.mac) ?: return false
        if (wakeJob?.isActive == true) return true
        val target = device.copy(mac = mac)
        if (macOverride != null) {
            savedTvs.remember(target)
            mutableSelected.value = target
        }
        wakeJob = viewModelScope.launch {
            mutablePowerMessage.value = if (WakeOnLan.send(mac, target.address)) {
                "Señal de encendido enviada. Espera a que responda el TV."
            } else {
                "No se pudo enviar la señal de encendido. Revisa la conexión Wi‑Fi."
            }
        }
        return true
    }

    private fun clearWake() {
        wakeJob?.cancel()
        mutablePowerMessage.value = null
    }

    fun toggleVoice() {
        if (voiceActive.value) {
            stopVoice()
        } else if (repository.setVoiceKey(pressed = true)) {
            mutableVoiceActive.value = true
            voiceTimeout = viewModelScope.launch {
                delay(8_000)
                stopVoice()
            }
        }
    }

    fun stopVoice() {
        voiceTimeout?.cancel()
        voiceTimeout = null
        if (!voiceActive.value) return
        mutableVoiceActive.value = false
        if (connection.value == ConnectionState.CONNECTED) repository.setVoiceKey(pressed = false)
    }

    override fun onCleared() {
        stopVoice()
        repository.disconnect()
    }
}
