package com.shrug.shrugtv.chat

/** Emote position in a chat message (replaces IrcEmotePlacement). */
data class EmotePlacement(val id: String, val start: Int, val end: Int)

/** User badge like subscriber/12, moderator/1 (replaces IrcBadge). */
data class ChatBadge(val name: String, val version: String)

/** Reply metadata (replaces IrcReply). */
data class ChatReply(
    val parentMsgId: String,
    val parentUserLogin: String,
    val parentDisplayName: String,
    val parentMsgBody: String
)
