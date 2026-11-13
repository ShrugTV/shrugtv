package com.shrug.shrugtv.chat

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.shrug.shrugtv.chat.ui.ChatMessageRenderer
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.telemetry.CrashReporter
import java.lang.ref.WeakReference

/**
 * Filters and processes incoming chat messages on the background thread.
 * Optimised for stage 2 (#108): uses a pre-compiled regex pattern instead of Pattern.compile() per message.
 */
class ChatMessageProcessor(
    contextRef: WeakReference<Context>,
    private val uiSink: ChatUiSink?,
    fontSize: Int,
    lineHeight: Int,
    private val filterSettings: ChatFilterSettings?,
    private val messageDelayMs: Int,
    private val channelName: String,
    animated: Boolean,
    highlights: ChatHighlights = ChatHighlights.NONE,
    extras: ChatExtras = ChatExtras()
) : HandlerThread("ChatMessageConstructor") {

    @JvmField
    var handler: Handler

    private val renderer: ChatMessageRenderer = ChatMessageRenderer(contextRef, fontSize, lineHeight, animated, highlights, extras)

    private val spamGlyphs = arrayOf(
        "░", "⣿", "▤", "▊", "⠄", "⠈", "▧", "⢻", "⢿", "⣠", "⣴", "⣶", "⣀", "☒", "⌧", "▎", "▂", "▔"
    )

    init {
        start()
        val processorHandler = ProcessorHandler(looper, filterSettings, messageDelayMs)
        this.handler = processorHandler
        processorHandler.post {
            Thread.currentThread().name = "Thread ChatMessageConstructor $channelName"
        }
    }

    inner class ProcessorHandler(
        looper: Looper,
        private val filterSettings: ChatFilterSettings?,
        private val messageDelayMs: Int
    ) : Handler(looper) {

        override fun handleMessage(message: Message) {
            super.handleMessage(message)
            try {
                if (message.arg1 == 1) {
                    val chatMessage = message.obj as? ChatMessage ?: return
                    if (containsSpamGlyph(chatMessage)) return

                    if (filterSettings == null || !(isBlockedUser(chatMessage) ||
                                containsBlockedWord(chatMessage) ||
                                isFilteredSub(chatMessage) ||
                                isFilteredGiftSub(chatMessage) ||
                                isFilteredBot(chatMessage) ||
                                isFilteredCommand(chatMessage) ||
                                isFilteredModBot(chatMessage))
                    ) {
                        renderer.render(chatMessage)
                        // formatted check removed #137

                        val sink = uiSink ?: return

                        if (messageDelayMs != 0 && !chatMessage.isHistorical) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                sink.onEvent(ChatUiEvent.Message(chatMessage))
                            }, messageDelayMs.toLong())
                            return
                        }
                        sink.onEvent(ChatUiEvent.Message(chatMessage))
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    private fun containsSpamGlyph(chatMessage: ChatMessage): Boolean {
        val text = chatMessage.text ?: return false
        for (glyph in spamGlyphs) {
            if (text.contains(glyph)) return true
        }
        return false
    }

    private fun isFilteredCommand(chatMessage: ChatMessage): Boolean =
        filterSettings?.isFilterCommands == true && !chatMessage.isNotice && chatMessage.isCommand

    private fun isFilteredBot(chatMessage: ChatMessage): Boolean {
        if (filterSettings?.isFilterBots == true) {
            if (chatMessage.isCommand) return true
            val name = chatMessage.displayName
            if (name != null && KnownBots.bots.contains(name)) return true
        }
        return false
    }

    private fun isFilteredGiftSub(chatMessage: ChatMessage): Boolean {
        return filterSettings?.isShowGiftedSubs == false && chatMessage.noticeType == Enums.SubType.GIFTED_SUB
    }

    private fun isFilteredModBot(chatMessage: ChatMessage): Boolean {
        return filterSettings?.isFilterModBots == true && !chatMessage.isNotice && chatMessage.isBot
    }

    private fun isFilteredSub(chatMessage: ChatMessage): Boolean {
        return filterSettings?.isShowSubMessages == false && chatMessage.noticeType == Enums.SubType.NORMAL_SUB
    }

    private fun isBlockedUser(chatMessage: ChatMessage): Boolean {
        val blocked = filterSettings?.blockedUsers ?: return false
        val name = chatMessage.displayName ?: return false
        if (chatMessage.isNotice) return false
        for (user in blocked) {
            if (name.equals(user, ignoreCase = true)) return true
        }
        return false
    }

    private fun containsBlockedWord(chatMessage: ChatMessage): Boolean {
        if (chatMessage.isNotice) return false
        val text = chatMessage.text ?: return false
        // Uses the pre-compiled pattern from ChatFilterSettings instead of Pattern.compile() per message
        return filterSettings?.compiledPattern?.matcher(text)?.find() == true
    }

    override fun quit(): Boolean {
        handler = Handler(Looper.getMainLooper())
        return super.quit()
    }

    override fun quitSafely(): Boolean {
        handler = Handler(Looper.getMainLooper())
        return super.quitSafely()
    }
}
