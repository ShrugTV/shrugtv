package com.shrug.shrugtv.hub

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonParser
import com.shrug.shrugtv.R
import com.shrug.shrugtv.activities.PlayerActivity
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie

/**
 * Commands from Home Assistant (MQTT) and the phone companion, by name and text payload
 * (docs/mqtt_ha_plan.md section 4). Validates the input, runs it on the main thread against the open
 * player; without a player only "watch" (starts the player) and "notify" (a toast) do something.
 */
object HubCommandRouter {
    private const val TAG = "HubCommands"
    private val main = Handler(Looper.getMainLooper())

    /** Names the integrations may send. */
    val NAMES = setOf(
        "play_pause", "play", "pause", "live", "next", "previous", "close", "watch", "quality",
        "chat", "chat_layout", "volume", "chat_message", "notify", "replay", "audio_only"
    )

    /** Parsed command, or null if invalid. Pure: testable without Android. */
    sealed class Command {
        object PlayPause : Command()
        object Play : Command()
        object Pause : Command()
        object Live : Command()
        data class Zap(val direction: Int) : Command()
        object Close : Command()
        object Replay : Command()
        data class AudioOnly(val on: Boolean) : Command()
        data class WatchLogin(val login: String) : Command()
        data class WatchVod(val videoId: Long, val positionSeconds: Int?) : Command()
        data class Quality(val name: String) : Command()
        data class Chat(val visible: Boolean) : Command()
        data class ChatLayout(val value: String) : Command()
        data class Volume(val percent: Int) : Command()
        data class ChatMessage(val text: String) : Command()
        data class Notice(val title: String, val text: String?, val imageUrl: String?, val seconds: Int) : Command()
    }

    private val LOGIN = Regex("^[A-Za-z0-9_]{1,25}$")

    @JvmStatic
    fun parse(name: String, payload: String): Command? {
        val p = payload.trim()
        return when (name) {
            "play_pause" -> Command.PlayPause
            "play" -> Command.Play
            "pause" -> Command.Pause
            "live" -> Command.Live
            "next" -> Command.Zap(1)
            "previous" -> Command.Zap(-1)
            "close" -> Command.Close
            "replay" -> Command.Replay
            "audio_only" -> when (p.uppercase()) {
                "ON", "TRUE", "1" -> Command.AudioOnly(true)
                "OFF", "FALSE", "0" -> Command.AudioOnly(false)
                else -> null
            }
            "watch" -> parseWatch(p)
            "quality" -> p.takeIf { it.isNotEmpty() && it.length <= 40 }?.let { Command.Quality(it) }
            "chat" -> when (p.uppercase()) {
                "ON", "TRUE", "1" -> Command.Chat(true)
                "OFF", "FALSE", "0" -> Command.Chat(false)
                else -> null
            }
            "chat_layout" -> p.takeIf { it in setOf("side_by_side", "overlay", "transparent") }?.let { Command.ChatLayout(it) }
            "volume" -> p.toFloatOrNull()?.let { Command.Volume(it.toInt().coerceIn(0, 100)) }
            "chat_message" -> p.takeIf { it.isNotEmpty() }?.let { Command.ChatMessage(it.take(500)) }
            "notify" -> parseNotice(p)
            else -> null
        }
    }

    private fun parseWatch(p: String): Command? {
        if (p.startsWith("{")) {
            return try {
                val o = JsonParser.parseString(p).asJsonObject
                val vod = o.get("vod_id")?.takeIf { !it.isJsonNull }?.asLong
                if (vod != null && vod > 0) {
                    Command.WatchVod(vod, o.get("position_s")?.takeIf { !it.isJsonNull }?.asInt)
                } else {
                    o.get("login")?.takeIf { !it.isJsonNull }?.asString?.trim()?.removePrefix("@")
                        ?.takeIf { LOGIN.matches(it) }?.let { Command.WatchLogin(it.lowercase()) }
                }
            } catch (e: Exception) {
                null
            }
        }
        val login = p.removePrefix("@")
        return if (LOGIN.matches(login)) Command.WatchLogin(login.lowercase()) else null
    }

    private fun parseNotice(p: String): Command? {
        if (!p.startsWith("{")) return p.takeIf { it.isNotEmpty() }?.let { Command.Notice(it.take(80), null, null, 8) }
        return try {
            val o = JsonParser.parseString(p).asJsonObject
            val title = o.get("title")?.takeIf { !it.isJsonNull }?.asString ?: return null
            Command.Notice(
                title.take(80),
                o.get("text")?.takeIf { !it.isJsonNull }?.asString?.take(200),
                o.get("image_url")?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.startsWith("http") },
                (o.get("seconds")?.takeIf { !it.isJsonNull }?.asInt ?: 8).coerceIn(2, 60)
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Parses and runs [name]/[payload] on the main thread. */
    @JvmStatic
    fun dispatch(context: Context, name: String, payload: String) {
        val cmd = parse(name, payload)
        if (cmd == null) {
            Log.w(TAG, "Ignored command $name")
            return
        }
        main.post { run(context.applicationContext, cmd) }
    }

    private fun run(context: Context, cmd: Command) {
        val player = PlaybackStateHub.commands
        if (player == null) {
            // No player: start one, or show a notice as a toast; everything else needs a player
            when (cmd) {
                is Command.WatchLogin -> startPlayer(context, Movie().apply {
                    login = cmd.login
                    displayName = cmd.login
                    itemType = Enums.ContentType.STREAM
                })
                is Command.WatchVod -> startPlayer(context, Movie().apply {
                    setVideoIdFromString(cmd.videoId.toString())
                    itemType = Enums.ContentType.VOD
                })
                is Command.Notice -> Toast.makeText(context, listOfNotNull(cmd.title, cmd.text).joinToString(": "), Toast.LENGTH_LONG).show()
                else -> Log.d(TAG, "No player open for $cmd")
            }
            return
        }
        when (cmd) {
            Command.PlayPause -> player.playPause()
            Command.Play -> player.play()
            Command.Pause -> player.pause()
            Command.Live -> player.jumpToLive()
            is Command.Zap -> player.zap(cmd.direction)
            Command.Close -> player.close()
            Command.Replay -> player.replay()
            is Command.AudioOnly -> player.setAudioOnly(cmd.on)
            is Command.WatchLogin -> player.watchLogin(cmd.login)
            is Command.WatchVod -> player.watchVod(cmd.videoId, cmd.positionSeconds)
            is Command.Quality -> player.selectQualityByName(cmd.name)
            is Command.Chat -> player.setChatVisible(cmd.visible)
            is Command.ChatLayout -> player.setChatLayout(cmd.value)
            is Command.Volume -> player.setVolume(cmd.percent)
            is Command.ChatMessage -> player.sendChat(cmd.text)
            is Command.Notice -> player.showNotice(cmd.title, cmd.text, cmd.imageUrl, cmd.seconds)
        }
    }

    /** Works while ShrugTV is in the foreground; Android blocks activity starts from the background. */
    private fun startPlayer(context: Context, movie: Movie) {
        try {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(context.getString(R.string.movie), movie)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cannot start the player: ${e.message}")
        }
    }
}
