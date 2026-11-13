package com.shrug.shrugtv.chat.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.shrug.shrugtv.R
import com.shrug.shrugtv.chat.ChatHighlights
import com.shrug.shrugtv.chat.ChatMessage
import com.shrug.shrugtv.chat.emote.Emote
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import androidx.compose.ui.graphics.toArgb
import com.shrug.shrugtv.ui.tv.theme.ThemeSettings
import pl.droidsonroids.gif.GifDrawable
import java.lang.ref.WeakReference
import java.util.Random

/**
 * Modernisierter ChatMessageRenderer in Kotlin (Stage 2, #108).
 * Builds a SpannableStringBuilder for badges, name, text and animated/static emotes.
 */
class ChatMessageRenderer(
    private val contextRef: WeakReference<Context>,
    private val badgeSize: Int,
    private val emoteSize: Int,
    private val showPrimeBadge: Boolean,
    private val highlights: ChatHighlights = ChatHighlights.NONE,
    private val extras: com.shrug.shrugtv.chat.ChatExtras = com.shrug.shrugtv.chat.ChatExtras()
) {
    companion object {
        /** How much of the answered message is shown after "↪ @name: ": [chars] in all with the name, at least 10 for the message. */
        @JvmStatic
        fun replyBodyRoom(authorName: String, chars: Int = com.shrug.shrugtv.chat.ChatPrefs.REPLY_CHARS_DEFAULT): Int =
            (chars - authorName.length).coerceAtLeast(10)

        /** Accent of notices (sub, raid, announcement) and highlighted users. */
        private const val ACCENT = 0xFF9F7AEA.toInt()
        private const val NOTICE_TEXT = 0xFFD6CCFF.toInt()
        private const val MUTED = 0xFFADADB8.toInt()
        /** Mentions of the signed-in user: Twitch-like red. */
        private const val MENTION = 0xFFF0526C.toInt()
        /** Messages highlighted with channel points: Twitch purple. */
        private const val CHANNEL_POINTS = 0xFF9147FF.toInt()
    }

    private val modBadgeSpan: ImageSpan?
    private val partnerBadgeSpan: ImageSpan?
    private val broadcasterBadgeSpan: ImageSpan?
    private val vipBadgeSpan: ImageSpan?
    private val primeBadgeSpan: ImageSpan?
    private var subscriberBadgeSpan: ImageSpan? = null

    private val darkBackgroundColor: Int
    private val mentionHighlightColor: Int
    private val nameColorMode: NameColorMode


    init {
        val ctx = contextRef.get()
        val modDrawable = ctx?.let { ContextCompat.getDrawable(it, R.drawable.ic_moderator) }?.apply { setBounds(0, 0, badgeSize, badgeSize) }
        val partnerDrawable = ctx?.let { ContextCompat.getDrawable(it, R.drawable.verified) }?.apply { setBounds(0, 0, badgeSize, badgeSize) }
        val broadcasterDrawable = ctx?.let { ContextCompat.getDrawable(it, R.drawable.broadcaster) }?.apply { setBounds(0, 0, badgeSize, badgeSize) }
        val vipDrawable = ctx?.let { ContextCompat.getDrawable(it, R.drawable.vip) }?.apply { setBounds(0, 0, badgeSize, badgeSize) }
        val primeDrawable = ctx?.let { ContextCompat.getDrawable(it, R.drawable.prime) }?.apply { setBounds(0, 0, badgeSize, badgeSize) }

        modBadgeSpan = modDrawable?.let { CenteredImageSpan(it) }
        partnerBadgeSpan = partnerDrawable?.let { CenteredImageSpan(it) }
        broadcasterBadgeSpan = broadcasterDrawable?.let { CenteredImageSpan(it) }
        vipBadgeSpan = vipDrawable?.let { CenteredImageSpan(it) }
        primeBadgeSpan = primeDrawable?.let { CenteredImageSpan(it) }

        // Colours of the theme in effect; the renderer is built per chat, so a change applies
        // from the next chat on
        val colors = ThemeSettings.theme.value.colors
        darkBackgroundColor = colors.surface.toArgb()
        mentionHighlightColor = colors.accent.toArgb()
        nameColorMode = NameColorMode.of(ctx?.let {
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(it).getString("pref_chat_name_colors", null)
        })
    }

    private fun renderGiftSub(chatMessage: ChatMessage): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        val text = chatMessage.text ?: return ssb
        val ctx = contextRef.get()
        try {
            val colorUser = ForegroundColorSpan(Color.parseColor("#18fbff"))
            val colorRecipient = ForegroundColorSpan(Color.parseColor("#ff5218"))
            val span = SpannableString(text)
            val greyColor = if (ctx != null) ContextCompat.getColor(ctx, R.color.lb_grey) else Color.GRAY
            span.setSpan(ForegroundColorSpan(greyColor), 0, span.length, 33)

            val noticeUser = chatMessage.noticeUser
            if (noticeUser != null) {
                val idx = text.indexOf(noticeUser)
                if (idx != -1) {
                    span.setSpan(colorUser, idx, idx + noticeUser.length, 33)
                } else if (text.contains("An anonymous user gifted")) {
                    val anonIdx = text.indexOf("anonymous")
                    if (anonIdx != -1) {
                        span.setSpan(colorUser, anonIdx, anonIdx + 9, 33)
                    }
                }
            }
            val recipient = chatMessage.giftRecipient
            if (!recipient.isNullOrEmpty()) {
                val rIdx = text.indexOf(recipient)
                if (rIdx != -1) {
                    span.setSpan(colorRecipient, rIdx, rIdx + recipient.length, 33)
                }
            }
            ssb.append(span)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return ssb
    }

    private fun renderSub(chatMessage: ChatMessage): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        val text = chatMessage.text ?: return ssb
        val ctx = contextRef.get()
        try {
            val span = SpannableString(text)
            val greyColor = if (ctx != null) ContextCompat.getColor(ctx, R.color.lb_grey) else Color.GRAY
            span.setSpan(ForegroundColorSpan(greyColor), 0, span.length, 33)

            val noticeUser = chatMessage.noticeUser
            if (noticeUser != null) {
                val idx = text.indexOf(noticeUser)
                if (idx != -1) {
                    span.setSpan(ForegroundColorSpan(Color.WHITE), idx, idx + noticeUser.length, 33)
                }
                val lastIdx = text.lastIndexOf(noticeUser)
                if (lastIdx != -1) {
                    span.setSpan(ForegroundColorSpan(Color.WHITE), lastIdx, lastIdx + noticeUser.length, 33)
                }
            }
            ssb.append(span)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return ssb
    }

    private fun getTextColor(chatMessage: ChatMessage): Int {
        val ctx = contextRef.get()
        val defaultWhite = if (ctx != null) ContextCompat.getColor(ctx, R.color.white) else Color.WHITE
        if (!chatMessage.isAction || chatMessage.color.isEmpty()) {
            return defaultWhite
        }
        return try {
            Color.parseColor(chatMessage.color)
        } catch (e: Exception) {
            defaultWhite
        }
    }

    /** Name colour by the setting `pref_chat_name_colors`; the readable default needs 4.5:1 on the panel. */
    private fun getUserColor(chatMessage: ChatMessage): Int {
        val base = ChatColors.parse(chatMessage.color) ?: ChatColors.fallback(chatMessage.displayName ?: "")
        return when (nameColorMode) {
            NameColorMode.RAW -> base
            NameColorMode.READABLE -> ChatColors.readableOn(base, darkBackgroundColor)
            NameColorMode.ACCENT -> ChatColors.readableOn(mentionHighlightColor, darkBackgroundColor)
        }
    }

    private fun renderNotice(chatMessage: ChatMessage): SpannableStringBuilder {
        val text = chatMessage.text ?: ""
        val ssb = SpannableStringBuilder(text)
        ssb.setSpan(ForegroundColorSpan(NOTICE_TEXT), 0, ssb.length, 33)
        val user = chatMessage.noticeUser
        if (!user.isNullOrEmpty()) {
            val i = text.indexOf(user)
            if (i >= 0) ssb.setSpan(StyleSpan(1), i, i + user.length, 33)
        }
        return ssb
    }

    private fun scaleWidth(origWidth: Int, origHeight: Int, targetHeight: Int): Int {
        return if (origHeight <= 0) targetHeight else (targetHeight * origWidth) / origHeight
    }

    private class Placed(val start: Int, val end: Int, val drawable: Drawable, val zeroWidth: Boolean)

    /**
     * Emote images into the text. Zero-width emotes (overlays) that follow another emote with only
     * spaces between are drawn over it, their code hidden (setting "Zero-width emotes", like 7TV).
     */
    private fun placeEmotes(textSsb: SpannableStringBuilder, text: String, emotes: List<Emote>) {
        val placed = ArrayList<Placed>()
        for (emote in emotes) {
            val d: Drawable = (if (emote.isGif) emote.gifDrawable else null) ?: emote.drawable ?: continue
            for (pos in emote.positions) {
                val parts = pos.split("-")
                if (parts.size != 2) continue
                val start = parts[0].toIntOrNull() ?: continue
                val end = parts[1].toIntOrNull() ?: continue
                if (start !in 0 until textSsb.length || end < start) continue
                placed.add(Placed(start, (end + 1).coerceAtMost(textSsb.length), d, emote.isZeroWidth && extras.zeroWidth))
            }
        }
        placed.sortBy { it.start }
        // Groups: a base emote followed by its overlays
        val groups = ArrayList<MutableList<Placed>>()
        for (p in placed) {
            val last = groups.lastOrNull()
            val gapIsSpace = last != null && last.last().end <= p.start && text.substring(last.last().end, p.start).isBlank()
            if (p.zeroWidth && last != null && gapIsSpace) last.add(p) else groups.add(mutableListOf(p))
        }
        for (g in groups) {
            try {
                val base = g.first()
                val drawable = if (g.size == 1) base.drawable else StackedDrawable(g.map { it.drawable })
                val w = scaleWidth(base.drawable.intrinsicWidth, base.drawable.intrinsicHeight, emoteSize)
                val span = CenteredImageSpan(drawable)
                span.drawable.setBounds(0, 0, w, emoteSize)
                textSsb.setSpan(span, base.start, base.end, 33)
                // Hide the overlays' codes and the spaces before them
                if (g.size > 1) textSsb.setSpan(EmptySpan(), base.end, g.last().end, 33)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }

    fun render(chatMessage: ChatMessage): ChatMessage {
        val ctx = contextRef.get()
        if (chatMessage.isNotice) {
            // Subs, gifts, raids, announcements: one notice style (docs/chat_plan.md 2.1)
            chatMessage.formatted = renderNotice(chatMessage)
            chatMessage.row = ChatMessage.RowStyle(ACCENT, 0x269F7AEA)
            return chatMessage
        }

        if (chatMessage.isSubscriber && chatMessage.subscriberBadge != null) {
            val badgeDrawable = chatMessage.subscriberBadge
            badgeDrawable.setBounds(0, 0, badgeSize, badgeSize)
            subscriberBadgeSpan = CenteredImageSpan(badgeDrawable)
        }

        val ssb = SpannableStringBuilder()
        // Reply: a small line with the message it answers (docs/chat_plan.md 2.5)
        chatMessage.reply?.let { reply ->
            // The message that is answered: room for about two lines of the panel, then cut
            val room = replyBodyRoom(reply.parentDisplayName, extras.replyChars)
            val body = reply.parentMsgBody.let { if (it.length > room) it.take(room - 1).trimEnd() + "…" else it }
            val line = SpannableStringBuilder("↪ @${reply.parentDisplayName}: $body\n")
            line.setSpan(ForegroundColorSpan(MUTED), 0, line.length, 33)
            line.setSpan(RelativeSizeSpan(0.85f), 0, line.length, 33)
            ssb.append(line)
        }
        val timestamp = chatMessage.timestamp
        if (!timestamp.isNullOrEmpty()) {
            val timeSsb = SpannableStringBuilder("$timestamp  ")
            timeSsb.setSpan(ForegroundColorSpan(Color.parseColor("#ADADB8")), 0, timeSsb.length, 33)
            timeSsb.setSpan(AbsoluteSizeSpan(12, true), 0, timeSsb.length, 33)
            ssb.append(timeSsb)
        }

        if (!extras.showBadges) {
            // Compact chat without badges (setting "Show badges")
        } else if (chatMessage.badgeDrawables.isNotEmpty()) {
            // Real badges from the global and channel badge sets
            for (d in chatMessage.badgeDrawables) {
                d.setBounds(0, 0, scaleWidth(d.intrinsicWidth, d.intrinsicHeight, badgeSize), badgeSize)
                ssb.append("  ").setSpan(CenteredImageSpan(d), ssb.length - 2, ssb.length - 1, 0)
            }
        } else {
        if (chatMessage.isPartner && partnerBadgeSpan != null) {
            ssb.append("  ").setSpan(partnerBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        if (chatMessage.isVip && vipBadgeSpan != null) {
            ssb.append("  ").setSpan(vipBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        if (chatMessage.isMod && modBadgeSpan != null) {
            ssb.append("  ").setSpan(modBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        if (chatMessage.isSubscriber && subscriberBadgeSpan != null) {
            ssb.append("  ").setSpan(subscriberBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        if (chatMessage.isPrime && showPrimeBadge && primeBadgeSpan != null) {
            ssb.append("  ").setSpan(primeBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        if (chatMessage.isBroadcaster && broadcasterBadgeSpan != null) {
            ssb.append("  ").setSpan(broadcasterBadgeSpan, ssb.length - 2, ssb.length - 1, 0)
        }
        }
        // 7TV badge after the Twitch badges (only set when "7TV badges" is on)
        if (extras.showBadges) chatMessage.sevenTvBadge?.let { d ->
            d.setBounds(0, 0, scaleWidth(d.intrinsicWidth, d.intrinsicHeight, badgeSize), badgeSize)
            ssb.append("  ").setSpan(CenteredImageSpan(d), ssb.length - 2, ssb.length - 1, 0)
        }

        val displayName = chatMessage.displayName ?: return chatMessage
        if (chatMessage.rawLine != null) {
            CrashReporter.setCustomKey("chat_message", chatMessage.rawLine)
        }

        val separator = if (chatMessage.isAction) " " else ": "
        val userColor = getUserColor(chatMessage)

        val nameSsb = SpannableStringBuilder(displayName)
        nameSsb.setSpan(ForegroundColorSpan(userColor), 0, nameSsb.length, 33)
        nameSsb.setSpan(StyleSpan(1), 0, nameSsb.length, 33)
        // 7TV paint: the name drawn with its gradient (setting "7TV name colours")
        chatMessage.paint?.let { nameSsb.setSpan(PaintSpan(it), 0, nameSsb.length, 33) }
        ssb.append(nameSsb)

        if (chatMessage.cheerBits > 0) {
            val cheer = SpannableStringBuilder(" ◆${chatMessage.cheerBits}")
            cheer.setSpan(ForegroundColorSpan(ACCENT), 0, cheer.length, 33)
            cheer.setSpan(StyleSpan(1), 0, cheer.length, 33)
            ssb.append(cheer)
        }

        val sepSsb = SpannableStringBuilder(separator)
        sepSsb.setSpan(ForegroundColorSpan(userColor), 0, separator.length, 33)
        ssb.append(sepSsb)

        val messageText = chatMessage.text ?: ""
        val textSsb = SpannableStringBuilder(messageText)
        textSsb.setSpan(ForegroundColorSpan(getTextColor(chatMessage)), 0, textSsb.length, 33)

        chatMessage.emotes?.takeIf { it.isNotEmpty() }?.let { placeEmotes(textSsb, messageText, it) }

        // Own highlight words (docs/chat_plan.md 2.6a), in the highlight colour
        val wordBackground = (highlights.color and 0x00FFFFFF) or 0x66000000
        for (r in highlights.wordRanges(messageText)) {
            textSsb.setSpan(BackgroundColorSpan(wordBackground), r.first, r.last + 1, 33)
        }
        // Mentions by login or display name, case-insensitive (2.6); accent bar instead of a
        // full-line colour with white text
        val mentioned = AppState.userProfileLoaded &&
            ChatHighlights.mentions(messageText, AppState.userLogin, AppState.userDisplayName)
        val userHighlighted = highlights.isHighlightedUser(chatMessage.chatterLogin, chatMessage.displayName) ||
            highlights.isHighlightedRole(chatMessage.isBroadcaster, chatMessage.isMod, chatMessage.isVip)

        ssb.append(textSsb)
        // Whole-row highlights (drawn by the chat list over the full width, not behind the glyphs)
        chatMessage.row = when {
            mentioned -> ChatMessage.RowStyle(MENTION, (MENTION and 0x00FFFFFF) or 0x33000000)
            chatMessage.isHighlighted -> ChatMessage.RowStyle(CHANNEL_POINTS, (CHANNEL_POINTS and 0x00FFFFFF) or 0x33000000)
            userHighlighted -> ChatMessage.RowStyle(highlights.color, (highlights.color and 0x00FFFFFF) or 0x26000000)
            else -> null
        }
        chatMessage.formatted = ssb
        return chatMessage
    }
}
