package com.shrug.shrugtv.companion

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.chat.ui.ChatColors
import com.shrug.shrugtv.ui.tv.player.ChatLine

/**
 * Chat lines for the phone: name, readable colour, text and emote images by URL with their
 * positions; the phone renders them itself. Badges stay on the TV (they are drawables there).
 */
object ChatMirror {
    @JvmStatic
    fun line(l: ChatLine): JsonObject {
        val m = l.message
        val name = m.displayName ?: ""
        val color = ChatColors.readable(ChatColors.parse(m.color) ?: ChatColors.fallback(name))
        return JsonObject().apply {
            addProperty("id", l.id)
            addProperty("n", name)
            addProperty("c", "#%06X".format(color and 0xFFFFFF))
            addProperty("x", m.text ?: "")
            addProperty("d", l.deleted)
            addProperty("no", m.isNotice)
            addProperty("hl", m.isHighlighted)
            m.reply?.let { addProperty("r", "@${it.parentDisplayName}: ${it.parentMsgBody.take(60)}") }
            val emotes = JsonArray()
            for (e in m.emotes.orEmpty()) {
                if (e.url.isEmpty()) continue
                for (pos in e.positions) {
                    val parts = pos.split("-")
                    val start = parts.getOrNull(0)?.toIntOrNull() ?: continue
                    val end = parts.getOrNull(1)?.toIntOrNull() ?: continue
                    emotes.add(JsonArray().apply { add(start); add(end); add(e.url) })
                }
            }
            add("e", emotes)
        }
    }
}
