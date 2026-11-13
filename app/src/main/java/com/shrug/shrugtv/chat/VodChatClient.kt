package com.shrug.shrugtv.chat

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.api.gql.GqlApi
import com.shrug.shrugtv.api.gql.GqlRequests
import com.shrug.shrugtv.chat.emote.Emote
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.TimeUtil
import java.util.ArrayList

/**
 * Modernisierter VodChatClient in Kotlin (Stage 2, #108).
 * Loads historic chat messages in sync with the VOD through GQL video.comments.
 */
class VodChatClient(builder: ChatClient.Builder) : ChatClient(builder) {

    private var emotesLoaded = false
    private var fetching = false

    private fun extractGiftRecipient(str: String): String? {
        if (str.contains(" gifted a Tier ") && str.contains(" sub to ")) {
            val parts = str.split(" ")
            if (parts.isNotEmpty()) {
                val last = parts.last()
                if (last.endsWith("!")) {
                    return last.substring(0, last.length - 1)
                }
            }
        }
        return null
    }

    private fun isSubscriptionNotice(login: String, text: String): Boolean {
        if (text.contains("$login subscribed with Prime. They've subscribed for ")) {
            return true
        }
        return text.contains("$login subscribed at Tier 1. They've subscribed for")
    }

    override fun startChat() {
        Thread.currentThread().name = "Thread ChatManager $channelName"
        Log.d("ChatManager", "Starting the chat of recording $vodId of $channelName")
        if (!stopRequested.get()) {
            subscriberBadges = emoteManager.loadSubscriberBadges()
            Log.d("ChatManager", "SubEmote Done")
        }
        if (!stopRequested.get()) {
            emoteManager.loadGlobalEmotes()
            Log.d("ChatManager", "Global emotes Done")
        }
        if (!stopRequested.get()) {
            emoteManager.loadBttvChannelEmotes()
            Log.d("ChatManager", "BTTV Done")
        }
        if (!stopRequested.get()) {
            emoteManager.loadFfzChannelEmotes()
            Log.d("ChatManager", "FFZ Done")
        }
        if (!stopRequested.get()) {
            emoteManager.loadSevenTvChannelEmotes()
            Log.d("ChatManager", "7TV Done")
        }
        emoteManager.mergeChannelEmotes()
        emotesLoaded = true
    }

