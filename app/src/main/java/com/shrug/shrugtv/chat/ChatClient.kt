package com.shrug.shrugtv.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.shrug.shrugtv.chat.emote.EmoteManager
import com.shrug.shrugtv.model.RaidNotification
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base of the chat clients (live: EventSub, recording: GQL comments): the worker thread, the
 * message processor and the connection to the player. The old IRC code is gone (
 * #147); events reach the player through a typed [ChatUiSink].
 */
abstract class ChatClient(builder: Builder) : HandlerThread("ChatManager") {

    class Builder {
        var contextRef: WeakReference<Context>? = null
            private set
        var uiSink: ChatUiSink? = null
            private set
        var channelName: String? = null
            private set
        var channelId: Int = 0
            private set
        var badgeSize: Int = 0
            private set
        var emoteSize: Int = 0
            private set
        var filterSettings: ChatFilterSettings? = null
            private set
        var messageDelayMs: Int = 0
            private set
        var showPrimeBadge: Boolean = false
            private set
        var vodId: Long = 0
            private set
        var animatedEmotes: Boolean = false
            private set
        var slotId: String? = null
            private set
        var loadRecentMessages: Boolean = false
            private set
        var timestampFormat: String? = null
            private set
        var highlights: ChatHighlights = ChatHighlights.NONE
            private set

        fun highlights(h: ChatHighlights) = apply { this.highlights = h }

        var extras: ChatExtras = ChatExtras()
            private set

        fun extras(e: ChatExtras) = apply { this.extras = e }

        fun context(context: Context) = apply { this.contextRef = WeakReference(context) }
        fun uiSink(sink: ChatUiSink) = apply { this.uiSink = sink }
        fun channelName(name: String?) = apply { this.channelName = name }
        fun channelId(id: Int) = apply { this.channelId = id }
        fun badgeSize(size: Int) = apply { this.badgeSize = size }
        fun emoteSize(size: Int) = apply { this.emoteSize = size }
        fun filterSettings(settings: ChatFilterSettings?) = apply { this.filterSettings = settings }
        fun messageDelayMs(delay: Int) = apply { this.messageDelayMs = delay }
        fun showPrimeBadge(show: Boolean) = apply { this.showPrimeBadge = show }
        fun vodId(id: Long) = apply { this.vodId = id }
        fun animatedEmotes(animated: Boolean) = apply { this.animatedEmotes = animated }
        fun slotId(id: String?) = apply { this.slotId = id }
        fun loadRecentMessages(load: Boolean) = apply { this.loadRecentMessages = load }
        fun timestampFormat(fmt: String?) = apply { this.timestampFormat = fmt }

        // begin() only after construction: the subclass fields must be initialised before the chat
        // thread starts working with them
        fun buildVodChat(): VodChatClient = VodChatClient(this).apply { begin() }
        fun buildLiveChat(): LiveChatClient = LiveChatClient(this).apply { begin() }
    }

    @JvmField
    var channelId: Int = builder.channelId

    @JvmField
    var channelName: String? = builder.channelName

    @JvmField
    var vodId: Long = builder.vodId

    @JvmField
    var slotId: String? = builder.slotId

    @JvmField
    var badgeSize: Int = builder.badgeSize

    @JvmField
    var emoteSize: Int = builder.emoteSize

    @JvmField
    var animatedEmotes: Boolean = builder.animatedEmotes

    @JvmField
    var loadRecentMessages: Boolean = builder.loadRecentMessages

    @JvmField
    var timestampFormat: String? = builder.timestampFormat

    @JvmField
    var contextRef: WeakReference<Context>? = builder.contextRef

    /** Where events for the player go; held strongly, unlike the old weakly referenced handler. */
    @JvmField
    var uiSink: ChatUiSink? = builder.uiSink

    /** Chat features from the 7TV extension (providers, zero-width, 7TV cosmetics, history). */
    val extras: ChatExtras = builder.extras

    @JvmField
    var emoteManager: EmoteManager

    @JvmField
    var stopRequested: AtomicBoolean = AtomicBoolean(false)

    @JvmField
    var subscriberBadges: HashMap<String, Drawable>? = null

    @JvmField
    var workerHandler: Handler? = null

    @JvmField
    var messageProcessor: ChatMessageProcessor? = null

    init {
        emoteManager = EmoteManager(channelId, channelName, contextRef, badgeSize, emoteSize, animatedEmotes, builder.extras)
        start()
        workerHandler = Handler(looper)
        messageProcessor = ChatMessageProcessor(
            contextRef ?: WeakReference(null),
            uiSink,
            badgeSize,
            emoteSize,
            builder.filterSettings,
            builder.messageDelayMs,
            channelName ?: "",
            builder.showPrimeBadge,
            builder.highlights,
            builder.extras
        )
    }

    /**
     * Starts the chat on its thread. Not in init: Kotlin initialises a subclass's fields only after
     * this init block, so the chat thread could run before e.g. LiveChatClient's socket field was
     * set, and its later initialiser would then overwrite the open socket with null (the welcome was
     * ignored, nothing subscribed, the chat stayed empty without any notice).
     */
    fun begin() {
        post(Runnable { startChat() })
    }

    abstract fun startChat()

    open fun stopChat() {
        stopRequested.set(true)
        Log.d("ChatManager", "stopThread: $channelName")
    }

    /** Sending is the live client's business; recordings are read only. */
    open fun sendMessage(str: String) {}

    /** Loads the comments around [offsetSeconds] of a recording (on the chat thread). */
    fun requestVodMessages(offsetSeconds: Long) {
        post(Runnable {
            Log.d("ChatManager", "requestVodMessages: $offsetSeconds")
            fetchVodMessages(offsetSeconds)
        })
    }

    protected open fun fetchVodMessages(offsetSeconds: Long) {}

    fun post(runnable: Runnable): ChatClient {
        workerHandler?.post(runnable)
        return this
    }

    /** A message to filter and render; the processor hands the result to the sink. */
    fun postMessage(chatMessage: ChatMessage) {
        val messageObtain = Message.obtain().apply {
            obj = chatMessage
            arg1 = 1
        }
        messageProcessor?.handler?.sendMessage(messageObtain)
    }

    fun postRaidEvent(raidNotification: RaidNotification) {
        uiSink?.onEvent(ChatUiEvent.Raid(raidNotification))
    }

    /** Connects to the chat service; the live client overrides this. */
    protected open fun connect() {}
}
