package com.shrug.shrugtv.eventSub

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Twitch emotes of `channel.chat.message` come as fragments, not as an `emotes` list; the old model
 * read a field that does not exist, so native emotes never became images (chat plan A5).
 */
class EventSubChatMessageTest {

    private val payload = """
        {"metadata":{"message_id":"env-1","message_type":"notification","subscription_type":"channel.chat.message"},
         "payload":{"event":{
           "chatter_user_login":"viewer","chatter_user_name":"Viewer","message_id":"msg-1","color":"#FF0000",
           "badges":[{"set_id":"subscriber","id":"12","info":"14"},{"set_id":"vip","id":"1","info":""}],
           "message":{"text":"😀 Kappa hi Kappa",
             "fragments":[
               {"type":"text","text":"😀 ","emote":null},
               {"type":"emote","text":"Kappa","emote":{"id":"25","emote_set_id":"0"}},
               {"type":"text","text":" hi ","emote":null},
               {"type":"emote","text":"Kappa","emote":{"id":"25","emote_set_id":"0"}}
             ]}}}}
    """.trimIndent()

    @Test
    fun `fragments are parsed`() {
        val model = Gson().fromJson(payload, EventSubChatMessageModel::class.java)
        val ev = model.payload!!.event!!
        assertEquals("msg-1", ev.messageId)
        assertEquals(4, ev.message!!.fragments!!.size)
        assertEquals("12", ev.badges!!.first { it.setId == "subscriber" }.id)
    }

    @Test
    fun `emote positions are UTF-16 offsets into the text`() {
        val model = Gson().fromJson(payload, EventSubChatMessageModel::class.java)
        val text = model.payload!!.event!!.message!!.text!!
        val positions = EventSubChatMessageModel.twitchEmotePositions(model.payload!!.event!!.message!!.fragments)
        // The emoji is two UTF-16 chars, plus a space: first Kappa starts at 3
        assertEquals(listOf("25" to "3-7", "25" to "12-16"), positions)
        for ((_, pos) in positions) {
            val (start, end) = pos.split("-").map { it.toInt() }
            assertEquals("Kappa", text.substring(start, end + 1))
        }
    }

    @Test
    fun `no fragments means no emotes`() {
        assertTrue(EventSubChatMessageModel.twitchEmotePositions(null).isEmpty())
        assertTrue(EventSubChatMessageModel.twitchEmotePositions(emptyList()).isEmpty())
    }

    @Test
    fun `chat sub condition includes broadcaster_user_id and user_id`() {
        for (type in EventSubModels.CHAT_TYPES) {
            val sub = com.google.gson.JsonParser.parseString(
                EventSubModels.getChatSub(type, "123", "456", "sess-1")
            ).asJsonObject.getAsJsonObject("condition")
            assertEquals("123", sub.get("broadcaster_user_id").asString)
            assertEquals("456", sub.get("user_id").asString)
        }
    }
}
