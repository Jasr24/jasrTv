package com.jasrtv.data.remote

import android.util.Base64
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.Proxy
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal class SamsungTvClient {
    interface Listener {
        fun onWaiting()
        fun onConnected(token: String?, fingerprint: String)
        fun onDisconnected(state: ConnectionState)
    }

    fun connect(address: String, token: String?, fingerprint: String?, listener: Listener): WebSocket {
        val trust = TvCertificateTrust(fingerprint)
        val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
        val http = OkHttpClient.Builder()
            .sslSocketFactory(tls.socketFactory, trust)
            .hostnameVerifier { host, _ -> host == address }
            .proxy(Proxy.NO_PROXY)
            .followRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
        val url = HttpUrl.Builder().scheme("https").host(address).port(8002)
            .addPathSegments("api/v2/channels/samsung.remote.control")
            .addQueryParameter("name", encode("jasrTv"))
            .apply { if (token != null) addQueryParameter("token", token) }
            .build()
        return http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = listener.onWaiting()

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (message.optString("event")) {
                    "ms.channel.connect" -> {
                        val pin = trust.presentedFingerprint ?: return
                        val newToken = message.optJSONObject("data")?.optString("token")
                            ?.takeIf { it.isNotBlank() && it != "null" }
                        listener.onConnected(newToken, pin)
                    }
                    "ms.channel.unauthorized" -> listener.onDisconnected(ConnectionState.DENIED)
                    "ms.channel.timeOut" -> listener.onDisconnected(ConnectionState.TIMED_OUT)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onDisconnected(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onDisconnected(
                    when {
                        trust.changed -> ConnectionState.CERTIFICATE_CHANGED
                        response?.code in listOf(401, 403) -> ConnectionState.DENIED
                        else -> ConnectionState.UNREACHABLE
                    }
                )
            }
        })
    }

    fun sendKey(socket: WebSocket, key: RemoteKey): Boolean =
        send(socket, "ms.remote.control", JSONObject()
            .put("Cmd", "Click").put("DataOfCmd", key.code)
            .put("Option", "false").put("TypeOfRemote", "SendRemoteKey"))

    fun sendText(socket: WebSocket, text: String): Boolean =
        send(socket, "ms.channel.emit", JSONObject()
            .put("event", "custom.remote.textReceived").put("to", "broadcast")) &&
            send(socket, "ms.remote.control", JSONObject()
                .put("Cmd", encode(text)).put("DataOfCmd", "base64")
                .put("TypeOfRemote", "SendInputString"))

    private fun send(socket: WebSocket, method: String, params: JSONObject): Boolean =
        socket.send(JSONObject().put("method", method).put("params", params).toString())

    private fun encode(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}

// Many Samsung TVs use a self-signed certificate. Remember it after TV authorization.
private class TvCertificateTrust(private val expected: String?) : X509TrustManager {
    @Volatile var presentedFingerprint: String? = null
        private set
    @Volatile var changed: Boolean = false
        private set

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val certificate = chain.firstOrNull() ?: throw CertificateException("Missing TV certificate")
        val digest = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        val fingerprint = Base64.encodeToString(digest, Base64.NO_WRAP)
        if (expected != null && expected != fingerprint) {
            changed = true
            throw CertificateException("TV certificate changed")
        }
        presentedFingerprint = fingerprint
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        throw CertificateException("Client certificates are unsupported")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