    override fun fetchVodMessages(offsetSeconds: Long) {
        if (offsetSeconds < 0 || !emotesLoaded || fetching) return
        fetching = true
        val ctx = contextRef?.get()
        try {
            // By offset only: the cursor paging of this query answers "failed integrity check" without a Twitch
            // integrity token (measured 2026-11-01). A page covers about 10 s before to 50 s after the offset.
            val query = GqlRequests.buildVideoCommentsRequest(vodId, offsetSeconds, "").toString()
            val response: JsonObject? = GqlApi.postQuery(ctx, query)
            if (response == null) {
                fetching = false
                return
            }

            val commentsObj = response.getAsJsonObject("data")
                ?.getAsJsonObject("video")
                ?.getAsJsonObject("comments")

            val edges = commentsObj?.getAsJsonArray("edges")
            if (edges != null) {
                for (i in 0 until edges.size()) {
                    try {
                        val node = edges.get(i).asJsonObject.getAsJsonObject("node") ?: continue
                        val commenter = if (node.get("commenter").isJsonNull) null else node.getAsJsonObject("commenter")
                        val messageObj = if (node.get("message").isJsonNull) null else node.getAsJsonObject("message")
                        if (commenter == null || messageObj == null) continue

                        val emotesList = ArrayList<Emote>()
                        val vodEmoteIds = ArrayList<String>()

                        var isSub = false
                        var isMod = false
                        var isPartner = false
                        var isVip = false
                        var isBroadcaster = false
                        var isPrime = false
                        var subVersion: String? = null

                        val userBadges = if (messageObj.get("userBadges") == null || messageObj.get("userBadges").isJsonNull) {
                            null
                        } else {
                            messageObj.getAsJsonArray("userBadges")
                        }

                        if (userBadges != null) {
                            for (b in 0 until userBadges.size()) {
                                val badgeObj = userBadges.get(b).asJsonObject
                                val setId = if (badgeObj.get("setID").isJsonNull) null else badgeObj.get("setID").asString
                                when (setId) {
                                    "subscriber" -> {
                                        subVersion = if (badgeObj.get("version").isJsonNull) null else badgeObj.get("version").asString
                                        isSub = true
                                    }
                                    "moderator" -> isMod = true
                                    "partner" -> isPartner = true
                                    "vip" -> isVip = true
                                    "broadcaster" -> isBroadcaster = true
                                    "premium" -> isPrime = true
                                }
                            }
                        }

                        val messageId = if (node.get("id").isJsonNull) "" else node.get("id").asString
                        val contentOffset = if (node.get("contentOffsetSeconds").isJsonNull) -1 else node.get("contentOffsetSeconds").asInt
                        val displayName = if (commenter.get("displayName").isJsonNull) "Error" else commenter.get("displayName").asString
                        val timestampStr = if (contentOffset > 0 && timestampFormat != null) TimeUtil.hm(contentOffset.toLong()) else ""

                        val sb = StringBuilder()
                        val fragments = messageObj.getAsJsonArray("fragments")
                        if (fragments != null) {
                            for (f in 0 until fragments.size()) {
                                val frag = fragments.get(f).asJsonObject
                                sb.append(frag.get("text").asString)
                                val emoteObj = frag.get("emote")
                                if (emoteObj != null && !emoteObj.isJsonNull) {
                                    vodEmoteIds.add(emoteObj.asJsonObject.get("id").asString)
                                }
                            }
                        }

                        val text = sb.toString()
                        val isGift = text.contains(" gifted a Tier ") && text.contains(" sub to ")
                        val isSubNotice = isSubscriptionNotice(displayName, text)

                        emotesList.addAll(emoteManager.parseVodEmotes(vodEmoteIds))
                        emotesList.addAll(emoteManager.findGlobalEmotes(text))
                        emotesList.addAll(emoteManager.findChannelEmotes(text))

                        val userColor = if (messageObj.get("userColor").isJsonNull) "" else messageObj.get("userColor").asString

                        var subBadge: Drawable? = null
                        if (isSub && ctx != null) {
                            try {
                                subBadge = ContextCompat.getDrawable(ctx, R.drawable.default_subicon)?.apply {
                                    setBounds(0, 0, badgeSize, badgeSize)
                                }
                                if (!subVersion.isNullOrEmpty() && subscriberBadges != null) {
                                    val specific = subscriberBadges?.get(subVersion)
                                    if (specific != null) subBadge = specific
                                }
                            } catch (e: Exception) {
                                CrashReporter.recordException(e)
                            }
                        }

                        val chatMessage: ChatMessage = if (isGift) {
                            ChatMessage.Builder()
                                .text(text)
                                .noticeType(Enums.SubType.GIFTED_SUB)
                                .noticeUser(displayName)
                                .giftRecipient(extractGiftRecipient(text))
                                .slotId(slotId)
                                .build()
                        } else if (isSubNotice) {
                            ChatMessage.Builder()
                                .text(text)
                                .noticeType(Enums.SubType.NORMAL_SUB)
                                .noticeUser(displayName)
                                .slotId(slotId)
                                .build()
                        } else {
                            ChatMessage.Builder()
                                .text(text)
                                .displayName(displayName)
                                .color(userColor)
                                .mod(isMod)
                                .partner(isPartner)
                                .subscriber(isSub)
                                .broadcaster(isBroadcaster)
                                .vip(isVip)
                                .prime(isPrime)
                                .emotes(emotesList)
                                .subscriberBadge(subBadge)
                                .vodOffsetSeconds(contentOffset.toDouble())
                                .messageId(messageId)
                                .channelName(channelName)
                                .action(false)
                                .highlighted(false)
                                .timestamp(timestampStr)
                                .slotId(slotId)
                                .build()
                        }
                        postMessage(chatMessage)
                    } catch (e: Exception) {
                        CrashReporter.recordException(e)
                    }
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }

        fetching = false
    }
}
