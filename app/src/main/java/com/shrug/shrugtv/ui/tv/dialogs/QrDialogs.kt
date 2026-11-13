package com.shrug.shrugtv.ui.tv.dialogs

import android.content.Context
import com.shrug.shrugtv.ui.tv.TvShapes
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.home.NetImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QR dialogs in the new style (step 7). Replace FollowActivity and
 * OpenChatInAppQRcodeActivity: following and chatting only work in the official app, so the TV
 * shows a QR code with the channel or chat address. They expect the extra "Stream" and return the
 * same result codes as the old activities (36797 and 36001) on close, so callers reload the
 * follow state.
 */
abstract class QrActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }
    /** String resources of title and hint; read in onCreate, where the app language applies. */
    @get:androidx.annotation.StringRes protected abstract val title: Int
    @get:androidx.annotation.StringRes protected abstract val hint: Int
    protected abstract val response: Int
    protected abstract fun address(m: Movie): String?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val m = intent.getSerializableExtra(EXTRA) as? Movie
        val url = m?.let { address(it) }
        if (m == null || url.isNullOrEmpty()) { finish(); return }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { setResult(response, Intent()); finish() }
        })
        setContent { TvHomeTheme { QrContent(m, getString(title), getString(hint), url) } }
    }

    companion object {
        const val EXTRA = "Stream"
    }
}

/** QR code for the channel page: follow/unfollow in the official app. */
class FollowQrActivity : QrActivity() {
    override val title = R.string.dialog_qr_follow_title
    override val hint = R.string.dialog_qr_follow_hint
    override val response = 36797
    override fun address(m: Movie): String? = m.webUrl

    companion object {
        fun intent(c: Context, m: Movie) = Intent(c, FollowQrActivity::class.java).putExtra(EXTRA, m)
    }
}

/** QR code for the pop-out chat: write in the chat in the official app or the browser. */
class ChatQrActivity : QrActivity() {
    override val title = R.string.dialog_qr_chat_title
    override val hint = R.string.dialog_qr_chat_hint
    override val response = 36001
    override fun address(m: Movie): String? = m.popoutChatUrl

    companion object {
        fun intent(c: Context, m: Movie) = Intent(c, ChatQrActivity::class.java).putExtra(EXTRA, m)
    }
}

private suspend fun qr(text: String): Bitmap? = withContext(Dispatchers.Default) {
    runCatching { com.shrug.shrugtv.util.QrCode.generate(text, 512, 1) }.getOrNull()
}

@Composable
private fun QrContent(m: Movie, title: String, hint: String, url: String) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) { image = qr(url) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Row(
            Modifier.background(TvColors.Surface, TvShapes.r(20.dp)).padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(220.dp).clip(TvShapes.r(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                image?.let { Image(it.asImageBitmap(), contentDescription = stringResource(R.string.dialog_qr_code), filterQuality = FilterQuality.None, modifier = Modifier.size(204.dp)) }
            }
            Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NetImage(m.avatarUrl, Modifier.size(36.dp).clip(RoundedCornerShape(50)), width = 150, height = 150)
                    Text(m.displayName ?: m.login ?: "", fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TvColors.TextMedium)
                }
                Text(title, fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                Text(hint, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
                Text(url, fontFamily = Inter, fontSize = 10.sp, color = TvColors.AccentLight)
            }
        }
    }
}
