package com.jasrtv.data.remote

enum class StreamingApp(val label: String, internal val appIds: List<String>) {
    NETFLIX("Netflix", listOf("3201907018807", "11101200001", "org.tizen.netflix-app")),
    CRUNCHYROLL("Crunchyroll", listOf("3202302030097")),
    DISNEY_PLUS("Disney+", listOf("3201901017640", "3202009021709", "3202204027038")),
    PRIME_VIDEO("Prime Video", listOf("3201910019365", "3201512006785", "org.tizen.ignition"))
}
