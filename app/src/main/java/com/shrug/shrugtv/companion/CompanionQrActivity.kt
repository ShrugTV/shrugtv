package com.shrug.shrugtv.companion

import android.graphics.Bitmap
import com.shrug.shrugtv.ui.tv.TvShapes
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "Connect a phone": QR code with the one-time pairing link, plus address and PIN (docs/companion_plan.md 3). */
class CompanionQrActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val offer = Companion.newPairing()
        setContent { TvHomeTheme { CompanionQrContent(offer) } }
    }
}

@Composable
private fun CompanionQrContent(offer: Triple<String, String, String>?) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(offer) {
        if (offer != null) image = withContext(Dispatchers.Default) {
            runCatching { com.shrug.shrugtv.util.QrCode.generate(offer.first, 512, 1) }.getOrNull()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Row(
            Modifier.background(TvColors.Surface, TvShapes.r(20.dp)).padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offer != null) {
                Box(Modifier.size(240.dp).clip(TvShapes.r(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    image?.let {
                        Image(it.asImageBitmap(), contentDescription = stringResource(R.string.dialog_qr_code), filterQuality = FilterQuality.None, modifier = Modifier.size(224.dp))
                    }
                }
            }
            Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_companion_pair), fontFamily = Inter, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                if (offer == null) {
                    Text(stringResource(R.string.companion_no_network), fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMedium)
                } else {
                    Text(stringResource(R.string.companion_qr_steps), fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMedium)
                    Text(offer.second, fontFamily = Inter, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TvColors.AccentLight)
                    Text(stringResource(R.string.companion_pin, offer.third), fontFamily = Inter, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
                    Text(stringResource(R.string.companion_qr_valid), fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
                }
            }
        }
    }
}
