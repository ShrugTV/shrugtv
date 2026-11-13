package com.shrug.shrugtv.channels

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.JsonObject
import com.shrug.shrugtv.R
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.database.VodInfo
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.session.UserSession
import com.shrug.shrugtv.telemetry.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Modernized CoroutineWorker maintaining Android TV launcher channels and Google TV
 * "Continue Watching" / Watch Next row.
 */
class RecommendationWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val TAG = "RecommendationWorker"
        private val PREVIEW_PROGRAM_URI: Uri = Uri.parse("content://android.media.tv/preview_program")
        private val CHANNEL_PROJECTION = arrayOf("_id", "display_name", "browsable")
        private val WATCH_NEXT_PROJECTION = arrayOf(
            TvContractCompat.WatchNextPrograms._ID,
            TvContractCompat.WatchNextPrograms.COLUMN_INTENT_URI
        )

        @JvmStatic
        fun syncWatchNext(context: Context) {
            try {
                val request = androidx.work.OneTimeWorkRequestBuilder<RecommendationWorker>().build()
                androidx.work.WorkManager.getInstance(context).enqueue(request)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue WatchNext sync: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 26 || !isLeanbackDevice()) {
            return@withContext Result.success()
        }

        try {
            removeFeaturedChannel()
            updateChannels()
            updateWatchNext()
        } catch (e: Exception) {
            Log.e(TAG, "Error in RecommendationWorker: ${e.message}", e)
            CrashReporter.recordException(e)
        }

        Result.success()
    }

    private suspend fun updateChannels() {
        var followed = findChannel("Followed")
        var topStreams = findChannel("Top Streams")

        if (AppState.userLoggedOut) {
            Log.d(TAG, "User logged out, clearing channels")
            followed?.let { deleteChannel(it.channelId) }
            topStreams?.let { deleteChannel(it.channelId) }
            return
        }

        if (followed == null) {
            val id = createChannel("Followed")
            if (id != -1L) {
                followed = RecommendationChannel(id, true)
                TvContractCompat.requestChannelBrowsable(context, id)
            }
        }
        followed?.let { channel ->
            if (channel.isValid) {
                loadAndPublishFollowedStreams(channel.channelId)
            }
        }

        if (topStreams == null) {
            val id = createChannel("Top Streams")
            if (id != -1L) {
                topStreams = RecommendationChannel(id, false)
                TvContractCompat.requestChannelBrowsable(context, id)
            }
        }
        topStreams?.let { channel ->
            if (channel.isValid) {
                loadAndPublishTopStreams(channel.channelId)
            }
        }
    }

    private suspend fun loadAndPublishFollowedStreams(channelId: Long) {
        val jsonObject = fetchFollowedStreamsJson() ?: run {
            deletePrograms(channelId)
            return
        }
        val dataArray = jsonObject.getAsJsonArray("data")
        if (dataArray == null || dataArray.size() == 0) {
            deletePrograms(channelId)
            return
        }

        val rowType = Enums.RowType.FOLLOWED
        val streams = HelixParser.parseStreams(context, rowType, dataArray)
        streams.sortWith { a, b -> b.viewerCount - a.viewerCount }
        UserSession.moveFavoritesToFront(streams)
        publishPrograms(streams, channelId, rowType)
    }

    private suspend fun loadAndPublishTopStreams(channelId: Long) {
        val jsonObject = fetchTopStreamsJson() ?: run {
            deletePrograms(channelId)
            return
        }
        val dataArray = jsonObject.getAsJsonArray("data")
        if (dataArray == null || dataArray.size() == 0) {
            deletePrograms(channelId)
            return
        }

        val rowType = Enums.RowType.TOP_STREAMS
        val streams = HelixParser.parseStreams(context, rowType, dataArray)
        if (streams.isEmpty()) {
            deletePrograms(channelId)
        } else {
            publishPrograms(streams, channelId, rowType)
        }
    }

    private suspend fun fetchFollowedStreamsJson(): JsonObject? = suspendCancellableCoroutine { cont ->
        HelixApi.getFollowedStreams(context) { exc, obj ->
            if (exc != null) {
                CrashReporter.recordException(exc)
                cont.resume(null)
            } else {
                cont.resume(obj as? JsonObject)
            }
        }
    }

    private suspend fun fetchTopStreamsJson(): JsonObject? = suspendCancellableCoroutine { cont ->
        HelixApi.getTopStreams(context, "") { exc, obj ->
            if (exc != null) {
                CrashReporter.recordException(exc)
                cont.resume(null)
            } else {
                cont.resume(obj as? JsonObject)
            }
        }
    }

    private fun publishPrograms(streams: List<Movie>, channelId: Long, rowType: Enums.RowType) {
        try {
            deletePrograms(channelId)
            var weight = streams.size
            for (movie in streams) {
                val favoritePrefix = if (movie.isFavorite) {
                    "${String(Character.toChars(AppState.titlePrefixCodePoint))} | "
                } else ""

                val genreText = "$favoritePrefix${movie.gameName} | ${movie.viewerCountText} viewers"
                val posterUrl = "${movie.cardImageUrl}?${AppState.appStartTime}"
                val intentUri = "channel://com.shrug.shrugtv/startstream/${movie.userId}/${rowType.ordinal}"

                val program = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                    .setTitle(movie.displayName)
                    .setDescription(movie.title)
                    .setGenre(genreText)
                    .setPosterArtUri(Uri.parse(posterUrl))
                    .setIntentUri(Uri.parse(intentUri))
                    .setWeight(weight--)
                    .build()

                context.contentResolver.insert(PREVIEW_PROGRAM_URI, program.toContentValues())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish preview programs: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    /**
     * Updates Google TV / Android TV "Play Next" / Watch Next row with unfinished VODs.
     */
    private fun updateWatchNext() {
        try {
            val db = AppDatabase.getInstance(context) ?: return
            val history = db.vodInfoDao().history ?: return

            // Delete existing WatchNext programs for shrugtv vods to refresh them cleanly
            context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                WATCH_NEXT_PROJECTION,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                val uriIdx = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_INTENT_URI)
                while (cursor.moveToNext()) {
                    val programId = cursor.getLong(idIdx)
                    val intentUri = cursor.getString(uriIdx)
                    if (intentUri != null && (intentUri.startsWith("shrugtv://vod") || intentUri.contains("shrugtv"))) {
                        context.contentResolver.delete(
                            TvContractCompat.buildWatchNextProgramUri(programId),
                            null,
                            null
                        )
                    }
                }
            }

            // Insert top 5 unfinished VODs
            val activeVods = history.filter { it.position > 10 && (it.vodLength <= 0 || it.position < (it.vodLength - 60)) }
                .take(5)

            for (vod in activeVods) {
                val intentUri = Uri.parse("shrugtv://vod/${vod.broadcastId}")
                val durationMs = if (vod.vodLength > 0) vod.vodLength * 1000L else -1L
                val posMs = vod.position * 1000L

                val builder = WatchNextProgram.Builder()
                    .setType(TvContractCompat.WatchNextPrograms.TYPE_CLIP)
                    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                    .setTitle(vod.title ?: "Twitch VOD")
                    .setDescription(vod.channelName ?: "")
                    .setLastPlaybackPositionMillis(posMs.toInt())
                    .setDurationMillis(durationMs.toInt())
                    .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                    .setIntentUri(intentUri)

                vod.videoThumbnail?.let { thumb ->
                    builder.setPosterArtUri(Uri.parse(thumb))
                }

                context.contentResolver.insert(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI,
                    builder.build().toContentValues()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WatchNext programs: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    private fun findChannel(displayName: String): RecommendationChannel? {
        val resolver = context.contentResolver
        resolver.query(TvContractCompat.Channels.CONTENT_URI, CHANNEL_PROJECTION, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                var firstFound: RecommendationChannel? = null
                do {
                    val channel = Channel.fromCursor(cursor)
                    if (displayName == channel.displayName) {
                        if (firstFound == null) {
                            firstFound = RecommendationChannel(channel.id, channel.isBrowsable)
                        } else {
                            deleteChannel(channel.id)
                        }
                    }
                } while (cursor.moveToNext())
                return firstFound
            }
        }
        return null
    }

    private fun createChannel(displayName: String): Long {
        return try {
            val channel = Channel.Builder()
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setAppLinkColor(context.getColor(R.color.theme_red))
                .setAppLinkIntent(Intent(context, com.shrug.shrugtv.ui.tv.home.HomeActivity::class.java))
                .setDisplayName(displayName)
                .build()

            val uri = context.contentResolver.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
                ?: return -1L
            val id = ContentUris.parseId(uri)
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.channel_logo)
            ChannelLogoUtils.storeChannelLogo(context, id, logo)
            id
        } catch (e: Exception) {
            Log.e(TAG, "createChannel failed: ${e.message}", e)
            CrashReporter.recordException(e)
            -1L
        }
    }

    private fun deleteChannel(channelId: Long) {
        try {
            if (channelId != -1L) {
                context.contentResolver.delete(TvContractCompat.buildChannelUri(channelId), null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteChannel failed: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    private fun deletePrograms(channelId: Long) {
        try {
            context.contentResolver.delete(TvContractCompat.buildPreviewProgramsUriForChannel(channelId), null, null)
        } catch (e: Exception) {
            Log.e(TAG, "deletePrograms failed: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    private fun removeFeaturedChannel() {
        findChannel("Featured Streams")?.let {
            deleteChannel(it.channelId)
        }
    }

    private fun isLeanbackDevice(): Boolean {
        return try {
            context.packageManager.hasSystemFeature("android.software.leanback")
        } catch (e: Exception) {
            false
        }
    }
}
