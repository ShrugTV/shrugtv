package com.shrug.shrugtv.chat

import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import com.shrug.shrugtv.chat.emote.Emote


import com.shrug.shrugtv.chat.model.ChatSegment
import com.shrug.shrugtv.model.Enums
import java.util.regex.Pattern

/**
 * Represents a chat message (Twitch IRC or VOD comment).
 * Modernised in Kotlin for stage 2 (#108).
 */
class ChatMessage private constructor(builder: Builder) {

    val messageId: String? = builder.messageId
    val isHighlighted: Boolean = builder.highlighted
    val timestamp: String? = builder.timestamp
    private val rawText: String? = builder.text
    val displayName: String? = builder.displayName
    val color: String = builder.color
    val isMod: Boolean = builder.mod
    val isPartner: Boolean = builder.partner
    val isSubscriber: Boolean = builder.subscriber
    val isBroadcaster: Boolean = builder.broadcaster
    val isVip: Boolean = builder.vip
    val isPrime: Boolean = builder.prime
    val emotes: List<Emote>? = builder.emotes
    val subscriberBadge: Drawable? = builder.subscriberBadge
    val isNotice: Boolean = builder.notice
    val vodOffsetSeconds: Double = builder.vodOffsetSeconds
    val channelName: String? = builder.channelName
    val isAction: Boolean = builder.action
    val slotId: String? = builder.slotId
    val isHistorical: Boolean = builder.historical
    val noticeType: Enums.SubType? = builder.noticeType
    val noticeUser: String? = builder.noticeUser
    val giftRecipient: String? = builder.giftRecipient
    val rawLine: String? = builder.rawLine
    var formatted: SpannableStringBuilder? = builder.formatted

    // Stage 2 additions
    var reply: ChatReply? = builder.reply
    var badges: List<ChatBadge> = builder.badges
    var segments: List<ChatSegment> = builder.segments

    /** Login of the author (lower case); filters, mentions, timeouts work on it, not the display name. */
    val chatterLogin: String? = builder.chatterLogin

    /** Badge images in display order (global/channel badge sets); empty = use the boolean badges. */
    val badgeDrawables: List<Drawable> = builder.badgeDrawables

    /** Bits of a cheer, 0 for none. */
    val cheerBits: Int = builder.cheerBits

    /** 7TV name paint (gradient) of the author, null for none or when switched off. */
    val paint: com.shrug.shrugtv.chat.seventv.SevenTvPaint? = builder.paint

    /** 7TV badge of the author, shown after the Twitch badges. */
    val sevenTvBadge: Drawable? = builder.sevenTvBadge

    /**
     * Whole-row look set by the renderer: a bar on the left and a background over the full width
     * (notices, mentions, highlighted users/roles). Null = plain row.
     */
    var row: RowStyle? = null

    data class RowStyle(val bar: Int, val background: Int)

    private val commandPattern = Pattern.compile("^(\\s+)?!")
    private val botNamePattern = Pattern.compile("(?i)bot|b0t$")

    /**
     * The message text. Highlighted messages used to get a space on both sides, which moved every
     * emote image one character to the right (positions refer to the raw text).
     */
    val text: String?
        get() = rawText

    val isCommand: Boolean
        get() = rawText != null && commandPattern.matcher(rawText).find()

    val isBot: Boolean
        get() = displayName != null && botNamePattern.matcher(displayName).find() && isMod

    override fun toString(): String {
        return "ChatMessage{message='$rawText', name='$displayName', color='$color', mod=$isMod, sub=$isSubscriber}"
    }

