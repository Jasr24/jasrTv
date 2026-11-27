package com.jasrtv.data.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.jasrtv.data.power.WakeOnLan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class SamsungDiscovery(context: Context) {
    private val wifi = context.getSystemService(WifiManager::class.java)
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val http = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .callTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun search(remembered: List<TvDevice>): List<TvDevice> = withContext(Dispatchers.IO) {
        val network = wifiNetwork()
        val candidates = try {
            findCandidates(network)
        } catch (error: java.io.IOException) {
            if (network == null) throw error
            emptyMap()
        }
        val localHttp = httpFor(network)
        val discovered = coroutineScope {
            candidates.map { (address, samsungService) ->
                async { readDevice(address, samsungService, localHttp) }
            }.awaitAll()
        }.filterNotNull()
        val missing = remembered.map(TvDevice::id).filterNot(::isLocalIpv4).toMutableSet()
        missing.removeAll(discovered.map(TvDevice::id).toSet())
        val recovered = if (network == null || missing.isEmpty()) emptyList()
            else recoverSavedTvs(network, localHttp, missing, candidates.keys)
        (discovered + recovered).distinctBy(TvDevice::id).sortedBy(TvDevice::name)
    }

    private suspend fun recoverSavedTvs(
        network: Network,
        localHttp: OkHttpClient,
        missing: MutableSet<String>,
        checked: Set<String>
    ): List<TvDevice> = coroutineScope {
        val link = connectivity?.getLinkProperties(network)?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address && isLocalIpv4(it.address.hostAddress.orEmpty()) }
            ?: return@coroutineScope emptyList()
        // Limit recovery to the phone's own IPv4 subnet, capped at a /24.
        val prefix = maxOf(24, link.prefixLength)
        if (prefix > 30) return@coroutineScope emptyList()
        val bytes = link.address.address.map { it.toInt() and 0xFF }
        val blockSize = 1 shl (32 - prefix)
        val first = bytes[3] / blockSize * blockSize
        val base = bytes.take(3).joinToString(".")
        val candidates = (first + 1 until first + blockSize - 1).asSequence()
            .filter { it != bytes[3] }.map { "$base.$it" }.filterNot { it in checked }
        val probeHttp = localHttp.newBuilder().callTimeout(450, TimeUnit.MILLISECONDS).build()
        val recovered = mutableListOf<TvDevice>()
        for (batch in candidates.chunked(24)) {
            if (missing.isEmpty()) break
            currentCoroutineContext().ensureActive()
            val matches = batch.map { address ->
                async { readDevice(address, samsungService = false, localHttp = probeHttp) }
            }.awaitAll().filterNotNull()
            recovered += matches.filter { missing.remove(it.id) }
        }
        recovered
    }

    suspend fun reachableAddresses(devices: List<TvDevice>): Set<String> = withContext(Dispatchers.IO) {
        val network = wifiNetwork()
        coroutineScope {
            devices.map { it.address }.distinct().map { address ->
                async {
                    val responds = runCatching {
                        Socket(Proxy.NO_PROXY).use { socket ->
                            network?.bindSocket(socket)
                            socket.connect(InetSocketAddress(address, 8002), 1_500)
                        }
                    }.isSuccess
                    address.takeIf { responds }
                }
            }.awaitAll().filterNotNull().toSet()
        }
    }

    suspend fun refresh(device: TvDevice): TvDevice = withContext(Dispatchers.IO) {
        val info = readDevice(device.address, samsungService = false, httpFor(wifiNetwork()))
        device.copy(mac = info?.mac ?: device.mac)
    }

    fun wifiChanges() = callbackFlow {
        val manager = connectivity
        if (manager == null) {
            close()
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val validated = mutableMapOf<Network, Boolean>()

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                trySend(Unit)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val available = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val previous = validated.put(network, available)
                if (previous != null && previous != available) trySend(Unit)
            }

            override fun onLost(network: Network) {
                validated.remove(network)
                trySend(Unit)
            }
        }
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        manager.registerNetworkCallback(request, callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate()

    private fun wifiNetwork(): Network? = localWifiNetwork(connectivity)

    private fun httpFor(network: Network?): OkHttpClient =
        if (network == null) http else http.newBuilder().socketFactory(network.socketFactory).build()

    private suspend fun findCandidates(network: Network?): Map<String, Boolean> {
        val candidates = linkedMapOf<String, Boolean>()
        val lock = wifi?.createMulticastLock("jasrTv-discovery")
        lock?.acquire()
        try {
            DatagramSocket().use { socket ->
                network?.bindSocket(socket)
                socket.soTimeout = 250
                val group = InetAddress.getByName("239.255.255.250")
                val services = listOf(
                    "urn:samsung.com:device:RemoteControlReceiver:1",
                    "urn:samsung.com:service:MainTVAgent2:1",
                    "urn:schemas-upnp-org:device:MediaRenderer:1",
                    "urn:schemas-upnp-org:service:RenderingControl:1"
                )
                val requests = services.map { service ->
                    val request = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $service\r\n\r\n")
                        .toByteArray(Charsets.US_ASCII)
                    DatagramPacket(request, request.size, group, 1900)
                }
                val start = SystemClock.elapsedRealtime()
                val deadline = start + 3_800
                var nextProbe = start
                val buffer = ByteArray(4096)
                while (SystemClock.elapsedRealtime() < deadline && candidates.size < 32) {
                    currentCoroutineContext().ensureActive()
                    if (SystemClock.elapsedRealtime() >= nextProbe) {
                        requests.forEach(socket::send)
                        nextProbe = if (nextProbe == start) start + 1_250 else Long.MAX_VALUE
                    }
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

    private fun readDevice(address: String, samsungService: Boolean, localHttp: OkHttpClient): TvDevice? {
        val info = runCatching {
            val request = Request.Builder().url("http://$address:8001/api/v2/").build()
            localHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                JSONObject(response.peekBody(64 * 1024).string())
            }
        }.getOrNull()
        val details = info?.optJSONObject("device")
        if (!samsungService &&
            !info?.optString("type").equals("samsung.tv", ignoreCase = true) &&
            details?.optString("type")?.contains("samsung", ignoreCase = true) != true) return null
        val mac = WakeOnLan.normalizeMac(details?.optString("networkMac"))
            ?: WakeOnLan.normalizeMac(details?.optString("mac"))
            ?: if (details?.optString("networkType").equals("wired", ignoreCase = true)) {
                WakeOnLan.normalizeMac(details?.optString("ethernetMac"))
            } else {
                WakeOnLan.normalizeMac(details?.optString("wifiMac"))
            }
        return TvDevice(
            id = details?.optString("id").orEmpty().ifBlank { address },
            address = address,
            name = details?.optString("name").orEmpty().ifBlank { "Samsung TV" },
            model = details?.optString("modelName").orEmpty(),
            mac = mac
        )
    }
}

@Suppress("DEPRECATION") // Wi-Fi can be available while mobile data is the default.
internal fun localWifiNetwork(manager: ConnectivityManager?): Network? {
    if (manager == null) return null
    fun isWifi(network: Network) = manager.getNetworkCapabilities(network)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    return manager.activeNetwork?.takeIf(::isWifi) ?: manager.allNetworks.firstOrNull(::isWifi)
}
