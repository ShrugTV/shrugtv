package com.shrug.shrugtv.player

import com.shrug.shrugtv.BuildConfig
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shrug.shrugtv.apihelpers.RestrictedVodPlaylist
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.LoadState
import com.shrug.shrugtv.model.PlaybackUrlCacheEntry
import com.shrug.shrugtv.model.StreamQuality
import com.shrug.shrugtv.net.Http
import com.shrug.shrugtv.player.hls.TwitchAdBlocker
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.Threads
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale
import java.util.Random
import java.util.regex.Pattern

class PlaybackUrlResolver(
    context: Context?,
    private val channelOrVideoId: String?,
    private val playbackType: Enums.PlaybackKind?,
    private val listener: Listener
) {

    fun interface Listener {
        fun onQualitiesResolved(arrayList: ArrayList<StreamQuality>?, str: String?)
    }

    private val contextRef: WeakReference<Context?> = WeakReference(context)
    var useStreamToken: Boolean = false
    private var manifestCluster: String = ""
    private var vodTokenJson: JsonObject? = null

    companion object {
        private val liveUrlCache = HashMap<String, PlaybackUrlCacheEntry>()
        private var proxyServer = Enums.ProxyServer.NONE

        // Own proxy URL (setting pref_proxy_url), only used with ProxyServer.CUSTOM.
        // Template with one %s for the channel name, e.g. https://proxy.lan/hls-raw/%s.m3u8.
        // The proxy receives the request in usher style (?player=twitchweb&token=…&sig=…) and has to
        // pass it on to usher.ttvnw.net from an ad-free region.
        private var proxyUrlTemplate = ""

        @JvmStatic
        fun setProxyServer(proxyServer2: Enums.ProxyServer) {
            proxyServer = proxyServer2
        }

        @JvmStatic
        fun setProxyUrl(proxyUrl: String?) {
            proxyUrlTemplate = proxyUrl?.trim() ?: ""
        }

        /** True when the VOD identifier is a Twitch clip slug rather than a numeric broadcast ID. */
        @JvmStatic
        fun isClipSlug(id: String?): Boolean {
            return !id.isNullOrEmpty() && !id.matches(Regex("\\d+"))
        }

        /**
         * Qualities of a master playlist: "Auto" (the master address itself) and for each NAME the
         * following variant address, "Audio Only" at the end. Static and without Android so that it is testable
         * (PlaybackUrlResolverTest).
         */
        @JvmStatic
        fun qualitiesFromMaster(str: String, str2: String): ArrayList<StreamQuality> {
            val arrayList = ArrayList<StreamQuality>()
            val matcher = Pattern.compile("((?<=NAME=\").+(?=\",)|http.+m3u8|PREMIUM_CONTENT)").matcher(str)
            arrayList.add(StreamQuality("Auto", str2))
            var name = ""
            while (matcher.find()) {
                val hits = matcher.group()
                if (hits == "PREMIUM_CONTENT") {
                    continue
                }
                if (hits.startsWith("http")) {
                    if (name.isNotEmpty()) {
                        arrayList.add(StreamQuality(name, hits))
                        name = ""
                    }
                } else {
                    name = hits
                }
            }
            moveAudioOnlyToEnd(arrayList)
            return arrayList
        }

        /** Builds the StreamQuality list from a GQL clip(slug) response. */
        @JvmStatic
        fun qualitiesFromClip(response: JsonObject?): ArrayList<StreamQuality> {
            val list = ArrayList<StreamQuality>()
            if (response == null) {
                return list
            }
            try {
                val data = if (response.has("data") && !response.get("data").isJsonNull) response.getAsJsonObject("data") else null
                val clip = if (data != null && data.has("clip") && !data.get("clip").isJsonNull) data.getAsJsonObject("clip") else null
                if (clip == null) {
                    return list
                }
                val tokenObj = if (clip.has("playbackAccessToken") && !clip.get("playbackAccessToken").isJsonNull) {
                    clip.getAsJsonObject("playbackAccessToken")
                } else null
                val qualities = if (clip.has("videoQualities") && !clip.get("videoQualities").isJsonNull) {
                    clip.getAsJsonArray("videoQualities")
                } else null
                if (tokenObj == null || qualities == null || qualities.size() == 0) {
                    return list
                }
                val sig = tokenObj.get("signature").asString
                val token = URLEncoder.encode(tokenObj.get("value").asString, "utf-8")
                for (i in 0 until qualities.size()) {
                    val q = qualities.get(i).asJsonObject
                    val sourceUrl = q.get("sourceURL").asString
                    val quality = q.get("quality").asString
                    val fps = if (q.has("frameRate") && !q.get("frameRate").isJsonNull) {
                        Math.round(q.get("frameRate").asDouble).toInt()
                    } else 30
                    val name = quality + "p" + (if (fps >= 50) fps.toString() else "")
                    val url = "$sourceUrl?sig=$sig&token=$token"
                    if (list.isEmpty()) {
                        list.add(StreamQuality("Auto", url))
                    }
                    list.add(StreamQuality(name, url))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
            }
            return list
        }

        private fun moveAudioOnlyToEnd(arrayList: ArrayList<StreamQuality>) {
            val it = arrayList.iterator()
            var audioOnly: StreamQuality? = null
            while (it.hasNext()) {
                val item = it.next()
                if (item.name == "Audio Only" || item.name == "audio_only") {
                    audioOnly = item
                    break
                }
            }
            if (audioOnly != null) {
                arrayList.remove(audioOnly)
                arrayList.add(audioOnly)
            }
        }
    }

    fun setUseStreamToken(bool: Boolean?) {
        this.useStreamToken = bool ?: false
    }

    fun start() {
        Threads.doInBackground { resolveInBackground() }
    }

    private fun resolveInBackground() {
        val id = channelOrVideoId.orEmpty()
        val arrayListFetchQualities: ArrayList<StreamQuality> = when {
            playbackType == Enums.PlaybackKind.LIVE -> fetchQualities(buildLivePlaylistUrl(id))
            isClipSlug(id) -> fetchClipQualities(id)
            else -> fetchQualities(buildVodPlaylistUrl(id))
        }
        Threads.doOnMainThread {
            listener.onQualitiesResolved(arrayListFetchQualities, manifestCluster)
        }
    }

    private fun storeCachedUrl(str: String, str2: String, l2: Long) {
        val entry = liveUrlCache[str] ?: return
        entry.playlistUrl = str2
        entry.state = LoadState.DONE
        entry.setTokenExpiresAt(l2)
    }

    private fun getCachedUrl(str: String): String? {
        val entry = liveUrlCache[str] ?: return null
        if (entry.state == LoadState.LOADING) {
            return ""
        }
        val url = entry.getPlaylistUrlIfValid()
        if (entry.state == LoadState.DONE && url == null) {
            return null
        }
        if (url != null && !LowLatency.matches(url, LowLatency.enabled(contextRef.get()))) {
            return null
        }
        return url
    }

    private fun parseTokenExpiry(str: String): Long {
        return Gson().fromJson(str, JsonObject::class.java).get("expires").asLong
    }

    private fun buildLivePlaylistUrl(str: String): String {
        val cachedUrl = getCachedUrl(str)
        if (cachedUrl != null) {
            return cachedUrl
        }
        liveUrlCache[str] = PlaybackUrlCacheEntry()
        var tokenExpiry = -1L
        var asString = ""
        var asString2 = ""
        val streamToken = AppState.remoteSettings?.streamToken
        val str3 = String.format(
            "{\"query\":\"query {\\r\\n  streamPlaybackAccessToken(channelName: \\\"%s\\\", params: {\\r\\n    platform: \\\"%s\\\",\\r\\n    playerBackend:\\\"%s\\\"\\r\\n    playerType:\\\"%s\\\"\\r\\n  })    {    value    signature    __typename  }  \\r\\n}\",\"variables\":{}}",
            str,
            AppState.remoteSettings?.platform ?: "android",
            AppState.remoteSettings?.playerBackend ?: "mediaplayer",
            AppState.remoteSettings?.playerType ?: "mobile"
        )
        try {
            val request = Http.load("POST", "https://gql.twitch.tv/gql").setHeader("Client-ID", com.shrug.shrugtv.api.gql.GqlApi.WEB_CLIENT_ID)
            if (streamToken != null) {
                request.setHeader("Authorization", "OAuth $streamToken")
            }
            val jsonObject = request.setStringBody(str3).asJsonObject().get()
            if (jsonObject != null) {
                asString = jsonObject.get("data").asJsonObject.get("streamPlaybackAccessToken").asJsonObject.get("value").asString
                try {
                    tokenExpiry = parseTokenExpiry(asString)
                    asString = URLEncoder.encode(asString, "utf-8")
                    asString2 = jsonObject.get("data").asJsonObject.get("streamPlaybackAccessToken").asJsonObject.get("signature").asString
                } catch (e3: Exception) {
                    e3.printStackTrace()
                    CrashReporter.recordException(e3)
                    asString2 = ""
                }
            }
        } catch (e10: Exception) {
            asString = ""
            e10.printStackTrace()
            CrashReporter.recordException(e10)
            asString2 = ""
        }

        val usherDirect = "https://usher.ttvnw.net/api/channel/hls/%s.m3u8?allow_source=true&allow_audio_only=true&playlist_include_framerate=true&type=any"
        // Proxies only serve to avoid ads, so the public edition always plays directly from
        // Twitch; R8 drops the proxy address from that APK.
        val str2 = if (!BuildConfig.AD_FREE) usherDirect else when (proxyServer) {
            Enums.ProxyServer.TOKYO -> "https://api.twitch.tyo.kwabang.net/hls-raw/%s.m3u8"
            Enums.ProxyServer.CUSTOM -> if (proxyUrlTemplate.isNotEmpty()) proxyUrlTemplate else usherDirect
            else -> usherDirect
        }
        var str4 = String.format(
            "$str2?player=twitchweb&&token=%s&sig=%s&allow_audio_only=true&allow_source=true&playlist_include_framerate=true&type=any&p=%s",
            str, asString, asString2, "" + Random().nextInt(6)
        )
        str4 = LowLatency.usherUrl(str4, LowLatency.enabled(contextRef.get()))
        storeCachedUrl(str, str4, tokenExpiry)
        return str4
    }

    private fun buildVodPlaylistUrl(str: String): String {
        var asString = ""
        var asString2 = ""
        try {
            val query = String.format(
                "{\"query\":\"query {\\r\\n  videoPlaybackAccessToken(id: \\\"%s\\\", params: {\\r\\n    platform: \\\"%s\\\",\\r\\n    playerBackend:\\\"%s\\\"\\r\\n    playerType:\\\"%s\\\"\\r\\n  })    {    value    signature    __typename  }  \\r\\n}\",\"variables\":{}}",
                str, "android", "mediaplayer", "mobile"
            )
            val jsonObject = Http.load("POST", "https://gql.twitch.tv/gql")
                .setHeader("Client-ID", com.shrug.shrugtv.api.gql.GqlApi.WEB_CLIENT_ID)
                .setStringBody(query)
                .asJsonObject().get()
            if (jsonObject != null) {
                asString = jsonObject.get("data").asJsonObject.get("videoPlaybackAccessToken").asJsonObject.get("value").asString
                try {
                    vodTokenJson = JsonParser().parse(asString).asJsonObject
                    asString = URLEncoder.encode(asString, "utf-8")
                    asString2 = jsonObject.get("data").asJsonObject.get("videoPlaybackAccessToken").asJsonObject.get("signature").asString
                } catch (e3: Exception) {
                    e3.printStackTrace()
                    CrashReporter.recordException(e3)
                    asString2 = ""
                }
            }
        } catch (e10: Exception) {
            asString = ""
            e10.printStackTrace()
            CrashReporter.recordException(e10)
            asString2 = ""
        }
        return String.format(
            "https://usher.ttvnw.net/vod/%s.m3u8?player=twitchweb&&token=%s&sig=%s&allow_audio_only=true&allow_source=true&playlist_include_framerate=true&type=any&p=%s",
            str, asString, asString2, "" + Random().nextInt(6)
        )
    }

    private fun parseMasterPlaylist(str: String, str2: String): ArrayList<StreamQuality> {
        if (playbackType == Enums.PlaybackKind.VOD && (str.contains("vod_manifest_restricted") || str.contains("unauthorized_entitlements") || str.contains("Forbidden"))) {
            val jsonObject = vodTokenJson
            return RestrictedVodPlaylist.buildQualities(
                contextRef.get(),
                channelOrVideoId.orEmpty(),
                if (jsonObject != null && jsonObject.has("chansub") && !jsonObject.get("chansub").isJsonNull) {
                    jsonObject.getAsJsonObject("chansub").getAsJsonArray("restricted_bitrates")
                } else null
            )
        }
        return qualitiesFromMaster(str, str2)
    }

    private fun fetchClipQualities(slug: String): ArrayList<StreamQuality> {
        return try {
            val body = String.format(
                Locale.ROOT,
                "{\"query\":\"{ clip(slug: \\\"%s\\\") { videoQualities { frameRate quality sourceURL } playbackAccessToken(params: { platform: \\\"web\\\", playerBackend: \\\"mediaplayer\\\", playerType: \\\"site\\\" }) { signature value } } }\"}",
                slug
            )
            val req = Http.load("POST", "https://gql.twitch.tv/gql")
                .setHeader("Client-ID", com.shrug.shrugtv.api.gql.GqlApi.WEB_CLIENT_ID)
                .setHeader("Content-Type", "application/json")
                .setStringBody(body)
            qualitiesFromClip(req.asJsonObject().get())
        } catch (e: Exception) {
            e.printStackTrace()
            CrashReporter.recordException(e)
            ArrayList()
        }
    }

    private fun fetchQualities(str: String): ArrayList<StreamQuality> {
        if (str.isEmpty()) {
            return ArrayList()
        }
        var str2: String
        try {
            str2 = Http.load(str).asString().get()
        } catch (e3: Exception) {
            e3.printStackTrace()
            str2 = ""
        }
        if (playbackType == Enums.PlaybackKind.LIVE) {
            TwitchAdBlocker.registerMaster(str, channelOrVideoId.orEmpty(), str2)
        }
        parseManifestCluster(str2)
        return parseMasterPlaylist(str2, str)
    }

    private fun parseManifestCluster(str: String) {
        try {
            val matcher = Pattern.compile("MANIFEST-CLUSTER=\"(.*)\",ORIGIN=\"(.*)?(?=\",C)").matcher(str)
            var strGroup = ""
            while (matcher.find()) {
                strGroup = matcher.group(1) ?: ""
            }
            manifestCluster = strGroup
        } catch (e3: Exception) {
            CrashReporter.recordException(e3)
            e3.printStackTrace()
        }
    }
}
