package com.shrug.shrugtv.ui.tv.player

import android.graphics.Typeface
import com.shrug.shrugtv.ui.tv.TvColors
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.eventSub.ChatEvent
import com.shrug.shrugtv.ui.tv.Inter

/**
 * Chat panel of the player (docs/chat_plan.md phase 3).
 *
 * A RecyclerView with a diffed list: new lines are appended, lines deleted by a moderator are
 * redrawn in place, and only visible lines exist as views. The text is the SpannableStringBuilder
 * pre-formatted by ChatMessageRenderer (badges, colours, emote images). Above the list a status line
 * shows the room state (slow mode, follower-only …).
 */
@Composable
fun ChatPanel(
    visible: Boolean,
    lines: List<ChatLine>,
    modifier: Modifier = Modifier,
    roomSettings: ChatEvent.RoomSettings? = null,
    textSizeSp: Float = 14f,
    background: Color = Color.Black.copy(alpha = 0.55f),
    /** Transparent layout: text shadow so it stays readable on any picture. */
    textShadow: Boolean = false,
    /** Side the panel slides in from. */
    fromLeft: Boolean = false,
    /** Alternating rows, separators, compact spacing (chat settings). */
    look: com.shrug.shrugtv.chat.ChatLook = com.shrug.shrugtv.chat.ChatLook()
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally { if (fromLeft) -it / 3 else it / 3 },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally { if (fromLeft) -it / 3 else it / 3 },
        modifier = modifier
    ) {
        ChatPanelContent(lines, roomSettings, textSizeSp, background, textShadow, look)
    }
}

@Composable
private fun ChatPanelContent(
    lines: List<ChatLine>,
    roomSettings: ChatEvent.RoomSettings?,
    textSizeSp: Float,
    background: Color,
    textShadow: Boolean,
    look: com.shrug.shrugtv.chat.ChatLook
) {
    // A new text size or look needs a new list and adapter. AndroidView calls its factory only once, so
    // without this key the list on the screen kept its old adapter while the lines went to the new one:
    // the chat stood still after the text size was changed in "Adjust chat"
    androidx.compose.runtime.key(textSizeSp, textShadow, look) {
        ChatPanelList(lines, roomSettings, textSizeSp, background, textShadow, look)
    }
}

@Composable
private fun ChatPanelList(
    lines: List<ChatLine>,
    roomSettings: ChatEvent.RoomSettings?,
    textSizeSp: Float,
    background: Color,
    textShadow: Boolean,
    look: com.shrug.shrugtv.chat.ChatLook
) {
    val ctx = LocalContext.current
    val removedText = stringResource(R.string.player_chat_message_removed)
    val adapter = remember { ChatLineAdapter(textSizeSp, removedText, textShadow, look) }
    val recycler = remember {
        RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx).apply { stackFromEnd = true }
            // New messages slide or fade in as set; old lines dropping out at the top and
            // changed lines (e.g. struck through) do not animate, so nothing flickers
            itemAnimator = if (look.animation == com.shrug.shrugtv.chat.ChatAnimation.OFF) null else
                androidx.recyclerview.widget.DefaultItemAnimator().apply {
                    addDuration = look.animation.addMs
                    moveDuration = look.animation.moveMs
                    removeDuration = 0L
                    changeDuration = 0L
                    supportsChangeAnimations = false
                }
            isFocusable = false
            this.adapter = adapter
            if (look.separators) addItemDecoration(SeparatorDecoration(ctx.resources.displayMetrics.density))
        }
    }

    // Rows bring their own side padding (room for the highlight bar), so the panel only adds a little
    Column(modifier = Modifier.fillMaxSize().background(background).padding(horizontal = 4.dp, vertical = 12.dp)) {
        val status = roomStatusText(roomSettings)
        if (status != null) {
            Text(
                text = status,
                fontFamily = Inter,
                fontSize = 12.sp,
                color = TvColors.glass(0.7f),
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 6.dp)
            )
        }
        AndroidView(
            factory = { recycler },
            // The list scrolls rows past its top edge; without clipping they are drawn over the status line
            modifier = Modifier.fillMaxSize().clipToBounds(),
            update = { rv ->
                val list = lines
                adapter.submitList(list) {
                    if (list.isNotEmpty()) rv.scrollToPosition(list.size - 1)
                }
            }
        )
    }
}

