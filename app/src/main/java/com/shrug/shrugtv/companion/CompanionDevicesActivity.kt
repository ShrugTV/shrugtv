package com.shrug.shrugtv.companion

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.TvStyle
import com.shrug.shrugtv.ui.tv.TvShapes
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import kotlinx.coroutines.delay

/**
 * "Connected devices": the phones and computers the companion remembers, with when they were last
 * used. OK on a device and OK again removes it (it needs the code again); "Forget all devices"
 * removes all.
 */
class CompanionDevicesActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvHomeTheme { DevicesContent() } }
    }
}

@Composable
private fun DevicesContent() {
    var list by remember { mutableStateOf(Companion.devices.list()) }
    // First OK arms a device, the second removes it; arming ends after a few seconds
    var armed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(armed) { if (armed != null) { delay(4_000); armed = null } }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(620.dp).background(TvColors.Surface, TvShapes.r(20.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.companion_devices_title), fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
            Text(stringResource(R.string.companion_devices_hint), fontFamily = Inter, fontSize = 13.sp, color = TvColors.TextMedium)
            if (list.isEmpty()) {
                Text(stringResource(R.string.companion_devices_empty), fontFamily = Inter, fontSize = 14.sp, color = TvColors.TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
            LazyColumn(Modifier.heightIn(max = 320.dp), contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(list, key = { it.id }) { d ->
                    val isArmed = armed == d.id
                    ListItem(
                        selected = false,
                        onClick = {
                            if (isArmed) {
                                Companion.forget(d.id)
                                list = Companion.devices.list()
                                armed = null
                            } else {
                                armed = d.id
                            }
                        },
                        modifier = if (d == list.first()) Modifier.focusRequester(first) else Modifier,
                        scale = ListItemDefaults.scale(scale = 1f, focusedScale = 1f),
                        border = ListItemDefaults.border(focusedBorder = androidx.tv.material3.Border(TvStyle.focusStroke, shape = TvShapes.r(10.dp))),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent, contentColor = TvColors.Text,
                            focusedContainerColor = TvColors.SurfaceHigh, focusedContentColor = TvColors.Text,
                        ),
                        headlineContent = { Text(d.name, fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                if (isArmed) stringResource(R.string.companion_devices_confirm)
                                else stringResource(R.string.companion_devices_seen, relative(d.lastSeen), relative(d.pairedAt)),
                                fontFamily = Inter, fontSize = 12.sp,
                                color = if (isArmed) Color(0xFFFF8A80) else TvColors.TextMuted
                            )
                        },
                    )
                }
            }
            if (list.isNotEmpty()) {
                Button(
                    onClick = { Companion.disconnectAll(); list = Companion.devices.list() },
                    modifier = if (list.isEmpty()) Modifier.focusRequester(first) else Modifier,
                    colors = ButtonDefaults.colors(containerColor = TvColors.glass(0.14f), contentColor = TvColors.Text,
                        focusedContainerColor = TvColors.Ink, focusedContentColor = TvColors.Background)
                ) {
                    Text(stringResource(R.string.settings_companion_disconnect), fontFamily = Inter, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun relative(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
