package com.jasrtv.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.jasrtv.data.discovery.PairedTvStore
import com.jasrtv.data.discovery.TvDevice
import com.jasrtv.data.discovery.isLocalIpv4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.WebSocket

class TvRemoteRepository(context: Context) {
    private val pairedTvs = PairedTvStore(context)
    private val prefs = context.getSharedPreferences("tv_pairings", Context.MODE_PRIVATE)
    private val client = SamsungTvClient()
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state = mutableState.asStateFlow()
    private var socket: WebSocket? = null
    private var installedAppIds: Set<String> = emptySet()
    private var authorizedAddress: String? = null
    private var authorizedFingerprint: String? = null
    private var generation = 0

    // Public operations and state changes run on the main thread.
    fun connect(tv: TvDevice) {
        disconnect()
        if (!isLocalIpv4(tv.address)) {
            mutableState.value = ConnectionState.UNREACHABLE
            return
        }
        val attempt = generation
        val pinKey = "pin:${tv.id}"
        val tokenKey = "token:${tv.id}"
        mutableState.value = ConnectionState.CONNECTING
        val listener = object : SamsungTvClient.Listener {
            override fun onWaiting() = forAttempt(attempt) {
                mutableState.value = ConnectionState.WAITING_FOR_TV
            }

            override fun onConnected(token: String?, fingerprint: String) = forAttempt(attempt) {
                handler.removeCallbacksAndMessages(null)
                prefs.edit().putString(pinKey, fingerprint).apply {
                    if (token != null) putString(tokenKey, token)
                }.apply()
                pairedTvs.remember(tv)
                authorizedAddress = tv.address
                authorizedFingerprint = fingerprint
                mutableState.value = ConnectionState.CONNECTED
                socket?.let(client::requestInstalledApps)
            }

            override fun onInstalledApps(appIds: Set<String>) = forAttempt(attempt) {
                installedAppIds = appIds
            }

            override fun onDisconnected(state: ConnectionState) = forAttempt(attempt) {
                if (state == ConnectionState.DENIED) prefs.edit().remove(tokenKey).apply()
                disconnect(state)
            }
        }
        socket = client.connect(tv.address, prefs.getString(tokenKey, null),
            prefs.getString(pinKey, null), listener)
        handler.postDelayed({ if (generation == attempt) disconnect(ConnectionState.TIMED_OUT) }, 30_000)
    }

    fun forgetPairing(tv: TvDevice) {
        disconnect()
        prefs.edit().remove("pin:${tv.id}").remove("token:${tv.id}").apply()
    }

    fun disconnect() = disconnect(ConnectionState.DISCONNECTED)

    fun sendKey(key: RemoteKey): Boolean = send { client.sendKey(it, key) }

    fun setVoiceKey(pressed: Boolean): Boolean =
        send { client.sendKey(it, RemoteKey.VOICE, if (pressed) "Press" else "Release") }

    fun sendText(text: String): Boolean = send { client.sendText(it, text) }

    suspend fun launchApp(app: StreamingApp): Boolean {
        val current = socket ?: return false
        val address = authorizedAddress ?: return false
        val fingerprint = authorizedFingerprint ?: return false
        if (mutableState.value != ConnectionState.CONNECTED) return false
        val id = app.appIds.firstOrNull { it in installedAppIds } ?: app.appIds.first()
        val candidates = (listOf(id) + app.appIds).distinct()
        val accepted = client.launchAppViaRest(address, fingerprint, candidates)
        if (socket !== current) return false
        return accepted || send { client.launchApp(it, id) }
    }

    private fun send(action: (WebSocket) -> Boolean): Boolean {
        val current = socket ?: return false
        if (mutableState.value != ConnectionState.CONNECTED) return false
        if (action(current)) return true
        disconnect()
        return false
    }

    private fun disconnect(nextState: ConnectionState) {
        generation++
        handler.removeCallbacksAndMessages(null)
        socket?.cancel()
        socket = null
        installedAppIds = emptySet()
        authorizedAddress = null
        authorizedFingerprint = null
        mutableState.value = nextState
    }

    private fun forAttempt(attempt: Int, action: () -> Unit) {
        handler.post { if (generation == attempt) action() }
    }
}
