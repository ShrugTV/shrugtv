package com.shrug.shrugtv.chat

import java.util.regex.Pattern

/**
 * Own highlights (words and users, docs/chat_plan.md 2.6a) and mentions of the signed-in user
 * (2.6). Pure, so it runs in JVM tests; the renderer turns the ranges into spans.
 */
class ChatHighlights(
    words: List<String>,
    users: List<String>,
    /** Roles whose messages are highlighted (like 7TV's badge highlights). */
    val roles: Set<Role> = emptySet(),
    /** Colour of highlighted users, roles and words (ARGB, opaque). */
    val color: Int = DEFAULT_COLOR
) {
    enum class Role { BROADCASTER, MOD, VIP }

    /** Whole words, case-insensitive (same rules as the blocked words filter). */
    private val wordsPattern: Pattern? = ChatPrefs.wordsRegex(words)?.let {
        Pattern.compile(it, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
    }

    private val users: Set<String> = users.map { it.removePrefix("@").lowercase() }.toSet()

    val isEmpty: Boolean get() = wordsPattern == null && users.isEmpty() && roles.isEmpty()

    /** True if a message of someone with these roles is highlighted. */
    fun isHighlightedRole(broadcaster: Boolean, mod: Boolean, vip: Boolean): Boolean =
        (broadcaster && Role.BROADCASTER in roles) || (mod && Role.MOD in roles) || (vip && Role.VIP in roles)

    /** Ranges (start inclusive, end exclusive) of highlighted words in [text]. */
    fun wordRanges(text: String): List<IntRange> {
        val p = wordsPattern ?: return emptyList()
        val m = p.matcher(text)
        val result = ArrayList<IntRange>()
        while (m.find()) result.add(m.start() until m.end())
        return result
    }

    fun isHighlightedUser(login: String?, displayName: String?): Boolean =
        (login != null && login.lowercase() in users) || (displayName != null && displayName.lowercase() in users)

    companion object {
        @JvmField
        val NONE = ChatHighlights(emptyList(), emptyList())

        const val DEFAULT_COLOR = 0xFF9F7AEA.toInt()

        /** `pref_chat_highlight_color` values. */
        val COLORS = linkedMapOf(
            "purple" to 0xFF9F7AEA.toInt(), "red" to 0xFFF0526C.toInt(), "orange" to 0xFFFF9F43.toInt(),
            "yellow" to 0xFFF5D547.toInt(), "green" to 0xFF3DDC84.toInt(), "blue" to 0xFF4FA3FF.toInt()
        )

        @JvmStatic
        fun colorOf(value: String?): Int = COLORS[value] ?: DEFAULT_COLOR

        /**
         * True if [text] mentions the user: "@login", "@DisplayName" or the bare login as a word,
         * case-insensitive. Matched only the display name with "@" before (bug list C2.6).
         */
        @JvmStatic
        fun mentions(text: String, login: String?, displayName: String?): Boolean {
            val names = listOfNotNull(login, displayName).filter { it.isNotBlank() }.distinct()
            if (names.isEmpty()) return false
            val alt = names.joinToString("|") { Pattern.quote(it) }
            return Pattern.compile("(?<![\\p{L}\\p{N}_])@?(?:$alt)(?![\\p{L}\\p{N}_])", Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
                .matcher(text).find()
        }
    }
}
