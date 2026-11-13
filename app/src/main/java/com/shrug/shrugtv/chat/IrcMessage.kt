package com.shrug.shrugtv.chat

/**
 * One IRC line with IRCv3 tags, as the chat history service returns them
 * (`@badges=…;color=…;display-name=… :nick!nick@nick.tmi.twitch.tv PRIVMSG #channel :text`).
 * Pure, so it is unit-tested.
 */
data class IrcMessage(
    val tags: Map<String, String>,
    val prefix: String?,
    val command: String,
    val params: List<String>,
    val trailing: String?
) {
    fun tag(key: String): String? = tags[key]?.takeIf { it.isNotEmpty() }

    /** Login from the prefix "nick!user@host". */
    val nick: String? get() = prefix?.substringBefore('!')?.takeIf { it.isNotEmpty() }

    /** "/me" messages arrive as CTCP ACTION. */
    val isAction: Boolean get() = trailing?.startsWith("\u0001ACTION ") == true

    /** Message text without the ACTION wrapper. */
    val text: String? get() = trailing?.let { if (isAction) it.removePrefix("\u0001ACTION ").removeSuffix("\u0001") else it }

    /** badges=moderator/1,subscriber/12 -> [(moderator, 1), (subscriber, 12)]. */
    val badges: List<Pair<String, String>>
        get() = tag("badges")?.split(',')?.mapNotNull { b ->
            val i = b.indexOf('/')
            if (i <= 0) null else b.substring(0, i) to b.substring(i + 1)
        } ?: emptyList()

    companion object {
        @JvmStatic
        fun parse(line: String): IrcMessage? {
            var rest = line.trimEnd('\r', '\n')
            if (rest.isEmpty()) return null
            val tags = HashMap<String, String>()
            if (rest.startsWith("@")) {
                val end = rest.indexOf(' ').takeIf { it > 0 } ?: return null
                for (pair in rest.substring(1, end).split(';')) {
                    val eq = pair.indexOf('=')
                    if (eq < 0) tags[pair] = "" else tags[pair.substring(0, eq)] = unescape(pair.substring(eq + 1))
                }
                rest = rest.substring(end + 1).trimStart()
            }
            var prefix: String? = null
            if (rest.startsWith(":")) {
                val end = rest.indexOf(' ').takeIf { it > 0 } ?: return null
                prefix = rest.substring(1, end)
                rest = rest.substring(end + 1).trimStart()
            }
            var trailing: String? = null
            val t = rest.indexOf(" :")
            if (t >= 0) {
                trailing = rest.substring(t + 2)
                rest = rest.substring(0, t)
            }
            val parts = rest.split(' ').filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            return IrcMessage(tags, prefix, parts[0], parts.drop(1), trailing)
        }

        /** IRCv3 tag value escapes: \s space, \: semicolon, \\ backslash, \r, \n. */
        @JvmStatic
        fun unescape(v: String): String {
            if (!v.contains('\\')) return v
            val sb = StringBuilder()
            var i = 0
            while (i < v.length) {
                val c = v[i]
                if (c == '\\' && i + 1 < v.length) {
                    when (v[i + 1]) {
                        's' -> sb.append(' ')
                        ':' -> sb.append(';')
                        '\\' -> sb.append('\\')
                        'r' -> sb.append('\r')
                        'n' -> sb.append('\n')
                        else -> sb.append(v[i + 1])
                    }
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}
