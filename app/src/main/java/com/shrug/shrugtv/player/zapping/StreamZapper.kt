package com.shrug.shrugtv.player.zapping

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.apihelpers.LiveStreamHistory
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.player.PlaybackUrlResolver
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.session.UserSession
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.Threads
import com.shrug.shrugtv.util.TimeUtil
import java.lang.ref.WeakReference
import java.util.ArrayList

class StreamZapper(context: Context?, private val listener: Listener) {

    fun interface Listener {
        fun onNeighbourResolved(movie: Movie?, direction: Int, neighbours: Array<Movie?>?, multiview: Boolean)
    }

    private val contextRef: WeakReference<Context?> = WeakReference(context)
    private var currentItem: Movie? = null
    private var direction: Int = 0
    private var multiview: Boolean = false
    private var pageOffset: Int = 0
    private var prefetchOnly: Boolean = false
    private val prefetchedItems: MutableList<Movie> = ArrayList()
    private var retryCount: Int = 0

    companion object {
        @JvmStatic
        fun compareByViewersDesc(movie: Movie, movie2: Movie): Int {
            return movie2.viewerCount - movie.viewerCount
        }

        @JvmStatic
        fun onPlaybackUrlPrefetched(movie: Movie, arrayList: ArrayList<*>?, str: String?) {
            if (!arrayList.isNullOrEmpty()) {
                movie.manifestCluster = str
                @Suppress("UNCHECKED_CAST")
                movie.qualities = arrayList as ArrayList<com.shrug.shrugtv.model.StreamQuality>
                val first = arrayList[0] as com.shrug.shrugtv.model.StreamQuality
                movie.setPlaybackUrl(first.url)
            }
        }
    }

    fun zap(movie: Movie?, i: Int, z: Boolean) {
        this.multiview = z
        Threads.doInBackground { resolveInBackground(movie, i) }
    }

    fun setPrefetchOnly(z: Boolean) {
        this.prefetchOnly = z
    }

    fun setStartIndex(i: Int) {
        this.pageOffset = if (i <= 50) 0 else i - 50
    }

    fun prefetchAroundCurrent(arrayList: ArrayList<Movie>) {
        var i = 0
        val current = currentItem ?: return
        for (item in arrayList) {
            if (item.userId == current.userId) {
                prefetchAround(i, arrayList)
            }
            i++
        }
    }

    private fun resolveInBackground(movie: Movie?, i: Int) {
        if (movie == null || movie.source == null || movie.source == Enums.RowType.SEARCH_STREAM) {
            return
        }
        this.currentItem = movie
        this.direction = i
        when (movie.source) {
            Enums.RowType.LANGUAGE -> loadLanguage()
            Enums.RowType.FOLLOWED -> loadFollowed()
            Enums.RowType.GAME -> loadGame()
            Enums.RowType.GAME_LANGUAGE -> loadGameLanguage()
            Enums.RowType.TOP_STREAMS -> loadTopStreams()
            Enums.RowType.LIVE_CHANNEL_HISTORY -> loadLiveHistory()
            else -> {}
        }
    }

