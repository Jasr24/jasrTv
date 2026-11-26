package com.jasrtv.ui.devices

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasrtv.data.discovery.PairedTvStore
import com.jasrtv.data.discovery.SamsungDiscovery
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.discovery.isLocalIpv4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<TvDevice> = emptyList(),
    val remembered: List<TvDevice> = emptyList(),
    val reachable: Set<String>? = null,
    val searching: Boolean = false,
    val searchFailed: Boolean = false
)

class DevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val discovery = SamsungDiscovery(application)
    private val pairedTvs = PairedTvStore(application)
    private val mutableState = MutableStateFlow(DevicesUiState())
    private var reachabilityCheck: Job? = null
    val state = mutableState.asStateFlow()

    init { search() }

    fun refreshRemembered() {
        val remembered = pairedTvs.devices()
        val found = mutableState.value.devices.associateBy(TvDevice::id)
        mutableState.value = mutableState.value.copy(remembered = remembered, reachable = null)
        reachabilityCheck?.cancel()
        reachabilityCheck = viewModelScope.launch {
            val reachable = discovery.reachableAddresses(remembered.map { found[it.id] ?: it })
            mutableState.value = mutableState.value.copy(reachable = reachable)
        }
    }

    fun search() {
        if (mutableState.value.searching) return
        mutableState.value = mutableState.value.copy(searching = true, searchFailed = false, devices = emptyList())
        refreshRemembered()
        viewModelScope.launch {
            try {
                val found = discovery.search()
                mutableState.value = mutableState.value.copy(devices = found, searching = false)
                refreshRemembered()
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
        return (state.value.devices + pairedTvs.devices()).firstOrNull { it.address == address }
            ?: TvDevice(address, address, "Samsung TV")
    }
}