@Composable
private fun roomStatusText(s: ChatEvent.RoomSettings?): String? {
    if (s == null || !s.isAnyActive) return null
    val parts = ArrayList<String>()
    s.slowSeconds?.let { parts.add(stringResource(R.string.player_chat_slow_mode, it)) }
    s.followersOnlyMinutes?.let { parts.add(stringResource(R.string.player_chat_followers_only)) }
    if (s.subsOnly) parts.add(stringResource(R.string.player_chat_subs_only))
    if (s.emoteOnly) parts.add(stringResource(R.string.player_chat_emote_only))
    if (s.uniqueChat) parts.add(stringResource(R.string.player_chat_unique))
    return parts.joinToString(" · ")
}

/**
 * Background of a highlighted row over the full width: light colour with rounded corners and a bar
 * on the left (notices, mentions, highlighted users and roles). Replaces the old background behind
 * the glyphs only, which looked ragged on wrapped lines.
 */
private class RowBackground(private val bar: Int, private val fill: Int, density: Float) : android.graphics.drawable.Drawable() {
    private val radius = 5 * density
    private val barWidth = 3 * density
    private val inset = density
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val rect = android.graphics.RectF()

    override fun draw(canvas: android.graphics.Canvas) {
        val b = bounds
        rect.set(b.left.toFloat(), b.top + inset, b.right.toFloat(), b.bottom - inset)
        paint.color = fill
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.color = bar
        rect.set(b.left.toFloat(), b.top + inset, b.left + barWidth, b.bottom - inset)
        canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

/** Thin line under every message (setting "Separator lines", like 7TV). */
private class SeparatorDecoration(density: Float) : RecyclerView.ItemDecoration() {
    private val paint = android.graphics.Paint().apply { color = 0x24FFFFFF; strokeWidth = maxOf(1f, density * 0.75f) }

    override fun onDrawOver(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val v = parent.getChildAt(i)
            val y = v.bottom + v.translationY
            c.drawLine(v.left.toFloat(), y, v.right.toFloat(), y, paint)
        }
    }
}

private class ChatLineAdapter(
    private val textSizeSp: Float,
    private val removedText: String,
    private val textShadow: Boolean,
    private val look: com.shrug.shrugtv.chat.ChatLook = com.shrug.shrugtv.chat.ChatLook()
) : ListAdapter<ChatLine, ChatLineAdapter.Holder>(DIFF) {

    class Holder(val text: TextView) : RecyclerView.ViewHolder(text) {
        /** Animated emotes of the row on the screen; stopped when the row is recycled or rebound. */
        var animated: List<Animatable> = emptyList()
    }

    private val timeOfDay = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val ctx = parent.context
        val tv = TextView(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(android.graphics.Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            // Compact: less spacing, more messages on the screen (setting "Compact chat")
            setLineSpacing(0f, if (look.compact) 1.0f else 1.15f)
            val density = ctx.resources.displayMetrics.density
            val vertical = ((if (look.compact) 2 else 4) * density).toInt()
            // Same indent for every row: highlighted rows draw their bar in it, text stays aligned
            setPadding((10 * density).toInt(), vertical, (8 * density).toInt(), vertical)
            typeface = ResourcesCompat.getFont(ctx, R.font.inter_regular)
            isFocusable = false
            if (textShadow) setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
        return Holder(tv)
    }

    override fun onViewRecycled(holder: Holder) {
        stopAnimatedEmotes(holder)
    }

    override fun onViewDetachedFromWindow(holder: Holder) {
        // Rows outside the screen draw nothing, so they must not keep animating
        holder.animated.forEach { it.stop() }
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        holder.animated.forEach { if (!it.isRunning) it.start() }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        stopAnimatedEmotes(holder)
        val line = getItem(position)
        val m = line.message
        val density = holder.text.resources.displayMetrics.density
        val row = m.row.takeIf { !line.deleted }
        holder.text.background = when {
            row != null -> RowBackground(row.bar, row.background, density)
            // Alternating rows by message id, so the pattern does not shift when new lines arrive
            look.alternate && line.id % 2L == 1L -> android.graphics.drawable.ColorDrawable(0x14FFFFFF)
            else -> null
        }
        // Messages from the chat history are a little dimmer than live ones
        holder.text.alpha = when {
            line.deleted && look.deletedStyle == com.shrug.shrugtv.chat.DeletedStyle.STRIKE -> 0.65f
            m.isHistorical && !m.isNotice -> 0.7f
            else -> 1f
        }
        val original = m.formatted ?: SpannableStringBuilder("${m.displayName ?: "?"}: ${m.text ?: ""}")
        val body: CharSequence = when {
            // Setting "Deleted messages": struck through (the default), grey, hidden ("message removed") or as sent
            line.deleted && look.deletedStyle == com.shrug.shrugtv.chat.DeletedStyle.HIDE -> SpannableStringBuilder("${m.displayName ?: ""}: $removedText").apply {
                setSpan(ForegroundColorSpan(0xFF8A8A94.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            line.deleted && look.deletedStyle == com.shrug.shrugtv.chat.DeletedStyle.GREY -> SpannableStringBuilder(original).apply {
                setSpan(ForegroundColorSpan(0xFF8A8A94.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            line.deleted && look.deletedStyle == com.shrug.shrugtv.chat.DeletedStyle.STRIKE -> SpannableStringBuilder(original).apply {
                setSpan(android.text.style.StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            else -> original
        }.let { b ->
            // Merged repetitions: the counter behind the first one
            if (line.repeat <= 1) b else SpannableStringBuilder(b).apply {
                val start = length
                append("  \u00D7${line.repeat}")
                setSpan(ForegroundColorSpan(0xFFBF94FF.toInt()), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        holder.text.text = if (look.timestamps && !m.isNotice) {
            // Recordings show the position in the video, live chat the time of day
            val stamp = if (m.vodOffsetSeconds > 0) com.shrug.shrugtv.util.TimeUtil.hms(m.vodOffsetSeconds.toLong()) else timeOfDay.format(java.util.Date(line.wallMs))
            SpannableStringBuilder("$stamp ").apply {
                setSpan(ForegroundColorSpan(0xFF8A8A94.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.RelativeSizeSpan(0.8f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(body)
            }
        } else body
        startAnimatedEmotes(holder)
    }

    /**
     * GIF/WebP emotes animate only with a callback that redraws the TextView. Before, nothing ever stopped
     * them: every row that was recycled or had scrolled away kept its emotes running and redrawing a
     * TextView that showed another message by now, so the more messages had passed, the slower the
     * visible emotes became.
     */
    private fun startAnimatedEmotes(holder: Holder) {
        val tv = holder.text
        val spanned = tv.text as? Spanned ?: return
        val found = ArrayList<Animatable>()
        for (span in spanned.getSpans(0, spanned.length, ImageSpan::class.java)) {
            val d = span.drawable
            if (d is Animatable) {
                d.callback = object : Drawable.Callback {
                    override fun invalidateDrawable(who: Drawable) { EmoteFrameMeter.tick(who); tv.invalidate() }
                    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                        tv.postDelayed(what, `when` - android.os.SystemClock.uptimeMillis())
                    }
                    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                        tv.removeCallbacks(what)
                    }
                }
                found.add(d)
                if (!d.isRunning) d.start()
            }
        }
        holder.animated = found
    }

    private fun stopAnimatedEmotes(holder: Holder) {
        for (d in holder.animated) {
            d.stop()
            (d as? Drawable)?.callback = null
        }
        holder.animated = emptyList()
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatLine>() {
            override fun areItemsTheSame(a: ChatLine, b: ChatLine) = a.id == b.id
            override fun areContentsTheSame(a: ChatLine, b: ChatLine) = a.deleted == b.deleted && a.message === b.message
        }
    }
}
