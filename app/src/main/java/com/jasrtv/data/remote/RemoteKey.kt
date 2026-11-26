package com.jasrtv.data.remote

enum class RemoteKey(internal val code: String) {
    UP("KEY_UP"), DOWN("KEY_DOWN"), LEFT("KEY_LEFT"), RIGHT("KEY_RIGHT"),
    OK("KEY_ENTER"), BACK("KEY_RETURN"), HOME("KEY_HOME"), VOICE("KEY_BT_VOICE"),
    POWER("KEY_POWER"), VOLUME_UP("KEY_VOLUP"), VOLUME_DOWN("KEY_VOLDOWN"), MUTE("KEY_MUTE"),
    CHANNEL_UP("KEY_CHUP"), CHANNEL_DOWN("KEY_CHDOWN")
}
