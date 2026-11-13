package com.shrug.shrugtv.chat.emote

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Applies channel emotes that were added, renamed or removed during the stream
 * (modernised in Kotlin for stage 2 #108).
 */
class EmoteLiveUpdates @JvmOverloads constructor(
    private val emotes: EmoteManager,
    private val channelId: Int,
    /** 7TV paints, badges and personal emote sets of the chatters; null = switched off. */
    private val cosmetics: com.shrug.shrugtv.chat.seventv.SevenTvCosmetics? = null
) {
    companion object {
        private const val TAG = "EmoteLive"
        private const val SEVEN_TV = "wss://events.7tv.io/v3"
        private const val BTTV = "wss://sockets.betterttv.net/ws"

        private fun fields(body: JsonObject?, kind: String): List<JsonObject> {
            val out = ArrayList<JsonObject>()
            if (body == null || !body.has(kind) || !body.get(kind).isJsonArray) return out
            for (e in body.getAsJsonArray(kind)) {
                if (!e.isJsonObject) continue
                val f = e.asJsonObject
                if (f.has("key") && "emotes" == f.get("key").asString) {
                    out.add(f)
                }
            }
            return out
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()

    private val tick: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Volatile
    private var running = false

    @Volatile
    private var sevenTv: WebSocket? = null

    @Volatile
    private var bttv: WebSocket? = null

    private var sevenTvAttempts = 0
    private var bttvAttempts = 0

    fun start() {
        running = true
        connectSevenTv()
        connectBttv()
        tick.scheduleWithFixedDelay({
            try {
                emotes.ffzReload()
            } catch (e: Exception) {
                Log.w(TAG, "Reload FFZ", e)
            }
        }, 5, 5, TimeUnit.MINUTES)
    }

    fun stop() {
        running = false
        tick.shutdownNow()
        sevenTv?.close(1000, null)
        bttv?.close(1000, null)
        client.dispatcher.executorService.shutdown()
    }

    private fun later(r: Runnable, attempt: Int) {
        if (!running || tick.isShutdown) return
        val seconds = (5L shl attempt.coerceAtMost(5)).coerceAtMost(120)
        tick.schedule(r, seconds, TimeUnit.SECONDS)
    }

    // ------------------------------------------------------------------------------------- 7TV

    private fun connectSevenTv() {
        // Cosmetics need the socket even when the channel has no 7TV emote set
        if (!running || (emotes.sevenTvEmoteSetId == null && cosmetics == null)) {
            Log.d(TAG, "7TV: no emote set for channel $channelId")
            return
        }
        sevenTv = client.newWebSocket(Request.Builder().url(SEVEN_TV).build(), object : WebSocketListener() {
            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    sevenTvMessage(ws, JsonParser().parse(text).asJsonObject)
                } catch (e: Exception) {
                    Log.w(TAG, "7TV-Nachricht", e)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (ws === sevenTv) later(Runnable { connectSevenTv() }, sevenTvAttempts++)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.d(TAG, "7TV getrennt: $t")
                if (ws === sevenTv) later(Runnable { connectSevenTv() }, sevenTvAttempts++)
            }
        })
    }

    /** Subscription with the channel as context: cosmetics and entitlements of its chatters. */
    private fun subscribeChannel(ws: WebSocket, type: String) {
        val condition = JsonObject().apply {
            addProperty("ctx", "channel")
            addProperty("platform", "TWITCH")
            addProperty("id", channelId.toString())
        }
        ws.send(JsonObject().apply {
            addProperty("op", 35)
            add("d", JsonObject().apply { addProperty("type", type); add("condition", condition) })
        }.toString())
    }

    private fun subscribe(ws: WebSocket, type: String, objectId: String?) {
        if (objectId == null) return
        val condition = JsonObject().apply { addProperty("object_id", objectId) }
        val d = JsonObject().apply {
            addProperty("type", type)
            add("condition", condition)
        }
        val message = JsonObject().apply {
            addProperty("op", 35)
            add("d", d)
        }
        ws.send(message.toString())
    }

    private fun sevenTvMessage(ws: WebSocket, n: JsonObject) {
        when (n.get("op").asInt) {
            1 -> { // Hello
                sevenTvAttempts = 0
                subscribe(ws, "emote_set.update", emotes.sevenTvEmoteSetId)
                subscribe(ws, "user.update", emotes.sevenTvUserId)
                if (cosmetics != null) {
                    subscribeChannel(ws, "cosmetic.*")
                    subscribeChannel(ws, "entitlement.*")
                    subscribeChannel(ws, "emote_set.*")
                }
                Log.i(TAG, "7TV verbunden, Set ${emotes.sevenTvEmoteSetId}")
            }
            4 -> { // Reconnect
                ws.close(1000, null)
            }
            5 -> { // Ack
                Log.d(TAG, "7TV Ack: ${n.get("d")}")
            }
            0 -> { // Dispatch
                sevenTvDispatch(n.getAsJsonObject("d"))
            }
        }
    }

    /** A 7TV dispatch; without a websocket so that it is testable. */
    fun sevenTvDispatch(d: JsonObject) {
        val type = d.get("type").asString
        val body = d.getAsJsonObject("body")
        val c = cosmetics
        when (type) {
            "cosmetic.create" -> { c?.onCosmetic(body?.getAsJsonObject("object")); return }
            "entitlement.create", "entitlement.delete" -> {
                val personal = c?.onEntitlement(body?.getAsJsonObject("object"), type == "entitlement.create")
                if (personal != null && !emotes.hasPersonalSet(personal.second)) {
                    tick.execute { emotes.loadPersonalSet(personal.second) }
                }
                return
            }
        }
        // A personal set changed (not the channel's own set): load it again
        val setId = body?.get("id")?.takeIf { it.isJsonPrimitive }?.asString
        val channelSet = emotes.sevenTvEmoteSetId
        if (type.startsWith("emote_set.") && setId != null &&
            ((channelSet != null && setId != channelSet) || emotes.hasPersonalSet(setId))) {
            // Never apply another set's changes to the channel's emotes
            if (emotes.hasPersonalSet(setId)) tick.execute { emotes.loadPersonalSet(setId) }
            return
        }
        if ("emote_set.update" == type) {
            for (f in fields(body, "pushed")) {
                if (f.has("value") && f.get("value").isJsonObject) {
                    Log.i(TAG, "7TV new: ${emotes.sevenTvEmoteSet(f.getAsJsonObject("value"), null)}")
                }
            }
            for (f in fields(body, "pulled")) {
                if (f.has("old_value") && f.get("old_value").isJsonObject) {
                    val name = f.getAsJsonObject("old_value").get("name").asString
                    emotes.sevenTvEmoteRemove(name)
                    Log.i(TAG, "7TV weg: $name")
                }
            }
            for (f in fields(body, "updated")) {
                if (f.has("value") && f.get("value").isJsonObject && f.has("old_value") && f.get("old_value").isJsonObject) {
                    val old = f.getAsJsonObject("old_value").get("name").asString
                    Log.i(TAG, "7TV renamed: $old -> ${emotes.sevenTvEmoteSet(f.getAsJsonObject("value"), old)}")
                }
            }
        } else if ("user.update" == type) {
            Log.i(TAG, "7TV user changed, reloading set")
            tick.execute {
                emotes.sevenTvReload()
                val old = sevenTv
                sevenTv = null
                old?.close(1000, null)
                connectSevenTv()
            }
        }
    }

    // ------------------------------------------------------------------------------------ BTTV

    private fun connectBttv() {
        if (!running) return
        bttv = client.newWebSocket(Request.Builder().url(BTTV).build(), object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                bttvAttempts = 0
                val data = JsonObject().apply { addProperty("name", "twitch:$channelId") }
                val message = JsonObject().apply {
                    addProperty("name", "join_channel")
                    add("data", data)
                }
                ws.send(message.toString())
                Log.i(TAG, "BTTV connected, channel $channelId")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    bttvMessage(JsonParser().parse(text).asJsonObject)
                } catch (e: Exception) {
                    Log.w(TAG, "BTTV-Nachricht", e)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (ws === bttv) later(Runnable { connectBttv() }, bttvAttempts++)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.d(TAG, "BTTV getrennt: $t")
                if (ws === bttv) later(Runnable { connectBttv() }, bttvAttempts++)
            }
        })
    }

    fun bttvMessage(n: JsonObject) {
        val name = n.get("name").asString
        Log.d(TAG, "BTTV: $name")
        val data = n.getAsJsonObject("data")
        if (data == null || "twitch:$channelId" != (if (data.has("channel")) data.get("channel").asString else null)) {
            return
        }
        if ("emote_create" == name || "emote_update" == name) {
            Log.i(TAG, "BTTV $name: ${emotes.bttvEmoteSet(data.getAsJsonObject("emote"))}")
        } else if ("emote_delete" == name) {
            val id = data.get("emoteId").asString
            Log.i(TAG, "BTTV weg: $id ${emotes.bttvEmoteRemove(id)}")
        }
    }
}
