package com.shrug.shrugtv.chat.emote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.telemetry.CrashReporter
import pl.droidsonroids.gif.GifDrawable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Modernisierter EmoteManager in Kotlin (Stage 2, #108).
 * Manages global and channel-specific Twitch, 7TV, BTTV and FFZ emotes.
 */
class EmoteManager @JvmOverloads constructor(
    val channelId: Int,
    val channelName: String?,
    private val contextRef: WeakReference<Context>?,
    val badgeSize: Int,
    val emoteSize: Int,
    val animatedEmotes: Boolean,
    private val extras: com.shrug.shrugtv.chat.ChatExtras = com.shrug.shrugtv.chat.ChatExtras()
) {
    companion object {
        private const val TAG = "ChatEmoteManager"

        /**
         * Emote and badge images load on the chat thread: without a limit one hanging download
         * stopped every later message (the chat stayed empty). Without the image the emote shows
         * as text.
         */
        private const val IMAGE_TIMEOUT_S = 6L

        @JvmField
        var globalEmotes: MutableMap<String, Emote> = ConcurrentHashMap()

        /** What the chat knows about a channel; every part is there once it has been loaded. */
        class ChannelData(
            val bttv: Map<String, Emote>? = null,
            val ffz: Map<String, Emote>? = null,
            val sevenTv: Map<String, Emote>? = null,
            val sevenTvSetId: String? = null,
            val sevenTvUserId: String? = null,
            val badgeUrls: Map<String, String>? = null
        ) {
            fun with(
                bttv: Map<String, Emote>? = this.bttv, ffz: Map<String, Emote>? = this.ffz, sevenTv: Map<String, Emote>? = this.sevenTv,
                sevenTvSetId: String? = this.sevenTvSetId, sevenTvUserId: String? = this.sevenTvUserId, badgeUrls: Map<String, String>? = this.badgeUrls
            ) = ChannelData(bttv, ffz, sevenTv, sevenTvSetId, sevenTvUserId, badgeUrls)
        }

        @JvmField
        val channelCache = ChannelCache<ChannelData>()

        /** The global badges are the same for every channel: loaded once per hour. */
        @Volatile
        private var globalBadgeUrls: Map<String, String>? = null

        @Volatile
        private var globalBadgesAt = 0L
        private const val GLOBAL_BADGES_TTL_MS = 60 * 60_000L

        @JvmStatic
        var globalEmotePattern: Pattern = Pattern.compile("")

        /** A 7TV emote as in emote_set.emotes; null if unusable. */
        @JvmStatic
        /** BTTV's overlay emotes (they have no flag in the API). */
        val BTTV_ZERO_WIDTH = setOf("SoSnowy", "IceCold", "SantaHat", "TopHat", "ReinDeer", "CandyCane", "cvMask", "cvHazmat")

        /** 7TV: active emote flag 1 = zero-width, or emote data flag 256 (ZERO_WIDTH). */
        @JvmStatic
        fun isSevenTvZeroWidth(e: JsonObject): Boolean {
            val flags = e.get("flags")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            val dataFlags = e.getAsJsonObject("data")?.get("flags")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            return (flags and 1) != 0 || (dataFlags and 256) != 0
        }

        fun parseSevenTvEmote(e: JsonObject): Emote? {
            return try {
                val name = e.get("name").asString
                val data = e.getAsJsonObject("data")
                val animated = data.get("animated").asBoolean
                val host = data.getAsJsonObject("host")
                var file = ""
                val files = host.getAsJsonArray("files")
                if (files != null) {
                    for (f in files) {
                        val fileObj = f.asJsonObject
                        if ("WEBP" == fileObj.get("format").asString) {
                            file = fileObj.get("name").asString
                        }
                    }
                }
                if (file.isEmpty()) return null
                Emote.Builder()
                    .provider(Emote.Provider.SEVEN_TV)
                    .code(name)
                    .url("https:" + host.get("url").asString + "/" + file)
                    .webp(true)
                    .animated(animated)
                    .zeroWidth(isSevenTvZeroWidth(e))
                    .build()
            } catch (ex: Exception) {
                CrashReporter.recordException(ex)
                null
            }
        }
    }

    private val SEVEN_TV_GLOBAL_URL = "https://7tv.io/v3/emote-sets/global"
    private val BTTV_GLOBAL_URL = "https://api.betterttv.net/3/cached/emotes/global"
    private val FFZ_GLOBAL_URL = "https://api.frankerfacez.com/v1/room/__ffz_global"

    /**
     * Key of this channel in [channelCache]: the emotes carry their pictures and flags for this size, animation and
     * set of providers, so another setting means another entry.
     */
    private val cacheKey: String get() = "$channelId|$emoteSize|$badgeSize|$animatedEmotes|${extras.providers.map { it.name }.sorted()}|${extras.sevenTvCosmetics}"

    @Volatile
    private var bttvChannelEmotes: MutableMap<String, Emote> = ConcurrentHashMap()

    @Volatile
    private var ffzChannelEmotes: MutableMap<String, Emote> = ConcurrentHashMap()

    @Volatile
    private var sevenTvChannelEmotes: MutableMap<String, Emote> = ConcurrentHashMap()

    @Volatile
    var sevenTvEmoteSetId: String? = null
        private set

    @Volatile
    var sevenTvUserId: String? = null
        private set

    @Volatile
    private var channelEmotes: MutableMap<String, Emote> = ConcurrentHashMap()

    @Volatile
    private var channelEmotePattern: Pattern = Pattern.compile("")

    private val twitchEmotesTagPattern = Pattern.compile("(?<=emotes=)[^;]+")

    fun getChannelEmoteCount(): Int = channelEmotes.size

    /** For tests: the currently valid channel emote for a code, otherwise null. */
    fun channelEmote(code: String): Emote? = channelEmotes[code]

    private fun parseBttvEmotes(jsonArray: JsonArray): MutableMap<String, Emote> {
        val map = HashMap<String, Emote>()
        for (i in 0 until jsonArray.size()) {
            try {
                val obj = jsonArray.get(i).asJsonObject
                val code = obj.get("code").asString
                val id = obj.get("id").asString
                val imageType = obj.get("imageType")?.asString
                // BTTV's CDN; without a URL the emote never got an image (same as Twitch before)
                map[code] = Emote.Builder()
                    .provider(Emote.Provider.BTTV)
                    .id(id)
                    .code(code)
                    .url("https://cdn.betterttv.net/emote/$id/2x")
                    .gif(animatedEmotes && "gif" == imageType)
                    .zeroWidth(code in BTTV_ZERO_WIDTH)
                    .build()
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return map
    }

    private fun toStaticDrawable(webpDrawable: WebpDrawable): Drawable? {
        val ctx = contextRef?.get() ?: return null
        return BitmapDrawable(ctx.resources, webpDrawable.firstFrame)
    }

    private fun addMissingGlobalEmotes(map: Map<String, Emote>) {
        for ((k, v) in map) {
            if (!globalEmotes.containsKey(k)) {
                globalEmotes[k] = v
            }
        }
    }

    private fun buildEmotePattern(map: Map<String, Emote>): Pattern {
        return try {
            val words = ArrayList<String>()
            val punct = ArrayList<String>()
            for (key in map.keys) {
                if (key.contains(":")) {
                    punct.add(Pattern.quote(key))
                } else {
                    words.add(Pattern.quote(key))
                }
            }
            val strJoin = TextUtils.join("|", words)
            val strJoin2 = TextUtils.join("|", punct)
            val sb = StringBuilder()
            if (strJoin.isNotEmpty()) {
                sb.append("\\b(").append(strJoin).append(")\\b")
            }
            if (strJoin2.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append("|")
                sb.append(strJoin2)
            }
            if (sb.isEmpty()) Pattern.compile("") else Pattern.compile(sb.toString())
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            Pattern.compile("")
        }
    }

    private fun loadDrawable(url: String?, height: Int): Drawable? {
        if (url.isNullOrEmpty()) return null
        val ctx = contextRef?.get() ?: return null
        return try {
            val drawable = Glide.with(ctx)
                .asDrawable()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .submit(Int.MIN_VALUE, height)
                .get(IMAGE_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS)
            if (drawable is WebpDrawable || drawable.intrinsicHeight >= height) {
                drawable
            } else {
                val bmp = (drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val scaledWidth = (drawable.intrinsicWidth * height) / drawable.intrinsicHeight
                    BitmapDrawable(ctx.resources, Bitmap.createScaledBitmap(bmp, scaledWidth, height, false))
                } else {
                    drawable
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load emote $url: ${e.message}")
            null
        }
    }

    private fun loadGif(url: String?): GifDrawable? {
        if (url.isNullOrEmpty()) return null
        val ctx = contextRef?.get() ?: return null
        return try {
            val bytes = Glide.with(ctx)
                .`as`(ByteArray::class.java)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .load(url)
                .submit()
                .get(IMAGE_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS)
            if (bytes != null && bytes.size > 3 && bytes[0] == 71.toByte() && bytes[1] == 73.toByte() && bytes[2] == 70.toByte()) {
                GifDrawable(bytes)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadBttvGlobalEmotes(): Map<String, Emote> {
        val map = HashMap<String, Emote>()
        try {
            val jsonArray = Http.load(BTTV_GLOBAL_URL).asJsonArray().get()
            if (jsonArray != null) {
                map.putAll(parseBttvEmotes(jsonArray))
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return map
    }

    fun loadSevenTvEmotes(emoteScope: Enums.EmoteScope, url: String): MutableMap<String, Emote>? {
        val map = HashMap<String, Emote>()
        try {
            val jsonObject = Http.load(url).asJsonObject().get() ?: return null
            if (jsonObject.get("status") != null && jsonObject.get("status").asInt == 404) {
                return null
            }
            val jsonArray: JsonArray? = when (emoteScope) {
                Enums.EmoteScope.CHANNEL -> {
                    rememberSevenTvIds(jsonObject)
                    jsonObject.getAsJsonObject("emote_set")?.getAsJsonArray("emotes")
                }
                Enums.EmoteScope.GLOBAL -> jsonObject.getAsJsonArray("emotes")
            }
            if (jsonArray == null || jsonArray.size() == 0) {
                return null
            }
            for (i in 0 until jsonArray.size()) {
                // One broken emote is skipped, not the whole list (#108)
                val item = jsonArray.get(i).takeIf { it.isJsonObject }?.asJsonObject ?: continue
                parseSevenTvEmote(item)?.let { map[it.code] = it }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return map
    }

    @Synchronized
    fun mergeChannelEmotes() {
        val updated = HashMap<String, Emote>()
        for (source in listOf(bttvChannelEmotes, ffzChannelEmotes, sevenTvChannelEmotes)) {
            for ((key, value) in source) {
                if (!globalEmotes.containsKey(key) && !updated.containsKey(key)) {
                    updated[key] = value
                }
            }
        }
        val pattern = buildEmotePattern(updated)
        channelEmotes = ConcurrentHashMap(updated)
        channelEmotePattern = pattern
    }

    private fun rememberSevenTvIds(response: JsonObject) {
        try {
            sevenTvEmoteSetId = response.getAsJsonObject("emote_set").get("id").asString
            sevenTvUserId = response.getAsJsonObject("user").get("id").asString
        } catch (e: Exception) {
            Log.w(TAG, "7TV-IDs fehlen", e)
        }
    }

    @Synchronized
    fun sevenTvEmoteSet(emote: JsonObject, oldName: String?): String? {
        val updated = parseSevenTvEmote(emote) ?: return null
        val map = HashMap(sevenTvChannelEmotes)
        if (oldName != null) {
            map.remove(oldName)
        }
        map[updated.code] = updated
        sevenTvChannelEmotes = ConcurrentHashMap(map)
        mergeChannelEmotes()
        return updated.code
    }

    @Synchronized
    fun sevenTvEmoteRemove(name: String): Boolean {
        val map = HashMap(sevenTvChannelEmotes)
        val gone = map.remove(name) != null
        sevenTvChannelEmotes = ConcurrentHashMap(map)
        mergeChannelEmotes()
        return gone
    }

    @Synchronized
    fun bttvEmoteSet(emoteJson: JsonObject): String? {
        var emote = emoteJson
        val id = emote.get("id").asString
        if (!emote.has("imageType")) {
            emote = emote.deepCopy()
            var type = "png"
            for (e in bttvChannelEmotes.values) {
                if (id == e.id && e.isGif) type = "gif"
            }
            emote.addProperty("imageType", type)
        }
        val single = JsonArray().apply { add(emote) }
        val parsed = parseBttvEmotes(single)
        if (parsed.isEmpty()) return null

        val map = HashMap(bttvChannelEmotes)
        map.values.removeAll { id == it.id }
        map.putAll(parsed)
        bttvChannelEmotes = ConcurrentHashMap(map)
        mergeChannelEmotes()
        return parsed.keys.firstOrNull()
    }

    @Synchronized
    fun bttvEmoteRemove(id: String): Boolean {
        val map = HashMap(bttvChannelEmotes)
        val gone = map.values.removeAll { id == it.id }
        bttvChannelEmotes = ConcurrentHashMap(map)
        mergeChannelEmotes()
        return gone
    }

    fun sevenTvReload() {
        loadSevenTvChannelEmotes()
        mergeChannelEmotes()
    }

    fun ffzReload() {
        loadFfzChannelEmotes()
        mergeChannelEmotes()
    }

    fun findEmotes(pattern: Pattern, map: Map<String, Emote>, str: String): List<Emote> {
        val result = ArrayList<Emote>()
        val seen = HashMap<String, Emote>()
        val matcher: Matcher = pattern.matcher(str)
        while (matcher.find()) {
            try {
                val group = matcher.group()
                val pos = "${matcher.start()}-${matcher.end() - 1}"
                val existing = seen[group]
                if (existing != null) {
                    existing.addPosition(pos)
                } else {
                    val base = map[group]
                    // Providers switched off in the settings ("Emotes from 7TV/BTTV/FFZ")
                    if (base != null && base.provider in extras.providers) {
                        val built = Emote.Builder()
                            .zeroWidth(base.isZeroWidth)
                            .provider(base.provider)
                            .id(base.id)
                            .code(base.code)
                            .url(base.url)
                            .gif(base.isGif)
                            .webp(base.isWebp)
                            .animated(base.isAnimated)
                            .build()
                        if (!animatedEmotes) {
                            var d = loadDrawable(built.url, emoteSize)
                            if (d is WebpDrawable && built.isAnimated) {
                                d = toStaticDrawable(d)
                            }
                            built.drawable = d
                        } else if (built.isGif) {
                            built.gifDrawable = loadGif(built.url)
                        } else {
                            built.drawable = loadDrawable(built.url, emoteSize)
                        }
                        built.addPosition(pos)
                        result.add(built)
                        seen[group] = built
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
        return result
    }

    fun findChannelEmotes(str: String): List<Emote> {
        val map = channelEmotes
        return if (map.isEmpty()) emptyList() else findEmotes(channelEmotePattern, map, str)
    }

    /** Personal 7TV emote sets (emotes a user may use in every channel) by set id. */
    private val personalSets = ConcurrentHashMap<String, Pair<Map<String, Emote>, Pattern>>()

    /** Loads a personal 7TV emote set; blocking, chat thread or 7TV thread. */
    fun loadPersonalSet(setId: String) {
        if (!extras.sevenTvPersonal || !setId.matches(Regex("[A-Za-z0-9]{1,40}"))) return
        try {
            val json = Http.load("https://7tv.io/v3/emote-sets/$setId").asJsonObject().get() ?: return
            val map = HashMap<String, Emote>()
            for (e in json.getAsJsonArray("emotes") ?: return) {
                val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                parseSevenTvEmote(o)?.let { map[it.code] = it }
            }
            personalSets[setId] = map to buildEmotePattern(map)
            Log.d(TAG, "7TV personal set $setId: ${map.size}")
        } catch (e: Exception) {
            Log.w(TAG, "7TV personal set $setId: ${e.message}")
        }
    }

    fun hasPersonalSet(setId: String) = personalSets.containsKey(setId)

    /** Emotes of a user's personal set in [str]; the set must have been loaded. */
    fun findPersonalEmotes(setId: String?, str: String): List<Emote> {
        if (setId == null || !extras.sevenTvPersonal) return emptyList()
        val (map, pattern) = personalSets[setId] ?: return emptyList()
        return if (map.isEmpty()) emptyList() else findEmotes(pattern, map, str)
    }

    /** 7TV badge image, cached by URL; blocking on first use (chat thread). */
    private val sevenTvBadgeImages = ConcurrentHashMap<String, Drawable>()

    fun sevenTvBadge(url: String?): Drawable? {
        url ?: return null
        sevenTvBadgeImages[url]?.let { return it }
        val d = loadDrawable(url, badgeSize) ?: return null
        sevenTvBadgeImages[url] = d
        return d
    }

    fun findGlobalEmotes(str: String): List<Emote> {
        val map = globalEmotes
        return if (map.isEmpty()) emptyList() else findEmotes(globalEmotePattern, map, str)
    }

    fun parseTwitchEmotesTag(str: String): List<Emote> {
        val result = ArrayList<Emote>()
        try {
            val matcher = twitchEmotesTagPattern.matcher(str)
            while (matcher.find()) {
                val group = matcher.group()
                if (!group.isNullOrEmpty()) {
                    val entries = group.split("/")
                    for (entry in entries) {
                        val parts = entry.split(":")
                        if (parts.size == 2) {
                            val emote = createTwitchEmote(parts[0], parts[1].split(","))
                            if (emote != null) result.add(emote)
                        }
                    }
                }
                return result
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return result
    }

    fun parseVodEmotes(list: List<String>): List<Emote> {
        val result = ArrayList<Emote>()
        for (item in list) {
            val parts = item.split(";")
            if (parts.size == 3) {
                val id = parts[0]
                val start = parts[1]
                val end = parts[2]
                val pos = "$start-$end"
                val existing = result.firstOrNull { it.id == id }
                if (existing != null) {
                    existing.addPosition(pos)
                } else {
                    val emote = createTwitchEmote(id, listOf(pos))
                    if (emote != null) result.add(emote)
                }
            }
        }
        return result
    }

    /** Badge image URLs by "set/version"; channel sets override global ones. */
    private val badgeUrls = ConcurrentHashMap<String, String>()

    /** Loaded badge images by "set/version". */
    private val badgeImages = ConcurrentHashMap<String, Drawable>()

    /**
     * Global and channel badge sets from Helix (replaces the six fixed badges and the
     * subscriber-only set; docs/chat_plan.md 2.4). Only the URLs: images load on first use.
     * Blocking.
     */
    fun loadBadgeSets() {
        val ctx = contextRef?.get() ?: return
        try {
            val now = System.currentTimeMillis()
            var global = globalBadgeUrls?.takeIf { now - globalBadgesAt <= GLOBAL_BADGES_TTL_MS }
            if (global == null) {
                global = HelixApi.getGlobalChatBadges(ctx)?.let { parseBadgeUrls(it) }
                if (!global.isNullOrEmpty()) {
                    globalBadgeUrls = global
                    globalBadgesAt = now
                }
            }
            global?.let { badgeUrls.putAll(it) }

            var channel = channelCache.get(cacheKey)?.badgeUrls
            if (channel == null) {
                channel = HelixApi.getChannelChatBadges(ctx, channelId)?.let { parseBadgeUrls(it) }
                if (channel != null) {
                    val loaded: Map<String, String> = channel
                    channelCache.update(cacheKey, { ChannelData() }) { it.with(badgeUrls = loaded) }
                }
            }
            channel?.let { badgeUrls.putAll(it) }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private fun parseBadgeUrls(json: JsonObject): Map<String, String> {
        val urls = HashMap<String, String>()
        val data = json.getAsJsonArray("data") ?: return urls
        for (set in data) {
            val setObj = set.asJsonObject
            val setId = setObj.get("set_id")?.asString ?: continue
            val versions = setObj.getAsJsonArray("versions") ?: continue
            for (v in versions) {
                val vObj = v.asJsonObject
                val id = vObj.get("id")?.asString ?: continue
                val url = vObj.get("image_url_2x")?.asString ?: vObj.get("image_url_1x")?.asString ?: continue
                urls["$setId/$id"] = url
            }
        }
        return urls
    }

    /** Badge image for a set and version, null if unknown. Blocking on first use (chat thread). */
    fun badgeDrawable(setId: String, version: String): Drawable? {
        val key = "$setId/$version"
        badgeImages[key]?.let { return it }
        val url = badgeUrls[key] ?: return null
        val d = loadDrawable(url, badgeSize) ?: return null
        badgeImages[key] = d
        return d
    }

    fun loadSubscriberBadges(): HashMap<String, Drawable>? {
        val map = HashMap<String, Drawable>()
        val ctx = contextRef?.get() ?: return null
        try {
            val badgesJson = HelixApi.getChannelChatBadges(ctx, channelId) ?: return null
            CrashReporter.setCustomKey("channel_id", channelId)
            val data = badgesJson.getAsJsonArray("data") ?: return null
            for (elem in data) {
                val obj = elem.asJsonObject
                if ("subscriber" == obj.get("set_id")?.asString) {
                    val versions = obj.getAsJsonArray("versions") ?: continue
                    for (v in versions) {
                        val versionObj = v.asJsonObject
                        val id = versionObj.get("id").asString
                        val url = versionObj.get("image_url_4x").asString
                        val d = loadDrawable(url, badgeSize)
                        if (d != null) map[id] = d
                    }
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        return if (map.isNotEmpty()) map else null
    }

    /** Loaded Twitch emote images by id: a busy chat repeats the same emotes constantly. */
    private val twitchEmoteImages = ConcurrentHashMap<String, Emote>()

    /**
     * Twitch emote by id with its image. The builder never set a URL before, so Twitch emotes were
     * never loaded; the URL is the documented CDN template.
     * Blocking (Glide): call it off the main thread and off the WebSocket thread.
     */
    fun createTwitchEmote(id: String, positions: List<String>): Emote? {
        if (id.isEmpty()) return null
        val cached = twitchEmoteImages[id]
        if (cached != null) {
            val copy = Emote.Builder()
                .provider(Emote.Provider.TWITCH)
                .id(id)
                .url(cached.url)
                .positions(positions)
                .build()
            copy.isGif = cached.isGif
            copy.isAnimated = cached.isAnimated
            copy.drawable = cached.drawable
            copy.gifDrawable = cached.gifDrawable
            return copy
        }
        val mayBeAnimated = animatedEmotes && id.startsWith("emotesv2_")
        val format = if (mayBeAnimated) "default" else "static"
        val emote = Emote.Builder()
            .provider(Emote.Provider.TWITCH)
            .id(id)
            .url("https://static-cdn.jtvnw.net/emoticons/v2/$id/$format/dark/3.0")
            .positions(positions)
            .gif(mayBeAnimated)
            .build()
        val url = emote.url
        if (emote.isGif) {
            val gif = loadGif(url)
            if (gif != null) {
                emote.gifDrawable = gif
                emote.isAnimated = true
            } else {
                emote.drawable = loadDrawable(url, emoteSize)
                emote.isGif = false
                emote.isAnimated = false
            }
        } else {
            emote.isGif = false
            emote.isAnimated = false
            emote.drawable = loadDrawable(url, emoteSize)
        }
        if (emote.drawable != null || emote.gifDrawable != null) {
            twitchEmoteImages[id] = emote
        }
        return emote
    }

    fun loadSevenTvChannelEmotes() {
        if (Emote.Provider.SEVEN_TV !in extras.providers && !extras.sevenTvCosmetics) return
        val remembered = channelCache.get(cacheKey)
        val rememberedEmotes = remembered?.sevenTv
        if (remembered != null && rememberedEmotes != null) {
            sevenTvChannelEmotes = ConcurrentHashMap(rememberedEmotes)
            sevenTvEmoteSetId = remembered.sevenTvSetId
            sevenTvUserId = remembered.sevenTvUserId
            Log.d(TAG, "load7tvChannelEmotes: ${rememberedEmotes.size} (remembered)")
            return
        }
        val emotes = loadSevenTvEmotes(Enums.EmoteScope.CHANNEL, "https://7tv.io/v3/users/twitch/$channelId")
        if (!emotes.isNullOrEmpty()) {
            sevenTvChannelEmotes = ConcurrentHashMap(emotes)
            val copy: Map<String, Emote> = HashMap(emotes)
            channelCache.update(cacheKey, { ChannelData() }) { it.with(sevenTv = copy, sevenTvSetId = sevenTvEmoteSetId, sevenTvUserId = sevenTvUserId) }
            Log.d(TAG, "load7tvChannelEmotes: ${emotes.size}")
        }
    }

    fun loadSevenTvGlobalEmotes(): Map<String, Emote>? {
        val emotes = loadSevenTvEmotes(Enums.EmoteScope.GLOBAL, SEVEN_TV_GLOBAL_URL)
        if (emotes != null) {
            Log.d(TAG, "load7tvGlobalEmotes: ${emotes.size}")
        }
        return emotes
    }

    fun loadFfzEmotes(url: String): MutableMap<String, Emote>? {
        val map = HashMap<String, Emote>()
        try {
            val jsonObject = Http.load(url).asJsonObject().get() ?: return null
            if (jsonObject.get("status") != null && jsonObject.get("status").asInt == 404) {
                return null
            }
            val roomSet = jsonObject.getAsJsonObject("room")?.get("set")?.asString ?: return null
            val emoticons = jsonObject.getAsJsonObject("sets")?.getAsJsonObject(roomSet)?.getAsJsonArray("emoticons") ?: return null
            for (i in 0 until emoticons.size()) {
                try {
                    val item = emoticons.get(i).asJsonObject
                    val name = item.get("name").asString
                    val id = "FFZ" + item.get("id").asString
                    val urls = item.getAsJsonObject("urls")
                    var imgUrl = ""
                    if (urls != null) {
                        if (urls.has("4")) imgUrl = urls.get("4").asString
                        else if (urls.has("2")) imgUrl = urls.get("2").asString
                        else if (urls.has("1")) imgUrl = urls.get("1").asString
                    }
                    map[name] = Emote.Builder()
                        .provider(Emote.Provider.FFZ)
                        .id(id)
                        .code(name)
                        .url(imgUrl)
                        .build()
                } catch (e: Exception) {
                    CrashReporter.recordException(e)
                }
            }
            return map
        } catch (e: Exception) {
            // A channel without a FrankerFaceZ room answers 404: normal, no emotes and no error report
            if (Http.StatusException.of(e)?.code == 404) return null
            CrashReporter.recordException(e)
            return null
        }
    }

    fun loadBttvChannelEmotes() {
        if (Emote.Provider.BTTV !in extras.providers) return
        channelCache.get(cacheKey)?.bttv?.let { bttvChannelEmotes = ConcurrentHashMap(it); return }
        val map = HashMap<String, Emote>()
        try {
            val jsonObject = Http.load("https://api.betterttv.net/3/cached/users/twitch/$channelId").asJsonObject().get()
            if (jsonObject != null) {
                val shared = jsonObject.getAsJsonArray("sharedEmotes")
                val channel = jsonObject.getAsJsonArray("channelEmotes")
                if (shared != null && shared.size() > 0) map.putAll(parseBttvEmotes(shared))
                if (channel != null && channel.size() > 0) map.putAll(parseBttvEmotes(channel))
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
        if (map.isNotEmpty()) {
            bttvChannelEmotes = ConcurrentHashMap(map)
            val copy: Map<String, Emote> = HashMap(map)
            channelCache.update(cacheKey, { ChannelData() }) { it.with(bttv = copy) }
        }
    }

    fun loadFfzChannelEmotes() {
        if (Emote.Provider.FFZ !in extras.providers) return
        channelCache.get(cacheKey)?.ffz?.let { ffzChannelEmotes = ConcurrentHashMap(it); return }
        val emotes = loadFfzEmotes("https://api.frankerfacez.com/v1/room/id/$channelId")
        if (!emotes.isNullOrEmpty()) {
            ffzChannelEmotes = ConcurrentHashMap(emotes)
            val copy: Map<String, Emote> = HashMap(emotes)
            channelCache.update(cacheKey, { ChannelData() }) { it.with(ffz = copy) }
        }
    }

    fun loadGlobalEmotes() {
        if (globalEmotes.isNotEmpty()) return
        val bttv = loadBttvGlobalEmotes()
        val ffz = loadFfzEmotes(FFZ_GLOBAL_URL)
        val sevenTv = loadSevenTvGlobalEmotes()
        globalEmotes.putAll(bttv)
        if (!ffz.isNullOrEmpty()) addMissingGlobalEmotes(ffz)
        if (!sevenTv.isNullOrEmpty()) addMissingGlobalEmotes(sevenTv)
        globalEmotePattern = buildEmotePattern(globalEmotes)
    }
}
