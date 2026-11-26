package com.jasrtv.data.power

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.NetworkInterface

internal object WakeOnLan {
    fun normalizeMac(value: String?): String? {
        val mac = value.orEmpty().trim().replace(":", "").replace("-", "").uppercase()
        if (mac.length != 12 || mac.any { it !in '0'..'9' && it !in 'A'..'F' }) return null
        if (mac == "000000000000" || (mac.substring(0, 2).toInt(16) and 1) != 0) return null
        return mac
    }

    suspend fun send(mac: String, address: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizeMac(mac) ?: return@withContext false
        val bytes = normalized.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val packet = ByteArray(102) { index ->
            if (index < 6) 0xFF.toByte() else bytes[(index - 6) % bytes.size]
        }
        val target = InetAddress.getByName(address)
        val directed = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().asSequence()
                .flatMap { it.interfaceAddresses.asSequence() }
                .firstOrNull { entry ->
                    entry.address is Inet4Address && entry.broadcast != null &&
                        sameSubnet(entry.address.address, target.address, entry.networkPrefixLength.toInt())
                }?.broadcast
        }.getOrNull()
        val destinations = listOfNotNull(directed, InetAddress.getByName("255.255.255.255")).distinct()
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                destinations.map { destination ->
                    runCatching { socket.send(DatagramPacket(packet, packet.size, destination, 9)) }.isSuccess
                }.any { it }
            }
        }.getOrDefault(false)
    }

    private fun sameSubnet(first: ByteArray, second: ByteArray, prefix: Int): Boolean {
        if (first.size != 4 || second.size != 4 || prefix !in 1..31) return false
        val mask = -1 shl (32 - prefix)
        return ((java.nio.ByteBuffer.wrap(first).int xor java.nio.ByteBuffer.wrap(second).int) and mask) == 0
    }
}