    private fun loadFollowed() {
        try {
            HelixApi.getFollowedStreams(contextRef.get()) { exc, obj ->
                onFollowedResponse(exc, obj as? JsonObject)
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun onFollowedResponse(exc: Exception?, jsonObject: JsonObject?) {
        if (exc != null) {
            try {
                exc.printStackTrace()
                CrashReporter.recordException(exc)
            } catch (e3: Exception) {
                e3.printStackTrace()
                CrashReporter.recordException(e3)
                return
            }
        }
        val asJsonArray = jsonObject?.getAsJsonArray("data") ?: return
        if (asJsonArray.size() <= 0) return
        Threads.doInBackground { onFollowedParsed(asJsonArray) }
    }

    private fun onFollowedParsed(jsonArray: JsonArray) {
        val streams = HelixParser.parseStreams(contextRef.get(), Enums.RowType.FOLLOWED, jsonArray)
        streams.sortWith { obj, obj2 -> compareByViewersDesc(obj, obj2) }
        UserSession.moveFavoritesToFront(streams)
        onListLoaded(streams)
    }

    private fun loadGameLanguage() {
        val current = currentItem ?: return
        try {
            HelixApi.getStreamsByGameAndLanguage(contextRef.get(), current.gameId, "") { exc, obj ->
                onGameLanguageResponse(exc, obj as? JsonObject)
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun onGameLanguageResponse(exc: Exception?, jsonObject: JsonObject?) {
        if (exc != null) {
            try {
                exc.printStackTrace()
                CrashReporter.recordException(exc)
            } catch (e3: Exception) {
                e3.printStackTrace()
                CrashReporter.recordException(e3)
                return
            }
        }
        val asJsonArray = jsonObject?.getAsJsonArray("data") ?: return
        Threads.doInBackground { onGameLanguageParsed(asJsonArray) }
    }

    private fun onGameLanguageParsed(jsonArray: JsonArray) {
        onListLoaded(HelixParser.parseStreams(contextRef.get(), Enums.RowType.GAME_LANGUAGE, jsonArray))
    }

    private fun loadGame() {
        val current = currentItem ?: return
        try {
            HelixApi.getStreamsByGame(contextRef.get(), current.gameId, "") { exc, obj ->
                onGameResponse(exc, obj as? JsonObject)
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun onGameResponse(exc: Exception?, jsonObject: JsonObject?) {
        if (exc != null) {
            try {
                exc.printStackTrace()
                CrashReporter.recordException(exc)
            } catch (e3: Exception) {
                e3.printStackTrace()
                CrashReporter.recordException(e3)
                return
            }
        }
        val asJsonArray = jsonObject?.getAsJsonArray("data") ?: return
        Threads.doInBackground { onGameParsed(asJsonArray) }
    }

    private fun onGameParsed(jsonArray: JsonArray) {
        onListLoaded(HelixParser.parseStreams(contextRef.get(), Enums.RowType.GAME, jsonArray))
    }

    private fun loadLanguage() {
        try {
            HelixApi.getStreamsByLanguage(contextRef.get(), "") { exc, obj ->
                onLanguageResponse(exc, obj as? JsonObject)
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun onLanguageResponse(exc: Exception?, jsonObject: JsonObject?) {
        if (exc != null) {
            try {
                exc.printStackTrace()
                CrashReporter.recordException(exc)
            } catch (e3: Exception) {
                e3.printStackTrace()
                CrashReporter.recordException(e3)
                return
            }
        }
        val asJsonArray = jsonObject?.getAsJsonArray("data") ?: return
        Threads.doInBackground { onLanguageParsed(asJsonArray) }
    }

    private fun onLanguageParsed(jsonArray: JsonArray) {
        onListLoaded(HelixParser.parseStreams(contextRef.get(), Enums.RowType.LANGUAGE, jsonArray))
    }

    private fun loadLiveHistory() {
        val ctx = contextRef.get() ?: return
        try {
            LiveStreamHistory.getLiveChannels(ctx) { arrayList ->
                onListLoaded(arrayList ?: ArrayList())
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun loadTopStreams() {
        try {
            HelixApi.getTopStreams(contextRef.get(), "") { exc, obj ->
                onTopStreamsResponse(exc, obj as? JsonObject)
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }

    private fun onTopStreamsResponse(exc: Exception?, jsonObject: JsonObject?) {
        if (exc != null) {
            try {
                exc.printStackTrace()
                CrashReporter.recordException(exc)
            } catch (e3: Exception) {
                e3.printStackTrace()
                CrashReporter.recordException(e3)
                return
            }
        }
        val asJsonArray = jsonObject?.getAsJsonArray("data") ?: return
        if (asJsonArray.size() <= 0) return
        Threads.doInBackground { onTopStreamsParsed(asJsonArray) }
    }

    private fun onTopStreamsParsed(jsonArray: JsonArray) {
        onListLoaded(HelixParser.parseStreams(contextRef.get(), Enums.RowType.TOP_STREAMS, jsonArray))
    }

    private fun onListLoaded(arrayList: ArrayList<Movie>) {
        if (prefetchOnly) {
            prefetchOnly = false
            prefetchAroundCurrent(arrayList)
            return
        }
        val current = currentItem ?: return
        var i = 0
        var found = false
        for (item in arrayList) {
            if (item.userId == current.userId) {
                try {
                    val movieArr = arrayOfNulls<Movie>(2)
                    val i3 = direction + i
                    if (i3 < 0 && pageOffset == 0) {
                        movieArr[0] = null
                        movieArr[1] = if (arrayList.size >= 2) arrayList[i + 1] else null
                        listener.onNeighbourResolved(null, direction, movieArr, multiview)
                        return
                    }
                    if (arrayList.size <= i3 && arrayList.size < 100) {
                        movieArr[1] = null
                        movieArr[0] = if (arrayList.size >= 2) arrayList[i - 1] else null
                        listener.onNeighbourResolved(null, direction, movieArr, multiview)
                        return
                    }
                    if (pageOffset > 0 && arrayList.size < 100 && direction == -1) {
                        pageOffset--
                    }
                    if (pageOffset == 0 && i > 50 && arrayList.size == 100) {
                        pageOffset += direction
                    } else if (pageOffset > 0 && arrayList.size == 100) {
                        pageOffset += direction
                    }
                    if (pageOffset < 0) {
                        pageOffset = 0
                    }
                    val movie = arrayList.getOrNull(i3)
                    if (movie != null) {
                        val prefetched = findPrefetched(movie)
                        if (prefetched?.playbackUrlIfFresh != null) {
                            movie.qualities = prefetched.qualities
                            movie.setPlaybackUrlWithTime(prefetched.playbackUrlIfFresh, prefetched.playbackUrlFetchedAt)
                            movie.manifestCluster = prefetched.manifestCluster
                        }
                        val i6 = i3 - 1
                        if (i6 in arrayList.indices) {
                            movieArr[0] = arrayList[i6]
                        }
                        val i10 = i3 + 1
                        if (i10 in arrayList.indices) {
                            movieArr[1] = arrayList[i10]
                        }
                        listener.onNeighbourResolved(movie, direction, movieArr, multiview)
                        retryCount = 0
                        found = true
                        break
                    }
                } catch (e10: Exception) {
                    e10.printStackTrace()
                    CrashReporter.recordException(e10)
                }
            }
            i++
        }
        if (found) {
            prefetchAround(i, arrayList)
        } else {
            retryNextPage()
        }
    }

    private fun retryNextPage() {
        val i = retryCount
        if (i > 10) {
            listener.onNeighbourResolved(null, 0, arrayOfNulls<Movie>(2), multiview)
            return
        }
        pageOffset = i * 100
        retryCount = i + 1
        zap(currentItem, direction, multiview)
    }

    private fun prefetchAround(i: Int, arrayList: ArrayList<Movie>) {
        val arrayList2 = ArrayList<Movie>()
        try {
            currentItem?.let { rememberPrefetched(it) }
            for (i3 in 1..4) {
                val i6 = i + i3
                if (i6 in arrayList.indices) {
                    val movie = arrayList[i6]
                    rememberPrefetched(movie)
                    arrayList2.add(movie)
                }
            }
            for (i10 in -1 downTo -4) {
                val i11 = i + i10
                if (i11 >= 0 && i11 < arrayList.size) {
                    val movie2 = arrayList[i11]
                    rememberPrefetched(movie2)
                    arrayList2.add(movie2)
                }
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
        prefetchItems(arrayList2)
    }

    private fun rememberPrefetched(movie: Movie) {
        val exists = prefetchedItems.any { it.userId == movie.userId }
        if (!exists) {
            prefetchedItems.add(movie)
        }
    }

    private fun findPrefetched(movie: Movie): Movie? {
        return prefetchedItems.firstOrNull { it.userId == movie.userId }
    }

    private fun prefetchItems(list: List<Movie>) {
        val ctx = contextRef.get() ?: return
        try {
            for (movie in list) {
                for (movie2 in prefetchedItems) {
                    if (movie.userId == movie2.userId && movie2.playbackUrlIfFresh == null) {
                        val playbackUrlResolver = PlaybackUrlResolver(ctx, movie2.playbackId, movie2.playbackType) { arrayList, str ->
                            onPlaybackUrlPrefetched(movie2, arrayList, str)
                        }
                        playbackUrlResolver.setUseStreamToken(false)
                        playbackUrlResolver.start()
                        Glide.with(ctx)
                            .load(movie2.cardImageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .apply(RequestOptions().signature(ObjectKey(AppState.appStartTime ?: "")))
                            .submit()
                        Glide.with(ctx)
                            .load(movie2.avatarUrl)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .apply(RequestOptions().override(200, 200))
                            .apply(RequestOptions().signature(ObjectKey(TimeUtil.dayOfYear())))
                            .submit()
                    }
                }
            }
        } catch (e3: Exception) {
            e3.printStackTrace()
            CrashReporter.recordException(e3)
        }
    }
}
