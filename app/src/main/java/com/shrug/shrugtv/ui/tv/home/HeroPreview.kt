package com.shrug.shrugtv.ui.tv.home

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultHttpDataSource
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.player.hls.TwitchHlsPlaylistParserFactory
import kotlinx.coroutines.delay

/**
 * Hero background: still image, and after a short pause the muted live preview of the selected stream
 * on top. Uses the same HLS parser as the player, so it also goes through the ad
 * blocker; resolution capped at 720p to save bandwidth. [active] = false (activity in the background)
 * pauses the preview.
 */
@Composable
fun HeroPreview(movie: Movie?, active: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Not keyed to movie: the player listener otherwise holds on to the state of the first movie and
    // never reported "ready" for any later one (2026-10-28). The LaunchedEffect below
    // resets it when the movie changes.
    var ready by remember { mutableStateOf(false) }
    val visible by animateFloatAsState(if (ready) 1f else 0f, tween(600), label = "vorschau")

    // The player can be handed over to PlayerActivity; the preview then gets a fresh
    // one. It does not release handed-over players, the player does that.
    var player by remember { mutableStateOf(com.shrug.shrugtv.player.PlayerFactory.createPreviewPlayer(context)) }
    val handedOver = remember { mutableSetOf<ExoPlayer>() }
    var surface by remember { mutableStateOf<SurfaceView?>(null) }
    DisposableEffect(player) {
        val p = player
        val l = object : Player.Listener {
            override fun onRenderedFirstFrame() { if (p === player) ready = true; android.util.Log.i("HeroPreview", "first frame") }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { android.util.Log.w("HeroPreview", "Error: " + error.errorCodeName) }
        }
        p.addListener(l)
        onDispose {
            p.removeListener(l)
            if (p !in handedOver) p.release()
        }
    }
    DisposableEffect(player, surface) {
        surface?.let { player.setVideoSurfaceView(it) }
        onDispose {}
    }
    val current by androidx.compose.runtime.rememberUpdatedState(movie)
    val running by androidx.compose.runtime.rememberUpdatedState(ready && active)
    DisposableEffect(Unit) {
        val provider: (Movie) -> ExoPlayer? = provider@{ m ->
            val p = player
            val now = current
            if (!running || now == null || now.login != m.login || p.currentMediaItem == null) {
                android.util.Log.i("HeroPreview", "no handover: running=" + running + " hero=" + now?.login + " chosen=" + m.login + " item=" + (p.currentMediaItem != null))
                return@provider null
            }
            surface?.let { p.clearVideoSurfaceView(it) }
            handedOver.add(p)
            player = com.shrug.shrugtv.player.PlayerFactory.createPreviewPlayer(context)
            android.util.Log.i("HeroPreview", "handed over to the player: " + m.login)
            p
        }
        com.shrug.shrugtv.player.HeroHandover.provider = provider
        onDispose { if (com.shrug.shrugtv.player.HeroHandover.provider === provider) com.shrug.shrugtv.player.HeroHandover.provider = null }
    }

    // The setting "Show Stream preview" used to apply only to the old cards, not to the hero
    val previewOn = remember {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_show_stream_preview", true)
    }

    LaunchedEffect(movie, active, player) {
        player.stop()
        player.clearMediaItems()
        ready = false
        if (movie == null || !active || !previewOn) return@LaunchedEffect
        delay(800) // only start once focus rests for a moment
        val url = HomeData.previewUrl(context.applicationContext, movie)
        android.util.Log.i("HeroPreview", "URL for " + movie.login + ": " + (url != null))
        if (url == null) return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true
    }

    Box(modifier) {
        // SurfaceView instead of TextureView (performance): with TextureView the whole home screen redrew
        // every video frame, 58 frames/s when idle, 76 % of them too slow on the Fire TV and keys delayed.
        // The system composes the SurfaceView itself. It cannot be cross-faded; so the still image lies on
        // top and fades out as soon as the video runs.
        // No PlayerView: its control layout needs theme attributes of the old activities (2026-10-20).
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx -> SurfaceView(ctx).also { surface = it } },
                modifier = Modifier.requiredSize(maxWidth, maxWidth * 9f / 16f),
            )
        }
        NetImage(url = movie?.cardImageUrl, modifier = Modifier.fillMaxSize().alpha(1f - visible), width = 1280, height = 720, placeholder = Color(0xFF2A2340), crossfade = true)
    }
}