    class Builder {
        var text: String? = null
            private set
        var displayName: String? = null
            private set
        var color: String = ""
            private set
        var mod: Boolean = false
            private set
        var partner: Boolean = false
            private set
        var subscriber: Boolean = false
            private set
        var broadcaster: Boolean = false
            private set
        var vip: Boolean = false
            private set
        var prime: Boolean = false
            private set
        var emotes: List<Emote>? = null
            private set
        var subscriberBadge: Drawable? = null
            private set
        var vodOffsetSeconds: Double = 0.0
            private set
        var channelName: String? = null
            private set
        var notice: Boolean = false
            private set
        var noticeType: Enums.SubType? = null
            private set
        var noticeUser: String? = null
            private set
        var giftRecipient: String? = null
            private set
        var messageId: String? = null
            private set
        var action: Boolean = false
            private set
        var formatted: SpannableStringBuilder? = null
            private set
        var slotId: String? = null
            private set
        var historical: Boolean = false
            private set
        var rawLine: String? = null
            private set
        var highlighted: Boolean = false
            private set
        var timestamp: String? = null
            private set
        var reply: ChatReply? = null
            private set
        var badges: List<ChatBadge> = emptyList()
            private set
        var segments: List<ChatSegment> = emptyList()
            private set
        var chatterLogin: String? = null
            private set
        var badgeDrawables: List<Drawable> = emptyList()
            private set
        var cheerBits: Int = 0
            private set
        var paint: com.shrug.shrugtv.chat.seventv.SevenTvPaint? = null
            private set
        var sevenTvBadge: Drawable? = null
            private set

        fun paint(p: com.shrug.shrugtv.chat.seventv.SevenTvPaint?) = apply { this.paint = p }
        fun sevenTvBadge(d: Drawable?) = apply { this.sevenTvBadge = d }

        fun chatterLogin(str: String?) = apply { this.chatterLogin = str?.lowercase() }
        fun badgeDrawables(list: List<Drawable>) = apply { this.badgeDrawables = list }
        fun cheerBits(bits: Int) = apply { this.cheerBits = bits }

        fun text(str: String?) = apply { this.text = str }
        fun displayName(str: String?) = apply { this.displayName = str }
        fun color(str: String?) = apply { this.color = str?.trim() ?: "" }
        fun mod(z: Boolean) = apply { this.mod = z }
        fun partner(z: Boolean) = apply { this.partner = z }
        fun subscriber(z: Boolean) = apply { this.subscriber = z }
        fun broadcaster(z: Boolean) = apply { this.broadcaster = z }
        fun vip(z: Boolean) = apply { this.vip = z }
        fun prime(z: Boolean) = apply { this.prime = z }
        fun emotes(list: List<Emote>?) = apply { this.emotes = list }
        fun subscriberBadge(drawable: Drawable?) = apply { this.subscriberBadge = drawable }
        fun vodOffsetSeconds(d: Double) = apply { this.vodOffsetSeconds = d }
        fun channelName(str: String?) = apply { this.channelName = str }
        fun notice(z: Boolean) = apply { this.notice = z }
        fun noticeType(type: Enums.SubType?) = apply {
            this.noticeType = type
            this.notice = true
        }
        fun noticeUser(str: String?) = apply {
            this.noticeUser = str
            this.notice = true
        }
        fun giftRecipient(str: String?) = apply {
            this.giftRecipient = str
            this.notice = true
        }
        fun messageId(str: String?) = apply { this.messageId = str }
        fun action(z: Boolean) = apply { this.action = z }
        fun formatted(ssb: SpannableStringBuilder?) = apply { this.formatted = ssb }
        fun slotId(str: String?) = apply { this.slotId = str }
        fun historical(z: Boolean) = apply { this.historical = z }
        fun rawLine(str: String?) = apply { this.rawLine = str }
        fun highlighted(z: Boolean) = apply { this.highlighted = z }
        fun timestamp(str: String?) = apply { this.timestamp = str }
        fun reply(r: ChatReply?) = apply { this.reply = r }
        fun badges(b: List<ChatBadge>) = apply { this.badges = b }
        fun segments(s: List<ChatSegment>) = apply { this.segments = s }

        fun systemMessage(z: Boolean) = apply {
            this.notice = z
            this.displayName = "SYSTEM"
            this.mod = true
            this.color = "#FF0000"
        }

        fun build(): ChatMessage = ChatMessage(this)
    }
}
