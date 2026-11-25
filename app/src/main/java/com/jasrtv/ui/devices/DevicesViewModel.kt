package com.jasrtv.ui.devices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasrtv.data.discovery.SamsungDiscovery
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.discovery.isLocalIpv4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<TvDevice> = emptyList(),
    val searching: Boolean = false,
    val searchFailed: Boolean = false
)

class DevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val discovery = SamsungDiscovery(application)
    private val mutableState = MutableStateFlow(DevicesUiState())
    val state = mutableState.asStateFlow()

    init { search() }

    fun search() {
        if (mutableState.value.searching) return
        mutableState.value = mutableState.value.copy(searching = true, searchFailed = false)
        viewModelScope.launch {
            try {
                mutableState.value = DevicesUiState(devices = discovery.search())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(searching = false, searchFailed = true)
            }
        }
    }

    fun deviceByAddress(input: String): TvDevice? {
        val address = input.trim()
        if (!isLocalIpv4(address)) return null
        return state.value.devices.firstOrNull { it.address == address }
            ?: TvDevice(address, address, "Samsung TV")
    }
}
