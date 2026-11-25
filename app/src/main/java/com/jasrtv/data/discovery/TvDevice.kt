package com.jasrtv.data.discovery

import java.net.InetAddress

data class TvDevice(val id: String, val address: String, val name: String, val model: String = "")

internal fun isLocalIpv4(address: String): Boolean {
    val parts = address.split('.')
    if (parts.size != 4 || parts.any { part ->
            part.isEmpty() || part.length > 3 || part.any { it !in '0'..'9' } ||
                (part.length > 1 && part.startsWith('0')) || part.toInt() > 255
        }) return false
    val ip = InetAddress.getByName(address)
    return ip.isSiteLocalAddress || ip.isLinkLocalAddress
}
