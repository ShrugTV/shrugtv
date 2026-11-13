package com.shrug.shrugtv.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.shrug.shrugtv.ui.tv.home.HomeActivity
import com.shrug.shrugtv.util.AppLanguage

/**
 * Formerly the Leanback home screen (MainFragment). Since  (step 7) it only
 * forwards to the new home screen, so intents that still name it (Android TV channels, shortcuts,
 * stored PendingIntents) keep working.
 */
class MainActivity : Activity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val target = Intent(this, HomeActivity::class.java)
        intent?.extras?.let {
            target.putExtras(it)
        }
        startActivity(target)
        finish()
    }
}
