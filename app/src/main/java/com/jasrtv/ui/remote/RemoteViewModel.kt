package com.jasrtv.ui.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.remote.RemoteKey
import com.jasrtv.data.remote.TvRemoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TvRemoteRepository(application)
    private val mutableSelected = MutableStateFlow<TvDevice?>(null)
    val selected = mutableSelected.asStateFlow()
    val connection = repository.state

    fun select(device: TvDevice) {
        repository.connect(device)
        mutableSelected.value = device
    }

    fun changeTv() {
        repository.disconnect()
        mutableSelected.value = null
    }

    fun reconnect() { selected.value?.let(repository::connect) }

    fun pairAgain() {
        val device = selected.value ?: return
        repository.forgetPairing(device)
        repository.connect(device)
    }

    fun sendKey(key: RemoteKey) { repository.sendKey(key) }

    fun sendText(text: String): Boolean = repository.sendText(text)

    override fun onCleared() = repository.disconnect()
}
