package com.jasrtv.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class SamsungDiscovery(context: Context) {
    private val wifi = context.getSystemService(WifiManager::class.java)
    private val http = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .callTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun search(): List<TvDevice> = withContext(Dispatchers.IO) {
        val candidates = findCandidates()
        coroutineScope {
            candidates.map { (address, samsungService) ->
                async { readDevice(address, samsungService) }
            }.awaitAll()
        }.filterNotNull().distinctBy(TvDevice::id).sortedBy(TvDevice::name)
    }

    private suspend fun findCandidates(): Map<String, Boolean> {
        val candidates = linkedMapOf<String, Boolean>()
        val lock = wifi?.createMulticastLock("jasrTv-discovery")
        lock?.acquire()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 250
                val group = InetAddress.getByName("239.255.255.250")
                val services = listOf(
                    "urn:samsung.com:device:RemoteControlReceiver:1",
                    "urn:samsung.com:service:MainTVAgent2:1",
                    "urn:schemas-upnp-org:device:MediaRenderer:1",
                    "urn:schemas-upnp-org:service:RenderingControl:1"
                )
                for (service in services) {
                    val request = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $service\r\n\r\n")
                        .toByteArray(Charsets.US_ASCII)
                    socket.send(DatagramPacket(request, request.size, group, 1900))
                }
                val deadline = SystemClock.elapsedRealtime() + 2_800
                val buffer = ByteArray(4096)
                while (SystemClock.elapsedRealtime() < deadline && candidates.size < 32) {
                    currentCoroutineContext().ensureActive()
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val response = String(packet.data, packet.offset, packet.length, Charsets.US_ASCII)
                    val address = packet.address.hostAddress.orEmpty()
                    if (!response.startsWith("HTTP/1.1 200", ignoreCase = true) || !isLocalIpv4(address)) continue
                    val target = response.lineSequence().firstOrNull { it.startsWith("ST:", ignoreCase = true) }
                        ?.substringAfter(':')?.trim()
                    if (services.any { it.equals(target, ignoreCase = true) }) {
                        val samsungService = target.orEmpty().startsWith("urn:samsung.com:", ignoreCase = true)
                        candidates[address] = candidates[address] == true || samsungService
                    }
                }
            }
        } finally {
            lock?.release()
        }
        return candidates
    }

    private fun readDevice(address: String, samsungService: Boolean): TvDevice? {
        val info = runCatching {
            val request = Request.Builder().url("http://$address:8001/api/v2/").build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                JSONObject(response.peekBody(64 * 1024).string())
            }
        }.getOrNull()
        val details = info?.optJSONObject("device")
        if (!samsungService &&
            !info?.optString("type").equals("samsung.tv", ignoreCase = true) &&
            details?.optString("type")?.contains("samsung", ignoreCase = true) != true) return null
        return TvDevice(
            id = details?.optString("id").orEmpty().ifBlank { address },
            address = address,
            name = details?.optString("name").orEmpty().ifBlank { "Samsung TV" },
            model = details?.optString("modelName").orEmpty()
        )
    }
}
