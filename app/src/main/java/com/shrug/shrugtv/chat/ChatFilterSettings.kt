package com.shrug.shrugtv.chat

import java.util.regex.Pattern

/**
 * Filter settings for the chat.
 * Compiles the blocked-word regex once on change.
 */
class ChatFilterSettings private constructor(builder: Builder) {
    val blockedWordsRegex: String? = builder.blockedWordsRegex
    val blockedUsers: Array<String>? = builder.blockedUsers
    val isShowSubMessages: Boolean = builder.showSubMessages
    val isShowGiftedSubs: Boolean = builder.showGiftedSubs
    val isFilterBots: Boolean = builder.filterBots
    val isFilterModBots: Boolean = builder.filterModBots
    /** Hides messages that start with "!", independent of the bot filter. */
    val isFilterCommands: Boolean = builder.filterCommands

    // Pre-compiled regex pattern, built once (null for an empty or faulty regex)
    val compiledPattern: Pattern? = blockedWordsRegex?.takeIf { it.isNotBlank() }?.let {
        try {
            Pattern.compile(it, Pattern.CASE_INSENSITIVE)
        } catch (_: Exception) {
            null
        }
    }

    class Builder {
        var blockedWordsRegex: String? = null
            private set
        var blockedUsers: Array<String>? = null
            private set
        var showSubMessages: Boolean = false
            private set
        var showGiftedSubs: Boolean = false
            private set
        var filterBots: Boolean = false
            private set
        var filterModBots: Boolean = false
            private set
        var filterCommands: Boolean = false
            private set

        fun blockedWordsRegex(regex: String?) = apply { this.blockedWordsRegex = regex }
        fun blockedUsers(users: Array<String>?) = apply { this.blockedUsers = users }
        fun showSubMessages(show: Boolean) = apply { this.showSubMessages = show }
        fun showGiftedSubs(show: Boolean) = apply { this.showGiftedSubs = show }
        fun filterBots(filter: Boolean) = apply { this.filterBots = filter }
        fun filterModBots(filter: Boolean) = apply { this.filterModBots = filter }
        fun filterCommands(filter: Boolean) = apply { this.filterCommands = filter }

        fun build(): ChatFilterSettings = ChatFilterSettings(this)
    }
}
