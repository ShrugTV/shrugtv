package com.shrug.shrugtv.chat

import android.text.TextUtils

object KnownBots {
    private val defaultBots = listOf("Nightbot", "Moobot", "StreamElements", "CalvinDotEXE", "Streamlabs")

    @JvmStatic
    var bots: ArrayList<String> = ArrayList(defaultBots)

    @JvmStatic
    fun getBotsJoined(): String = if (bots.isNotEmpty()) TextUtils.join(", ", bots) else ""

    @JvmStatic
    fun init() {
        bots = ArrayList(defaultBots)
    }
}
