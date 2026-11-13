package com.shrug.shrugtv.ui.tv.dialogs

import android.os.Bundle
import com.shrug.shrugtv.ui.tv.TvShapes
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.shrug.shrugtv.R
import com.shrug.shrugtv.database.AppDatabase
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.components.glassButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Yes/no prompt in the new style (step 5), replaces the Leanback
 * GuidedStep dialogs. Focus starts on "Cancel" so an accidental OK does nothing.
 */
@Composable
fun Confirmation(question: String, text: String, yes: String, execute: () -> Unit, cancel: () -> Unit) {
    val no = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { no.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(440.dp).background(TvColors.Surface, TvShapes.r(16.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(question, fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
            if (text.isNotEmpty()) Text(text, fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                Button(onClick = execute, colors = glassButton()) { Text(yes, fontFamily = Inter, fontWeight = FontWeight.Bold) }
                Button(onClick = cancel, colors = glassButton(), modifier = Modifier.focusRequester(no)) { Text(stringResource(R.string.dialog_cancel), fontFamily = Inter) }
            }
        }
    }
}

/** Sign out: clears token and user like the old LogoutStepFragment. */
class LogoutActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvHomeTheme {
                Confirmation(stringResource(R.string.dialog_logout_question), stringResource(R.string.dialog_logout_text),
                    stringResource(R.string.dialog_logout_confirm),
                    execute = { logOut(); finish() }, cancel = { finish() })
            }
        }
    }

    private fun logOut() {
        // Revoke the tokens at Twitch ( item 5); keeps running even after the dialog has closed
        val prefs = getSharedPreferences("User", 0)
        val access = prefs.getString("TOKEN", "") ?: ""
        val refresh = prefs.getString("REFRESH_TOKEN", "") ?: ""
        Thread { com.shrug.shrugtv.login.TwitchDeviceAuth.revoke(access, refresh) }.start()
        prefs.edit().remove("REFRESH_TOKEN").remove("TOKEN_EXPIRES_AT").apply()
        AppState.userLogin = ""
        AppState.userId = ""
        AppState.accessToken = ""
        AppState.isTurbo = false
        AppState.userLoggedOut = true
        getSharedPreferences("User", 0).edit()
            .putString("TOKEN", "").putString("USER_ID", "").putString("USER_NAME", "")
            .putBoolean("IS_TURBO", false).putBoolean("USER_LOGGED_OUT", true).apply()
    }
}

/** Clear the VOD history (saved positions), like the old ClearVodHistoryStepFragment. */
class ClearHistoryActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvHomeTheme {
                Confirmation(stringResource(R.string.dialog_vod_history_question), stringResource(R.string.dialog_vod_history_text),
                    stringResource(R.string.dialog_vod_history_confirm),
                    execute = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { AppDatabase.getInstance(applicationContext).vodInfoDao().clearHistory() }
                            Toast.makeText(applicationContext, getString(R.string.dialog_vod_history_cleared), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    cancel = { finish() })
            }
        }
    }
}
