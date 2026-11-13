package com.shrug.shrugtv.model

class ChatRoomState(
    val channel: String?,
    var emoteOnly: Int,
    var followersOnly: Int,
    var slowMode: Int,
    var subsOnly: Int
)
