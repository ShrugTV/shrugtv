package com.shrug.shrugtv.chat

import android.graphics.Typeface
import android.os.Message
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.R
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.chat.emote.EmoteLiveUpdates
import com.shrug.shrugtv.chat.emote.EmoteManager
import com.shrug.shrugtv.eventSub.ChatEvent
import com.shrug.shrugtv.eventSub.EventSubChatMessageModel
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.eventSub.EventSubModels
import com.shrug.shrugtv.eventSub.EventSubRaidNotificationModel
import com.shrug.shrugtv.eventSub.EventSubWelcomeModel
import com.shrug.shrugtv.model.RaidNotification
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Live chat over Twitch EventSub (WebSocket) with sending via Helix.
 *
 * Connection handling (docs/chat_plan.md phase 1): the envelope is parsed once; a watchdog
 * reconnects when no message (not even a keepalive) arrived within the session's keepalive timeout;
 * `session_reconnect` moves to the new URL without losing the subscriptions; a dropped socket is
 * rebuilt with backoff and subscribes again. All work except parsing runs on the chat thread.
 */
class LiveChatClient(builder: ChatClient.Builder) : ChatClient(builder) {

    companion object {
        private const val TAG_EVENTSUB = "EventSub"
        private const val EVENTSUB_WS_URL = "wss://eventsub.wss.twitch.tv/ws"
        private const val WATCHDOG_INTERVAL_MS = 5_000L


        /** Reconnect backoff: 1, 2, 4, 8, 16 s, then every 30 s. */
        @JvmStatic
        fun reconnectDelayMs(attempt: Int): Long = (1_000L shl attempt.coerceIn(0, 5)).coerceAtMost(30_000L)

        /** Sessions this app process has left behind (closed or replaced): their subscriptions may go. */
        private val deadSessions: MutableSet<String> = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

        /**
         * Ids of WebSocket subscriptions that block new ones: not enabled any more, or on a session
         * this process left behind. Subscriptions of other devices (an enabled session we do not
         * know) stay.
         */
        @JvmStatic
        fun staleSubscriptionIds(list: JsonObject?, dead: Set<String>): List<String> {
            val data = list?.getAsJsonArray("data") ?: return emptyList()
            return data.mapNotNull { e ->
                val o = e.asJsonObject
                val transport = o.getAsJsonObject("transport") ?: return@mapNotNull null
                if (transport.get("method")?.asString != "websocket") return@mapNotNull null
                val status = o.get("status")?.asString
                val session = transport.get("session_id")?.asString
                if (status != "enabled" || session in dead) o.get("id")?.asString else null
            }
        }

        /** Twitch ends a session after keepalive_timeout_seconds without any message; 5 s slack. */
        @JvmStatic
        fun keepaliveDeadlineMs(timeoutSeconds: Int?): Long = ((timeoutSeconds ?: 10).coerceIn(10, 600) + 5) * 1_000L
    }

    fun interface EmotesLoaded {
        fun done(count: Int)
    }

    @Volatile
    private var running: Boolean = true

    // Shares connection pool and dispatcher with the rest of the app: never shut its dispatcher down
    private val wsClient: OkHttpClient = Http.client().newBuilder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()

    /** Socket of the current session; replaced on session_reconnect and after a failure. */
    @Volatile
    private var eventSubSocket: WebSocket? = null

    @Volatile
    private var sessionId: String? = null

    /** All subscriptions created on the current session, deleted when the chat stops. */
    private val subscriptionIds = ConcurrentLinkedQueue<String>()

    @Volatile
    private var lastMessageAt: Long = 0L

    @Volatile
    private var keepaliveDeadline: Long = keepaliveDeadlineMs(null)

    @Volatile
    private var reconnectAttempt: Int = 0

    /** Socket opened on session_reconnect until its welcome; closed if the chat stops meanwhile. */
    @Volatile
    private var pendingSocket: WebSocket? = null

    /** "Connected to the chat" is shown once per chat, not after every reconnect. */
    @Volatile
    private var announcedConnected = false

