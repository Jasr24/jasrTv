package com.jasrtv.data.discovery

import android.content.Context
import org.json.JSONObject

class PairedTvStore(context: Context) {
    private val prefs = context.getSharedPreferences("paired_tvs", Context.MODE_PRIVATE)

    fun remember(device: TvDevice) {
        val entry = JSONObject().put("address", device.address)
            .put("name", device.name).put("model", device.model)
        prefs.edit().putString(device.id, entry.toString()).apply()
    }

    fun devices(): List<TvDevice> = prefs.all.mapNotNull { (id, raw) ->
        val entry = runCatching { JSONObject(raw as String) }.getOrNull() ?: return@mapNotNull null
        val address = entry.optString("address")
        if (!isLocalIpv4(address)) return@mapNotNull null
        TvDevice(id, address, entry.optString("name", "Samsung TV"), entry.optString("model"))
    }.sortedBy(TvDevice::name)
}
