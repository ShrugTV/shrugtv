package com.shrug.shrugtv.companion

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.hub.HubCommandRouter
import com.shrug.shrugtv.hub.PlaybackStateHub
import com.shrug.shrugtv.model.Movie
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URLEncoder

/**
 * HTTP + WebSocket server of the phone companion (docs/companion_plan.md). Serves the web app from
 * `assets/companion/`, the pairing, a small JSON API and `/ws` (state, chat and events to the phone,
 * keys and commands from it). Only private network addresses, only paired phones.
 */
class CompanionServer(
    private val context: Context,
    port: Int,
    private val pairing: Pairing,
    /** Remembered devices (setting "Remember connected devices"); null in tests without storage. */
    private val devices: TrustedDevices? = null,
    /** A device was removed from the list: its open connection has to end. */
    private val onDevicesChanged: () -> Unit = {},
    private val onPhonePaired: (Pairing.Phone) -> Unit
) : NanoWSD(port) {

    companion object {
        const val PREF_REQUIRE_PIN = "pref_companion_require_pin"
        private const val TAG = "Companion"
        private const val COOKIE = "shrugtv_sid"
        private const val MAX_SETTINGS_BODY = 64_000

        /** How long a channel list of the switcher may take for the phone page; tests set it short. */
        @JvmStatic
        var listTimeoutMs = 8_000L

        /** LAN, loopback and link-local only (IPv4 and IPv6). */
        @JvmStatic
        fun isPrivateAddress(ip: String?): Boolean {
            if (ip.isNullOrEmpty()) return false
            val a = ip.removePrefix("::ffff:")
            if (a.startsWith("10.") || a.startsWith("192.168.") || a.startsWith("127.") || a.startsWith("169.254.")) return true
            if (a.startsWith("172.")) return a.split(".").getOrNull(1)?.toIntOrNull()?.let { it in 16..31 } == true
            val l = a.lowercase()
            return l == "::1" || l.startsWith("fe80:") || l.startsWith("fc") || l.startsWith("fd")
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun remember() = devices != null &&
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_companion_remember", true)

    /**
     * PIN asked for (default off): without it every device in the LAN may use the companion;
     * outside the LAN nothing changes, serveHttp and the WebSocket only accept private addresses.
     */
    private fun pinRequired() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_REQUIRE_PIN, false)

    /** No PIN asked for, the cookie of a running session, or the key of a remembered device. */
    private fun authorised(cookieValue: String?, ip: String?): Boolean =
        !pinRequired() || pairing.isValid(cookieValue) || (remember() && devices?.find(cookieValue, ip) != null)

    /** After pairing: a remembered device gets a long-lived key, otherwise the session id (until the app stops). */
    private fun pairedCookie(phone: Pairing.Phone, ua: String, ip: String?): String =
        if (remember()) cookie(devices!!.add(ua, ip).second, persistent = true) else cookie(phone.sessionId)

    private fun chatAllowed() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_companion_chat", true)

    override fun serveHttp(session: IHTTPSession): Response {
        if (!isPrivateAddress(session.remoteIpAddress)) return text(Response.Status.FORBIDDEN, "LAN only")
        val uri = session.uri ?: "/"
        val ua = session.headers["user-agent"] ?: ""
        return try {
            when {
                uri.startsWith("/p/") -> {
                    val phone = pairing.redeemToken(uri.removePrefix("/p/"), ua)
                    if (phone != null) {
                        onPhonePaired(phone)
                        newFixedLengthResponse(Response.Status.REDIRECT_SEE_OTHER, MIME_HTML, "").apply {
                            addHeader("Location", "/")
                            addHeader("Set-Cookie", pairedCookie(phone, ua, session.remoteIpAddress))
                        }
                    } else {
                        newFixedLengthResponse(Response.Status.REDIRECT_SEE_OTHER, MIME_HTML, "").apply { addHeader("Location", "/?expired=1") }
                    }
                }
                uri == "/api/pin" && session.method == Method.POST -> {
                    val body = readBody(session)
                    val pin = try { JsonParser.parseString(body).asJsonObject.get("pin")?.asString } catch (e: Exception) { null }
                    val phone = pin?.let { pairing.redeemPin(it, ua) }
                    if (phone != null) {
                        onPhonePaired(phone)
                        json(JsonObject().apply { addProperty("ok", true) }).apply { addHeader("Set-Cookie", pairedCookie(phone, ua, session.remoteIpAddress)) }
                    } else {
                        json(JsonObject().apply { addProperty("ok", false) }, Response.Status.UNAUTHORIZED)
                    }
                }
                uri.startsWith("/api/") -> {
                    if (!authorised(session.cookies.read(COOKIE), session.remoteIpAddress)) return json(JsonObject().apply { addProperty("error", "not paired") }, Response.Status.UNAUTHORIZED)
                    api(uri, session)
                }
                else -> asset(uri)
            }
        } catch (e: Exception) {
            Log.w(TAG, "serve $uri", e)
            text(Response.Status.INTERNAL_ERROR, "error")
        }
    }

    private fun api(uri: String, session: IHTTPSession): Response = when (uri) {
        "/api/state" -> json(stateJson())
        "/api/settings", "/api/settings/action", "/api/mqtt", "/api/design" -> settings(uri, session)
        "/api/devices", "/api/devices/remove" -> devicesApi(uri, session)
        "/api/log" -> log(session)
        "/api/update", "/api/update/check", "/api/update/install" -> update(uri, session)
        "/api/theme" -> json(SkinApi.theme(context))
        "/api/live" -> json(JsonObject().apply { add("items", liveCards()) })
        "/api/lists" -> lists(session)
        "/api/favorites", "/api/favorite" -> favorites(uri, session)
        "/api/search" -> {
            val q = session.parameters["q"]?.firstOrNull()?.trim()?.take(100) ?: ""
            json(JsonObject().apply { add("items", if (q.isEmpty()) JsonArray() else searchCards(q)) })
        }
        else -> json(JsonObject().apply { addProperty("error", "unknown") }, Response.Status.NOT_FOUND)
    }

    /** All app settings from the browser (SettingsApi); switchable with "Allow settings from the browser". */
    private fun settings(uri: String, session: IHTTPSession): Response {
        if (!SettingsApi.allowed(context)) return json(JsonObject().apply { addProperty("error", "disabled") }, Response.Status.FORBIDDEN)
        if (session.method == Method.GET && uri == "/api/settings") {
            return json(SettingsApi.tree(context, session.parameters["lang"]?.firstOrNull()))
        }
        if (session.method == Method.GET && uri == "/api/mqtt") return json(com.shrug.shrugtv.ha.MqttBridge.info(context))
        if (session.method != Method.POST) return json(JsonObject().apply { addProperty("error", "method") }, Response.Status.METHOD_NOT_ALLOWED)
        if (!sameOrigin(session)) return json(JsonObject().apply { addProperty("error", "origin") }, Response.Status.FORBIDDEN)
        val length = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (length > MAX_SETTINGS_BODY) return json(JsonObject().apply { addProperty("error", "too large") }, Response.Status.BAD_REQUEST)
        val body = try { JsonParser.parseString(readBody(session, MAX_SETTINGS_BODY)).asJsonObject } catch (e: Exception) { null }
            ?: return json(JsonObject().apply { addProperty("error", "bad json") }, Response.Status.BAD_REQUEST)
        if (uri == "/api/design") return json(SettingsApi.design(context, body))
        return if (uri == "/api/settings") {
            val values = body.get("values")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: return json(JsonObject().apply { addProperty("error", "no values") }, Response.Status.BAD_REQUEST)
            json(SettingsApi.apply(context, values, fromBackup = body.get("backup")?.takeIf { it.isJsonPrimitive }?.asBoolean == true))
        } else {
            val key = body.get("key")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            json(SettingsApi.action(context, key))
        }
    }

    /** The channel ids marked with the star in ShrugTV (same list as on the TV). */
    private fun favoriteIds(): Set<String> = try {
        com.shrug.shrugtv.database.AppDatabase.getInstance(context).favoriteChannelDao().allChannelIds.toSet()
    } catch (e: Exception) {
        com.shrug.shrugtv.session.AppState.favoriteChannelIds.toSet()
    }

    /**
     * Stars from the phone: `GET /api/favorites` lists the ids, `POST /api/favorite {"id":"<user id>",
     * "on":true|false}` sets or removes one. The star decides which channels a Home Assistant dashboard shows first
     * (field `favorite` of the MQTT list). Needs "Allow settings from the browser" like the other changes.
     */
    private fun favorites(uri: String, session: IHTTPSession): Response {
        if (uri == "/api/favorites") return json(JsonObject().apply { add("ids", JsonArray().apply { favoriteIds().sorted().forEach { add(it) } }) })
        if (session.method != Method.POST) return json(JsonObject().apply { addProperty("error", "method") }, Response.Status.BAD_REQUEST)
        val body = try { JsonParser.parseString(readBody(session)).asJsonObject } catch (e: Exception) { null }
        if (!sameOrigin(session)) return json(JsonObject().apply { addProperty("error", "origin") }, Response.Status.FORBIDDEN)
        if (!SettingsApi.allowed(context)) return json(JsonObject().apply { addProperty("error", "disabled") }, Response.Status.FORBIDDEN)
        val id = body?.get("id")?.takeIf { it.isJsonPrimitive }?.asString
        val on = body?.get("on")?.takeIf { it.isJsonPrimitive }?.asBoolean
        if (id == null || !Regex("^[0-9]{1,12}$").matches(id) || on == null) {
            return json(JsonObject().apply { addProperty("error", "bad request") }, Response.Status.BAD_REQUEST)
        }
        val done = java.util.concurrent.CountDownLatch(1)
        val callback = object : com.shrug.shrugtv.apihelpers.FavoriteChangedCallback {
            override fun onChanged() { done.countDown() }
        }
        if (on) com.shrug.shrugtv.apihelpers.FavoriteChannels.setFavorite(context, id, callback)
        else com.shrug.shrugtv.apihelpers.FavoriteChannels.removeFavorite(context, id, callback)
        done.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return json(JsonObject().apply { addProperty("ok", true); addProperty("id", id); addProperty("favorite", on) })
    }

    /** Version and update menu (UpdateApi); checking and installing need a POST from this page and "Allow settings from the browser". */
    private fun update(uri: String, session: IHTTPSession): Response {
        if (uri == "/api/update") return json(UpdateApi.info(context))
        if (session.method != Method.POST) return json(JsonObject().apply { addProperty("error", "method") }, Response.Status.BAD_REQUEST)
        readBody(session) // unread body bytes would be taken for the next request on a kept-alive connection
        if (!sameOrigin(session)) return json(JsonObject().apply { addProperty("error", "origin") }, Response.Status.FORBIDDEN)
        if (!SettingsApi.allowed(context)) return json(JsonObject().apply { addProperty("error", "disabled") }, Response.Status.FORBIDDEN)
        return json(if (uri == "/api/update/check") UpdateApi.check(context) else UpdateApi.install(context))
    }

    /** Remembered devices: list (with the asking device marked) and removing one. */
    private fun devicesApi(uri: String, session: IHTTPSession): Response {
        val store = devices ?: return json(JsonObject().apply { add("devices", JsonArray()) })
        if (!SettingsApi.allowed(context)) return json(JsonObject().apply { addProperty("error", "disabled") }, Response.Status.FORBIDDEN)
        val mine = session.cookies.read(COOKIE)?.let { c -> store.find(c, session.remoteIpAddress)?.id }
        if (uri == "/api/devices/remove") {
            if (session.method != Method.POST || !sameOrigin(session)) return json(JsonObject().apply { addProperty("error", "method") }, Response.Status.BAD_REQUEST)
            val id = try { JsonParser.parseString(readBody(session)).asJsonObject.get("id")?.asString } catch (e: Exception) { null }
            val ok = id != null && store.remove(id)
            if (ok) onDevicesChanged()
            return json(JsonObject().apply { addProperty("ok", ok) })
        }
        return json(JsonObject().apply {
            addProperty("remember", remember())
            mine?.let { addProperty("current", it) }
            add("devices", JsonArray().apply {
                store.list().forEach { d ->
                    add(JsonObject().apply {
                        addProperty("id", d.id)
                        addProperty("name", d.name)
                        addProperty("paired", d.pairedAt)
                        addProperty("seen", d.lastSeen)
                    })
                }
            })
        })
    }

    /** App log for the browser (AppLog); switch "Log in the browser". */
    private fun log(session: IHTTPSession): Response {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_companion_log", true)) {
            return json(JsonObject().apply { addProperty("error", "disabled") }, Response.Status.FORBIDDEN)
        }
        val after = session.parameters["after"]?.firstOrNull()?.toLongOrNull() ?: 0L
        val limit = session.parameters["limit"]?.firstOrNull()?.toIntOrNull() ?: AppLog.MAX_LINES
        val (lines, first) = AppLog.since(after, limit)
        return json(JsonObject().apply {
            addProperty("first", first)
            addProperty("running", AppLog.isRunning)
            addProperty("version", BuildConfig.VERSION_NAME)
            addProperty("device", android.os.Build.MODEL)
            // Compact rows: [seq, time ms, level, tag, message, pid, tid]
            add("lines", JsonArray().apply {
                for (l in lines) add(JsonArray().apply {
                    add(l.seq); add(l.timeMs); add(l.level.toString()); add(l.tag); add(l.msg); add(l.pid); add(l.tid)
                })
            })
        })
    }

    /** Browsers send Origin with POSTs; it must be this server (on top of the SameSite cookie). */
    private fun sameOrigin(session: IHTTPSession): Boolean {
        val origin = session.headers["origin"] ?: return true
        val host = session.headers["host"] ?: return false
        return origin == "http://$host"
    }

    private fun stateJson(): JsonObject = PlaybackStateHub.state.value.toJson(System.currentTimeMillis(), BuildConfig.VERSION_NAME).apply {
        addProperty("chat_allowed", chatAllowed())
        PlaybackStateHub.state.value.userId?.let { addProperty("favorite", it in favoriteIds()) }
        addProperty("player_open", PlaybackStateHub.commands != null)
        addProperty("settings_allowed", SettingsApi.allowed(context))
        addProperty("log_allowed", PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_companion_log", true))
    }

    private fun card(m: Movie, favs: Set<String> = favoriteIds()): JsonObject = JsonObject().apply {
        if (m.userId > 0) addProperty("id", m.userId.toString())
        addProperty("login", m.login)
        addProperty("name", m.displayName.ifEmpty { m.login ?: "" })
        addProperty("category", m.gameName)
        addProperty("title", m.title)
        addProperty("viewers", m.viewerCount)
        addProperty("image", m.cardImageUrl)
        addProperty("avatar", m.avatarUrl)
        addProperty("favorite", m.isFavorite || (m.userId > 0 && m.userId.toString() in favs))
    }

    private fun liveCards(): JsonArray = JsonArray().apply {
        val list = try {
            runBlocking { com.shrug.shrugtv.ui.tv.home.HomeData.followedLive(context) }
        } catch (e: Exception) {
            emptyList()
        }
        val favs = favoriteIds()
        list.sortedByDescending { it.isFavorite || it.userId.toString() in favs }.take(50).forEach { add(card(it, favs)) }
    }

    /**
     * One list of the channel switcher on the TV for the phone: `?name=followed|same_game|
     * same_game_language|history|top_streams|top_language`; answers with the lists switched on in the TV settings and the
     * channels of the asked one, without the one that is playing.
     */
    private fun lists(session: IHTTPSession): Response {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val enabled = com.shrug.shrugtv.ui.tv.player.SwitcherNav.enabled { prefs.getBoolean(it.key, true) }
        val name = session.parameters["name"]?.firstOrNull() ?: "followed"
        val tab = com.shrug.shrugtv.ui.tv.player.ChannelSwitcherTab.values().firstOrNull { it.name.lowercase() == name }
            ?: return json(JsonObject().apply { addProperty("error", "unknown list") }, Response.Status.NOT_FOUND)
        val playing = PlaybackStateHub.state.value.login.orEmpty()
        val movies = try {
            runBlocking {
                // Without a network or a login the lists never answer: the page gets an empty list instead of waiting
                kotlinx.coroutines.withTimeoutOrNull(listTimeoutMs) {
                    com.shrug.shrugtv.ui.tv.player.SwitcherLists.load(context, tab) {
                        if (playing.isNotEmpty()) com.shrug.shrugtv.ui.tv.home.HomeData.gameIdOfLogin(context, playing) else 0L
                    }
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
        return json(JsonObject().apply {
            addProperty("name", tab.name.lowercase())
            add("enabled", JsonArray().apply { enabled.forEach { add(it.name.lowercase()) } })
            addProperty("playing", playing.isNotEmpty())
            add("items", JsonArray().apply {
                val favs = favoriteIds()
                movies.filter { it.login != null && !it.login.equals(playing, ignoreCase = true) }.take(50).forEach { add(card(it, favs)) }
            })
        })
    }

    private fun searchCards(q: String): JsonArray = JsonArray().apply {
        val favs = favoriteIds()
        val json = HelixApi.searchChannels(context, URLEncoder.encode(q, "UTF-8"), false) ?: return@apply
        for (e in json.getAsJsonArray("data") ?: return@apply) {
            val o = e.asJsonObject
            add(JsonObject().apply {
                val id = o.get("id")?.asString
                if (id != null) { addProperty("id", id); addProperty("favorite", id in favs) }
                addProperty("login", o.get("broadcaster_login")?.asString)
                addProperty("name", o.get("display_name")?.asString)
                addProperty("category", o.get("game_name")?.asString)
                addProperty("title", o.get("title")?.asString)
                addProperty("live", o.get("is_live")?.asBoolean ?: false)
                addProperty("avatar", o.get("thumbnail_url")?.asString)
            })
        }
    }

    // ------------------------------------------------------------------ static files

    private fun asset(uri: String): Response {
        val name = when (uri) {
            "/", "/index.html" -> "index.html"
            "/app.js" -> "app.js"
            "/app.css" -> "app.css"
            "/manifest.json" -> return manifest()
            // Browsers ask for /favicon.ico by themselves; the PNG is the app icon (shrug figure)
            "/favicon.png", "/favicon.ico" -> "favicon.png"
            else -> return text(Response.Status.NOT_FOUND, "not found")
        }
        var bytes = context.assets.open("companion/$name").use { it.readBytes() }
        // Cache buster: the page asks for app.js and app.css with a checksum of the file, so a changed file is always fetched anew
        if (name == "index.html") {
            var html = String(bytes, Charsets.UTF_8)
            for (file in listOf("app.js", "app.css")) {
                val v = context.assets.open("companion/$file").use { java.util.zip.CRC32().apply { update(it.readBytes()) }.value.toString(16) }
                html = html.replace("\"/$file\"", "\"/$file?v=$v\"")
            }
            bytes = html.toByteArray(Charsets.UTF_8)
        }
        val mime = when {
            name.endsWith(".html") -> "text/html; charset=utf-8"
            name.endsWith(".js") -> "application/javascript; charset=utf-8"
            name.endsWith(".css") -> "text/css; charset=utf-8"
            name.endsWith(".png") -> "image/png"
            else -> "application/json; charset=utf-8"
        }
        return newFixedLengthResponse(Response.Status.OK, mime, bytes.inputStream(), bytes.size.toLong()).apply {
            addHeader("Cache-Control", "no-cache")
            addHeader("X-Content-Type-Options", "nosniff")
            addHeader("Referrer-Policy", "no-referrer")
        }
    }

    /** Web app manifest: the colours follow the skin of the TV when "TV skin on the phone page" is on, so the installed icon matches. */
    private fun manifest(): Response {
        val bytes = SkinApi.manifest(context).toByteArray(Charsets.UTF_8)
        return newFixedLengthResponse(Response.Status.OK, "application/manifest+json; charset=utf-8", bytes.inputStream(), bytes.size.toLong()).apply {
            addHeader("Cache-Control", "no-cache")
            addHeader("X-Content-Type-Options", "nosniff")
        }
    }

    /** Session cookie; remembered devices keep theirs for a year (renewed while in use is not needed: the key stays). */
    private fun cookie(value: String, persistent: Boolean = false) =
        "$COOKIE=$value; Path=/; HttpOnly; SameSite=Strict" + if (persistent) "; Max-Age=31536000" else ""

    /**
     * The request body as UTF-8. NanoHTTPD's parseBody decodes by the charset of the Content-Type and falls back to
     * US-ASCII, which broke every umlaut from the browser; so the bytes are read here.
     */
    private fun readBody(session: IHTTPSession, limit: Int = 2_000): String {
        val length = (session.headers["content-length"]?.toLongOrNull() ?: 0L).coerceIn(0L, limit * 4L).toInt()
        val buf = ByteArray(length)
        var read = 0
        val input = session.inputStream
        while (read < length) {
            val n = input.read(buf, read, length - read)
            if (n < 0) break
            read += n
        }
        return String(buf, 0, read, Charsets.UTF_8).take(limit)
    }

    private fun text(status: Response.Status, body: String) = newFixedLengthResponse(status, "text/plain; charset=utf-8", body)

    private fun json(o: JsonObject, status: Response.Status = Response.Status.OK) =
        newFixedLengthResponse(status, "application/json; charset=utf-8", o.toString()).apply { addHeader("Cache-Control", "no-store") }

    // ------------------------------------------------------------------ WebSocket

    override fun openWebSocket(handshake: IHTTPSession): WebSocket = PhoneSocket(handshake)

    private inner class PhoneSocket(handshake: IHTTPSession) : WebSocket(handshake) {
        private val authorised = isPrivateAddress(handshake.remoteIpAddress) && authorised(handshake.cookies.read(COOKIE), handshake.remoteIpAddress)
        private val jobs = ArrayList<Job>()
        @Volatile private var chatSubscribed = false
        private var lastSentLine = -1L
        private val sentDeleted = HashSet<Long>()
        private var windowStart = 0L
        private var windowCount = 0
        /** Held here: SharedPreferences keeps listeners only weakly. */
        private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) scope.launch { sendJson("settings", JsonObject().apply { addProperty("key", key) }) }
        }

        override fun onOpen() {
            if (!authorised) {
                try { close(WebSocketFrame.CloseCode.PolicyViolation, "not paired", false) } catch (_: IOException) {}
                return
            }
            sendJson("state", stateJson())
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(prefListener)
            @OptIn(FlowPreview::class)
            jobs += scope.launch {
                PlaybackStateHub.state.debounce(300L).collect { sendJson("state", stateJson()) }
            }
            jobs += scope.launch {
                PlaybackStateHub.events.collect { sendJson("event", it.toJson()) }
            }
            jobs += scope.launch {
                var ticks = 0
                while (true) {
                    delay(500L)
                    if (chatSubscribed) pushChat()
                    // Keep the connection alive through Wi-Fi power saving
                    if (++ticks % 40 == 0) {
                        try { ping("p".toByteArray()) } catch (_: IOException) {}
                    }
                }
            }
        }

        private fun pushChat() {
            val lines = PlaybackStateHub.chatLines?.value ?: return
            val out = JsonArray()
            for (l in lines) {
                if (l.id > lastSentLine) {
                    out.add(ChatMirror.line(l))
                    if (l.deleted) sentDeleted.add(l.id)
                } else if (l.deleted && sentDeleted.add(l.id)) {
                    out.add(JsonObject().apply { addProperty("id", l.id); addProperty("d", true); addProperty("upd", true) })
                }
            }
            lines.lastOrNull()?.let { if (it.id > lastSentLine) lastSentLine = it.id }
            if (out.size() > 0) sendJson("chat", JsonObject().apply { add("lines", out) })
        }

        override fun onMessage(message: WebSocketFrame) {
            if (!authorised) return
            // Max. 10 messages per second and phone
            val now = System.currentTimeMillis()
            if (now - windowStart > 1_000L) { windowStart = now; windowCount = 0 }
            if (++windowCount > 10) return
            val o = try { JsonParser.parseString(message.textPayload.take(2_000)).asJsonObject } catch (e: Exception) { return }
            when (o.get("t")?.asString) {
                "key" -> o.get("k")?.asString?.let { KeyInjector.press(it) }
                "cmd" -> {
                    val name = o.get("name")?.asString ?: return
                    if (name == "chat_message" && !chatAllowed()) return
                    if (name in HubCommandRouter.NAMES) {
                        HubCommandRouter.dispatch(context, name, o.get("payload")?.takeIf { !it.isJsonNull }?.asString ?: "")
                    }
                }
                "chat" -> {
                    chatSubscribed = o.get("on")?.asBoolean ?: false
                    if (chatSubscribed) { lastSentLine = -1L; sentDeleted.clear() }
                }
            }
        }

        private fun sendJson(type: String, data: JsonObject) {
            try {
                if (isOpen) send(JsonObject().apply { addProperty("t", type); add("d", data) }.toString())
            } catch (e: IOException) {
                Log.d(TAG, "send: ${e.message}")
            }
        }

        override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) = cleanUp()

        private fun cleanUp() {
            jobs.forEach { it.cancel() }
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(prefListener)
        }

        override fun onPong(pong: WebSocketFrame?) = Unit

        override fun onException(exception: IOException?) = cleanUp()
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        stop()
    }
}
