package com.shrug.shrugtv.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.player.FrameRateMatch
import com.shrug.shrugtv.player.HeroHandover
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.components.Destinations
import com.shrug.shrugtv.ui.tv.player.PlayerScreen
import com.shrug.shrugtv.ui.tv.player.PlayerViewModel
import com.shrug.shrugtv.util.AppLanguage

/**
 * Moderner TV Player in Jetpack Compose (Stage 4).
 *
 * Replaces the original 9,102-line decompilation file `PlayerActivity.java`:
 * - 100% Jetpack Compose for TV (`PlayerScreen`)
 * - Seamless takeover of the hero preview from the home screen (`HeroHandover`)
 * - Live-Streams, VODs und Multiview
 * - TV remote key control
 * - Picture-in-picture support
 */
open class PlayerActivity : ComponentActivity() {

    protected lateinit var viewModel: PlayerViewModel

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Through ViewModelProvider: only then does Android call onCleared() when the activity
        // finishes, which releases the players, the media session and the chat. Created with
        // `PlayerViewModel(...)` directly, nothing was ever released (docs/bug_review.md B1).
        val appContext = applicationContext
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlayerViewModel(appContext) as T
        })[PlayerViewModel::class.java]

        // After a recreation the ViewModel still plays; only a fresh start reads the intent
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            TvHomeTheme {
                com.shrug.shrugtv.ui.tv.VideoTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onNavigateToChannel = { movie ->
                        Destinations.channel(this@PlayerActivity, movie)
                    }
                )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val movieExtraKey = getString(R.string.movie)
        @Suppress("DEPRECATION")
        val movie = (intent.getSerializableExtra(movieExtraKey) as? Movie)
            ?: movieFromDeepLink(intent.data)

        @Suppress("DEPRECATION")
        val multiviewMovie = intent.getSerializableExtra(EXTRA_MULTIVIEW) as? Movie

        if (movie != null) {
            val adopted = HeroHandover.take()
            viewModel.play(movie, adopted)
        }

        if (multiviewMovie != null) {
            viewModel.startMultiview(multiviewMovie)
        }
    }

    private fun movieFromDeepLink(uri: Uri?): Movie? {
        if (uri == null) return null
        val scheme = uri.scheme
        val host = uri.host ?: return null

        if (scheme == "shrugtv" && host == "stream") {
            val login = uri.lastPathSegment ?: return null
            return Movie().apply {
                this.login = login
                this.displayName = login
                this.itemType = Enums.ContentType.STREAM
            }
        }

        if (scheme == "shrugtv" && host == "vod") {
            val vodId = uri.lastPathSegment ?: return null
            return Movie().apply {
                this.setVideoIdFromString(vodId)
                this.itemType = Enums.ContentType.VOD
            }
        }

        if (scheme == "channel" && host == "com.shrug.shrugtv") {
            val segments = uri.pathSegments
            // channel://com.shrug.shrugtv/startstream/<id>; a single segment used to crash (B9)
            val channelId = segments?.getOrNull(1) ?: segments?.getOrNull(0)
            if (channelId != null) {
                return Movie().apply {
                    this.userId = channelId.toIntOrNull() ?: 0
                    this.itemType = Enums.ContentType.STREAM
                }
            }
        }

        return null
    }

    override fun onStart() {
        super.onStart()
        viewModel.resumeFromBackground()
    }

    override fun onStop() {
        super.onStop()
        // Home button: stop playing behind the launcher
        viewModel.pauseForBackground()
    }

    companion object {
        const val EXTRA_MULTIVIEW = "com.shrug.shrugtv.MULTIVIEW"
    }
}