    private val watchdog: Runnable = object : Runnable {
        override fun run() {
            if (!running) return
            if (sessionId != null && SystemClock.elapsedRealtime() - lastMessageAt > keepaliveDeadline) {
                lastMessageAt = SystemClock.elapsedRealtime()
                scheduleReconnect("keepalive timeout")
            }
            workerHandler?.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    private val reconnectRunnable = Runnable {
        if (!running) return@Runnable
        reconnectAttempt++
        sessionId?.let { deadSessions.add(it) }
        eventSubSocket?.cancel()
        eventSubSocket = null
        sessionId = null
        connectEventSub()
    }

    /** 7TV paints, badges and personal emotes of the chatters (settings "7TV …"). */
    private val cosmetics: com.shrug.shrugtv.chat.seventv.SevenTvCosmetics? =
        if (extras.sevenTvCosmetics) com.shrug.shrugtv.chat.seventv.SevenTvCosmetics() else null

    private var emoteLoaderPool: ExecutorService? = null
    private var emoteLive: EmoteLiveUpdates? = null
    private val gson = Gson()

    fun emotesReload(feedback: EmotesLoaded?) {
        if (channelId <= 0) {
            feedback?.done(0)
            return
        }
        Thread({
            try {
                emoteManager.loadBttvChannelEmotes()
                emoteManager.loadFfzChannelEmotes()
                emoteManager.loadSevenTvChannelEmotes()
                emoteManager.mergeChannelEmotes()
                if (running) {
                    emoteLive?.stop()
                    val live = EmoteLiveUpdates(emoteManager, channelId, cosmetics)
                    emoteLive = live
                    live.start()
                }
            } catch (e: Exception) {
                Log.w("ChatManager", "Reload emotes", e)
            }
            feedback?.done(emoteManager.getChannelEmoteCount())
        }, "Reload emotes").start()
    }


    /**
     * Listener of one socket. [replaces] is set for the socket opened on `session_reconnect`: after
     * its welcome it takes over from that socket and the subscriptions carry over.
     */
    private inner class EventSubListener(private val replaces: WebSocket?) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG_EVENTSUB, if (replaces == null) "WebSocket open" else "Reconnect socket open")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // OkHttp reader thread: parse the envelope once, never block here (no image loading,
            // no per-message logging of chat contents)
            lastMessageAt = SystemClock.elapsedRealtime()
            try {
                val root = JsonParser.parseString(text).asJsonObject
                val metadata = root.getAsJsonObject("metadata")
                val messageType = metadata?.get("message_type")?.asString
                val subscriptionType = metadata?.get("subscription_type")?.asString
                when (messageType) {
                    "session_welcome" -> onWelcome(webSocket, replaces, gson.fromJson(root, EventSubWelcomeModel::class.java))
                    "session_keepalive" -> Unit
                    "session_reconnect" -> onReconnectRequested(webSocket, gson.fromJson(root, EventSubWelcomeModel::class.java))
                    "revocation" -> {
                        val id = root.getAsJsonObject("payload")?.getAsJsonObject("subscription")?.get("id")?.asString
                        if (id != null) subscriptionIds.remove(id)
                        Log.w(TAG_EVENTSUB, "Subscription revoked: $subscriptionType")
                    }
                    "notification" -> when (subscriptionType) {
                        "channel.raid" -> onRaid(gson.fromJson(root, EventSubRaidNotificationModel::class.java))
                        "channel.chat.message" -> {
                            if (!running) return
                            val chatMsg = gson.fromJson(root, EventSubChatMessageModel::class.java)
                            val ev = chatMsg.payload?.event
                            Log.i(TAG_EVENTSUB, "Incoming chat message from ${ev?.chatterUserLogin}: ${ev?.message?.text}")
                            // Emote images are loaded with Glide (blocking): do it on the chat thread
                            post(Runnable { dispatchChatMessage(chatMsg) })
                        }
                        else -> {
                            if (!running) return
                            val event = root.getAsJsonObject("payload")?.getAsJsonObject("event")
                            ChatEvent.parse(subscriptionType, event)?.let { onChatEvent(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG_EVENTSUB, "WebSocket failure: ${t.message}")
            if (webSocket === eventSubSocket) scheduleReconnect("failure")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG_EVENTSUB, "WebSocket closed: $code")
            // Closed by Twitch (e.g. 4005 network timeout, 4007 invalid reconnect): start over
            if (webSocket === eventSubSocket) scheduleReconnect("closed $code")
        }
    }

    private fun onWelcome(socket: WebSocket, replaces: WebSocket?, welcome: EventSubWelcomeModel?) {
        val session = welcome?.payload?.session ?: return
        val id = session.id ?: return
        if (!running) {
            // The chat stopped while this socket was connecting: do not leave a session open
            socket.close(1000, "stopped")
            return
        }
        keepaliveDeadline = keepaliveDeadlineMs(session.keepaliveTimeoutSeconds)
        reconnectAttempt = 0
        if (replaces == null) mark("EventSub welcome")
        if (replaces != null) {
            // session_reconnect: same session id, subscriptions stay; the old socket can go
            if (pendingSocket === socket) pendingSocket = null
            eventSubSocket = socket
            sessionId = id
            replaces.close(1000, "reconnected")
            Log.d(TAG_EVENTSUB, "Reconnected")
            return
        }
        if (socket !== eventSubSocket) return
        sessionId = id
        subscriptionIds.clear()
        if (channelId <= 0) {
            Log.w(TAG_EVENTSUB, "Cannot subscribe EventSub with invalid channelId=$channelId")
            return
        }
        subscribe(EventSubModels.getRaidSub("$channelId", id), "channel.raid")
        val userId = AppState.userId
        Log.i(TAG_EVENTSUB, "onWelcome: session=$id, broadcaster=$channelId, userId=$userId")
        if (userId.isNotEmpty()) {
            for (type in EventSubModels.CHAT_TYPES) {
                subscribe(EventSubModels.getChatSub(type, "$channelId", userId, id), type)
            }
        }
    }

    /** Twitch asks to move to a new URL; it keeps sending on the old socket until the new welcome. */
    private fun onReconnectRequested(oldSocket: WebSocket, model: EventSubWelcomeModel?) {
        val url = model?.payload?.session?.reconnectUrl ?: return
        if (!running) return
        Log.d(TAG_EVENTSUB, "session_reconnect requested")
        pendingSocket?.cancel()
        pendingSocket = wsClient.newWebSocket(
            Request.Builder().url(url).addHeader("Authorization", "Bearer " + AppState.accessToken).build(),
            EventSubListener(oldSocket)
        )
    }

    /** Rebuilds the connection after a failure, with backoff; runs on the chat thread. */
    private fun scheduleReconnect(reason: String) {
        if (!running) return
        val handler = workerHandler ?: return
        val delay = reconnectDelayMs(reconnectAttempt)
        Log.w(TAG_EVENTSUB, "Connection lost ($reason), reconnecting in $delay ms")
        if (reconnectAttempt == 0) postNotice(R.string.player_chat_reconnecting)
        handler.removeCallbacks(reconnectRunnable)
        handler.postDelayed(reconnectRunnable, delay)
    }

    /** Opens a fresh session; chat thread. */
    private fun connectEventSub() {
        if (!running) return
        lastMessageAt = SystemClock.elapsedRealtime()
        eventSubSocket = wsClient.newWebSocket(
            Request.Builder().url(EVENTSUB_WS_URL).addHeader("Authorization", "Bearer " + AppState.accessToken).build(),
            EventSubListener(null)
        )
        workerHandler?.removeCallbacks(watchdog)
        workerHandler?.postDelayed(watchdog, WATCHDOG_INTERVAL_MS)
    }

    /**
     * Creates one subscription on the current session. Failures used to end up only in the log, so
     * the chat stayed empty without a word; now: 409 (exists) counts as done, 429 (limit, e.g. old
     * sessions of this app still counted) cleans up stale subscriptions and tries once more, network
     * and server errors are retried with backoff, and if the chat itself cannot be subscribed the
     * reason shows in the chat panel.
     */
    private fun subscribe(subJson: String, type: String, attempt: Int = 0) {
        val ctx = contextRef?.get() ?: return
        val session = sessionId
        try {
            HelixApi.createEventSubSubscription(ctx, subJson) { exc, obj ->
                if (!running || sessionId != session) return@createEventSubSubscription
                if (exc != null) {
                    val status = Http.StatusException.of(exc)
                    val code = status?.code
                    Log.e(TAG_EVENTSUB, "Subscription $type failed: HTTP $code ${status?.body ?: exc.message} (sub=$subJson)")
                    when {
                        code == 409 -> Unit
                        code == 429 && attempt == 0 -> post(Runnable {
                            cleanUpStaleSubscriptions()
                            subscribe(subJson, type, 1)
                        })
                        (code == null || code >= 500) && attempt < 3 -> workerHandler?.postDelayed({
                            if (running && sessionId == session) subscribe(subJson, type, attempt + 1)
                        }, reconnectDelayMs(attempt + 1))
                        type == "channel.chat.message" -> postNotice(R.string.player_chat_subscribe_failed, reason(status, exc))
                    }
                    return@createEventSubSubscription
                }
                val id = (obj as? JsonObject)?.getAsJsonArray("data")?.firstOrNull()?.asJsonObject?.get("id")?.asString
                if (id != null) subscriptionIds.add(id)
                Log.i(TAG_EVENTSUB, "Subscription $type established: id=$id")
                if (type == "channel.chat.message") mark("chat subscription established")
                if (type == "channel.chat.message" && !announcedConnected) {
                    announcedConnected = true
                    postNotice(R.string.player_chat_connected)
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    /** Twitch's message ("websocket transport session limit exceeded" …) or the HTTP code. */
    private fun reason(status: Http.StatusException?, exc: Exception): String {
        val msg = try {
            status?.body?.let { JsonParser.parseString(it).asJsonObject.get("message")?.asString }
        } catch (e: Exception) {
            null
        }
        return msg ?: status?.code?.let { "HTTP $it" } ?: (exc.message ?: "?")
    }

    /** Deletes subscriptions that block new ones (see [staleSubscriptionIds]); chat thread, blocking. */
    private fun cleanUpStaleSubscriptions() {
        val ctx = contextRef?.get() ?: return
        try {
            val stale = staleSubscriptionIds(HelixApi.getEventSubSubscriptions(ctx), deadSessions)
            Log.w(TAG_EVENTSUB, "Removing ${stale.size} stale subscriptions")
            for (id in stale) HelixApi.deleteEventSubSubscription(ctx, id)
            if (stale.isNotEmpty()) Thread.sleep(1_000L) // the deletes run asynchronously
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    /** A grey, italic line in the chat panel (no user, not filtered). */
    private fun postNotice(textRes: Int, vararg args: Any) {
        val ctx = contextRef?.get() ?: return
        val text = ctx.getString(textRes, *args)
        val ssb = SpannableStringBuilder(text).apply {
            setSpan(ForegroundColorSpan(0xFFADADB8.toInt()), 0, length, 33)
            setSpan(StyleSpan(Typeface.ITALIC), 0, length, 33)
        }
        val msg = ChatMessage.Builder().text(text).displayName("").formatted(ssb).build()
        uiSink?.onEvent(ChatUiEvent.Message(msg))
    }

    /** Notices go through the processor (filters, rendering); the rest straight to the UI. */
    private fun onChatEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Notice -> post(Runnable { dispatchNotice(event) })
            is ChatEvent.Delete -> uiSink?.onEvent(ChatUiEvent.Delete(event.messageId))
            is ChatEvent.ClearUser -> uiSink?.onEvent(ChatUiEvent.ClearUser(event.userLogin))
            is ChatEvent.ClearAll -> uiSink?.onEvent(ChatUiEvent.ClearAll)
            is ChatEvent.RoomSettings -> uiSink?.onEvent(ChatUiEvent.RoomSettings(event))
        }
    }

    private fun dispatchNotice(notice: ChatEvent.Notice) {
        if (!running) return
        val text = if (notice.messageText != null && notice.systemMessage.isNotEmpty()) {
            "${notice.systemMessage} ${notice.messageText}"
        } else {
            notice.systemMessage.ifEmpty { notice.messageText ?: return }
        }
        val msg = ChatMessage.Builder()
            .text(text)
            .displayName(notice.chatterName)
            .chatterLogin(notice.chatterLogin)
            .notice(true)
            .noticeType(
                when {
                    notice.isGift -> Enums.SubType.GIFTED_SUB
                    notice.isSub -> Enums.SubType.NORMAL_SUB
                    else -> Enums.SubType.NULL
                }
            )
            .noticeUser(notice.chatterName)
            .messageId(notice.messageId)
            .channelName(channelName)
            .slotId(slotId)
            .build()
        postMessage(msg)
    }

    /** Room state when the chat starts; later changes come as `channel.chat_settings.update`. */
    private fun loadRoomSettings() {
        if (channelId <= 0) return
        val ctx = contextRef?.get() ?: return
        HelixApi.getChatSettings(ctx, channelId) { exc, json ->
            if (exc != null || !running) return@getChatSettings
            val data = (json as? JsonObject)?.getAsJsonArray("data")?.firstOrNull()?.asJsonObject ?: return@getChatSettings
            uiSink?.onEvent(ChatUiEvent.RoomSettings(ChatEvent.roomSettings(data, eventSub = false)))
        }
    }

    private fun onRaid(raid: EventSubRaidNotificationModel?) {
        val event = raid?.payload?.event ?: return
        val toId = event.toBroadcasterUserId
        val toName = event.toBroadcasterUserName
        if (toId != null && toName != null) {
            postRaidEvent(RaidNotification(slotId, toName, toId))
        }
    }

    /** Builds the [ChatMessage] (loads emote images) and hands it to the processor. Chat thread. */
    private fun dispatchChatMessage(chatMsg: EventSubChatMessageModel) {
        if (!running) return
        val ev = chatMsg.payload?.event ?: return
        val text = ev.message?.text ?: return
        val user = ev.chatterUserName ?: ev.chatterUserLogin ?: ""
        Log.i(TAG_EVENTSUB, "dispatchChatMessage start: user=$user, text=$text")
        val emoteList = ArrayList<com.shrug.shrugtv.chat.emote.Emote>()
        // Personal 7TV emotes first: they win over channel and global emotes of the same name
        val personalSet = cosmetics?.emoteSetOf(ev.chatterUserId)
        val personal = emoteManager.findPersonalEmotes(personalSet, text)
        emoteList.addAll(personal)
        val taken = personal.map { it.code }.toSet()
        emoteList.addAll(emoteManager.findChannelEmotes(text).filter { it.code !in taken })
        emoteList.addAll(emoteManager.findGlobalEmotes(text).filter { it.code !in taken })
        // Native Twitch emotes come as fragments; EventSub has no emote offsets of its own
        for ((id, pos) in EventSubChatMessageModel.twitchEmotePositions(ev.message.fragments)) {
            val existing = emoteList.firstOrNull { it.provider == com.shrug.shrugtv.chat.emote.Emote.Provider.TWITCH && it.id == id }
            if (existing != null) {
                existing.addPosition(pos)
            } else {
                emoteManager.createTwitchEmote(id, listOf(pos))?.let { emoteList.add(it) }
            }
        }
        val badges = ev.badges.orEmpty()
        val subBadgeId = badges.firstOrNull { it.setId == "subscriber" || it.setId == "founder" }?.id
        // Real badge images in Twitch's order (docs/chat_plan.md 2.4)
        val badgeImages = badges.mapNotNull { b ->
            val set = b.setId ?: return@mapNotNull null
            val version = b.id ?: return@mapNotNull null
            emoteManager.badgeDrawable(set, version)
        }
        val reply = ev.reply?.let { r ->
            if (r.parentMessageId == null) null else ChatReply(
                parentMsgId = r.parentMessageId,
                parentUserLogin = r.parentUserLogin ?: "",
                parentDisplayName = r.parentUserName ?: r.parentUserLogin ?: "",
                parentMsgBody = r.parentMessageBody ?: ""
            )
        }
        val highlighted = ev.messageType == "channel_points_highlighted" || ev.messageType == "user_intro"
        val msg = ChatMessage.Builder()
            .text(text)
            .displayName(user)
            .chatterLogin(ev.chatterUserLogin)
            .badgeDrawables(badgeImages)
            .badges(badges.mapNotNull { b -> ChatBadge(b.setId ?: return@mapNotNull null, b.id ?: return@mapNotNull null) })
            .reply(reply)
            .highlighted(highlighted)
            .cheerBits(ev.cheer?.bits ?: 0)
            .color(ev.color ?: "")
            .channelName(channelName)
            .slotId(slotId)
            .messageId(ev.messageId ?: chatMsg.metadata?.messageId)
            .subscriber(subBadgeId != null)
            .subscriberBadge(subBadgeId?.let { subscriberBadges?.get(it) })
            .mod(badges.any { it.setId == "moderator" })
            .broadcaster(badges.any { it.setId == "broadcaster" } || ev.chatterUserLogin.equals(channelName, ignoreCase = true))
            .vip(badges.any { it.setId == "vip" })
            .partner(badges.any { it.setId == "partner" })
            .prime(badges.any { it.setId == "premium" })
            .emotes(emoteList)
            .paint(if (extras.sevenTvPaints) cosmetics?.paintOf(ev.chatterUserId) else null)
            .sevenTvBadge(if (extras.sevenTvBadges) emoteManager.sevenTvBadge(cosmetics?.badgeUrlOf(ev.chatterUserId)) else null)
            .build()
        postMessage(msg)
    }

    /**
     * The last messages before the chat was opened (setting "Show chat history", like 7TV): from
     * the third-party service recent-messages.robotty.de, which keeps the last messages of public
     * channels. Blocking, chat thread, before the live connection starts.
     */
    private fun loadRecentMessages() {
        val login = channelName?.lowercase()?.takeIf { it.matches(Regex("[a-z0-9_]{1,25}")) } ?: return
        try {
            val json = Http.load("https://recent-messages.robotty.de/api/v2/recent-messages/$login?limit=50&hide_moderation_messages=true")
                .asJsonObject().get() ?: return
            var count = 0
            for (e in json.getAsJsonArray("messages") ?: return) {
                val raw = e.takeIf { it.isJsonPrimitive }?.asString ?: continue
                val m = IrcMessage.parse(raw) ?: continue
                if (m.command != "PRIVMSG") continue
                historicalMessage(m, raw)?.let { postMessage(it); count++ }
            }
            if (count > 0) {
                val ctx = contextRef?.get() ?: return
                postMessage(ChatMessage.Builder().text(ctx.getString(R.string.player_chat_history_end)).displayName("")
                    .notice(true).historical(true).build())
            }
            Log.d(TAG_EVENTSUB, "Chat history: $count messages")
        } catch (e: Exception) {
            Log.w(TAG_EVENTSUB, "Chat history: ${e.message}")
        }
    }

    /** A history line as a chat message (badges, Twitch and third-party emotes, 7TV paint). */
    private fun historicalMessage(m: IrcMessage, raw: String): ChatMessage? {
        val text = m.text ?: return null
        val login = m.nick
        val userId = m.tag("user-id")
        val emoteList = ArrayList<com.shrug.shrugtv.chat.emote.Emote>()
        emoteList.addAll(emoteManager.findChannelEmotes(text))
        emoteList.addAll(emoteManager.findGlobalEmotes(text))
        emoteList.addAll(emoteManager.parseTwitchEmotesTag(raw))
        val badges = m.badges
        val badgeImages = badges.mapNotNull { (set, version) -> emoteManager.badgeDrawable(set, version) }
        return ChatMessage.Builder()
            .text(text)
            .displayName(m.tag("display-name") ?: login ?: "")
            .chatterLogin(login)
            .color(m.tag("color") ?: "")
            .action(m.isAction)
            .badgeDrawables(badgeImages)
            .badges(badges.map { ChatBadge(it.first, it.second) })
            .mod(badges.any { it.first == "moderator" })
            .broadcaster(badges.any { it.first == "broadcaster" })
            .vip(badges.any { it.first == "vip" })
            .subscriber(badges.any { it.first == "subscriber" || it.first == "founder" })
            .messageId(m.tag("id"))
            .channelName(channelName)
            .slotId(slotId)
            .emotes(emoteList)
            .paint(if (extras.sevenTvPaints) cosmetics?.paintOf(userId) else null)
            .historical(true)
            .build()
    }

    private fun shutdownEmoteLoader() {
        emoteLoaderPool?.shutdown()
        emoteLoaderPool = null
    }

    override fun sendMessage(str: String) {
        try {
            val ctx = contextRef?.get()
            val senderId = AppState.userId
            if (senderId.isEmpty() || channelId <= 0) return
            HelixApi.sendChatMessage(ctx, "$channelId", senderId, str) { exc, obj ->
                // Main thread. Show why Twitch dropped the message (slow mode, followers-only, …)
                if (exc != null) {
                    val status = Http.StatusException.of(exc)
                    val reason = try {
                        status?.body?.let { JsonParser.parseString(it).asJsonObject.get("message")?.asString }
                    } catch (e: Exception) {
                        null
                    }
                    postNotice(R.string.player_chat_send_failed, reason ?: (status?.code?.let { "HTTP $it" } ?: (exc.message ?: "")))
                    return@sendChatMessage
                }
                val data = (obj as? JsonObject)?.getAsJsonArray("data")?.firstOrNull()?.asJsonObject
                if (data != null && data.get("is_sent")?.asBoolean == false) {
                    val reason = data.getAsJsonObject("drop_reason")?.get("message")?.asString ?: ""
                    postNotice(R.string.player_chat_send_failed, reason)
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    override fun stopChat() {
        running = false
        Log.d("ChatManager", "stopThread: $channelName")
        workerHandler?.removeCallbacks(watchdog)
        workerHandler?.removeCallbacks(reconnectRunnable)
        shutdownEmoteLoader()
        emoteLive?.stop()
        emoteLive = null

        // WebSocket subscriptions end with the session anyway; deleting them frees the quota at once
        val ctx = contextRef?.get()
        if (ctx != null) {
            for (id in subscriptionIds) HelixApi.deleteEventSubSubscription(ctx, id)
        }
        subscriptionIds.clear()
        sessionId?.let { deadSessions.add(it) }
        eventSubSocket?.close(1000, null)
        eventSubSocket = null
        pendingSocket?.cancel()
        pendingSocket = null
        sessionId = null

        messageProcessor?.quit()
        messageProcessor?.interrupt()

        quit()
        interrupt()
    }

    override fun connect() {
        val token = AppState.accessToken
        val userId = AppState.userId
        Log.i(TAG_EVENTSUB, "connect(): channelId=$channelId, hasToken=${token.isNotEmpty()}, userId=$userId")
        if (token.isEmpty()) {
            postNotice(R.string.player_chat_sign_in)
            return
        }
        if (userId.isEmpty()) {
            val ctx = contextRef?.get() ?: return
            com.shrug.shrugtv.session.UserSession.loadUserProfile(ctx, object : com.shrug.shrugtv.session.UserSession.ProfileCallback {
                override fun onError(str: String) {
                    Log.w(TAG_EVENTSUB, "Failed to load user profile: $str")
                    postNotice(R.string.player_chat_sign_in)
                }
                override fun onUnauthorized() {
                    postNotice(R.string.player_chat_sign_in)
                }
                override fun onEmptyResponse() {
                    if (AppState.userId.isNotEmpty() && running) {
                        Log.i(TAG_EVENTSUB, "User profile loaded: userId=${AppState.userId}, connecting EventSub")
                        workerHandler?.post { connectEventSub() }
                    } else {
                        postNotice(R.string.player_chat_sign_in)
                    }
                }
                override fun onRequestFailed() {
                    Log.w(TAG_EVENTSUB, "Profile request failed")
                    postNotice(R.string.player_chat_sign_in)
                }
            })
            return
        }
        connectEventSub()
    }

    /** Start of this client, for the timing lines in the log. */
    private var startedAtMs = 0L

    /** One line "ChatTiming": how long after the start a stage was reached; the log shows where a channel change waits. */
    private fun mark(stage: String) {
        if (startedAtMs > 0L) Log.i("ChatTiming", "$channelName: $stage after ${SystemClock.elapsedRealtime() - startedAtMs} ms")
    }

    override fun startChat() {
        startedAtMs = SystemClock.elapsedRealtime()
        Thread.currentThread().name = "Thread ChatManager $channelName"
        val pool = Executors.newFixedThreadPool(if (EmoteManager.globalEmotes.isEmpty()) 4 else 3)
        emoteLoaderPool = pool
        val tasks = ArrayList<Callable<String?>>()

        if (EmoteManager.globalEmotes.isEmpty()) {
            tasks.add(Callable {
                emoteManager.loadGlobalEmotes()
                null
            })
        }
        tasks.add(Callable {
            emoteManager.loadBadgeSets()
            null
        })
        tasks.add(Callable {
            emoteManager.loadBttvChannelEmotes()
            null
        })
        tasks.add(Callable {
            emoteManager.loadFfzChannelEmotes()
            null
        })
        tasks.add(Callable {
            emoteManager.loadSevenTvChannelEmotes()
            null
        })

        try {
            pool.invokeAll(tasks)
            shutdownEmoteLoader()
            emoteManager.mergeChannelEmotes()
            mark("emotes and badges loaded")
            if (!running) return
            loadRoomSettings()
            mark("room settings loaded")
            if (extras.recentMessages) loadRecentMessages()
            connect()
            mark("connect called")
            if (channelId > 0) {
                val live = EmoteLiveUpdates(emoteManager, channelId, cosmetics)
                emoteLive = live
                live.start()
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            shutdownEmoteLoader()
        }
    }
}
