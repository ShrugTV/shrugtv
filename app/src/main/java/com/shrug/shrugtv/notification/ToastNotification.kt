package com.shrug.shrugtv.notification

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.shrug.shrugtv.R
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.telemetry.CrashReporter
import androidx.compose.ui.graphics.toArgb
import com.shrug.shrugtv.ui.tv.theme.ThemeSettings
import com.shrug.shrugtv.util.TimeUtil
import com.shrug.shrugtv.util.UiUtils

/**
 * Modernized ToastNotification showing styled in-app notifications and stream notices.
 */
class ToastNotification {

    companion object {
        const val LAYOUT_LIVE_TEXT = 0
        const val LAYOUT_PLAIN = 1
        const val LAYOUT_GIFTED_SUB = 2
        const val LAYOUT_DETAILED = 3
    }

    private val context: Context
    private val settings: NotificationSettings
    private val layoutType: Int
    private var channelInfo: Movie? = null
    private var message: String? = null
    var toast: Toast? = null
        private set
    private var timer: CountDownTimer? = null

    constructor(context: Context, channelInfo: Movie, settings: NotificationSettings) {
        this.context = context.applicationContext
        this.channelInfo = channelInfo
        this.settings = settings
        this.layoutType = LAYOUT_DETAILED
        buildAndShow()
    }

    constructor(context: Context, message: String, settings: NotificationSettings, layoutType: Int) {
        this.context = context.applicationContext
        this.message = message
        this.settings = settings
        this.layoutType = layoutType
        buildAndShow()
    }

    fun cancel() {
        timer?.cancel()
        timer = null
        toast?.cancel()
    }

    private fun buildAndShow() {
        try {
            val inflater = LayoutInflater.from(context)
            val view: View = when (layoutType) {
                LAYOUT_GIFTED_SUB -> inflater.inflate(R.layout.toast_notification_card_giftedsub, null)
                LAYOUT_DETAILED -> inflater.inflate(R.layout.toast_notification_card_detailed, null)
                else -> inflater.inflate(R.layout.toast_notification_card, null)
            }

            val textView = view.findViewById<TextView>(R.id.notification_text)
            textView?.textSize = settings.textSize

            if (layoutType == LAYOUT_DETAILED) {
                val movie = channelInfo
                val channelTitle = view.findViewById<TextView>(R.id.channelTitle)
                val badgeImage = view.findViewById<ImageView>(R.id.BadgeImage)

                channelTitle?.text = movie?.title ?: ""
                val avatarUrl = movie?.avatarUrl
                if (!avatarUrl.isNullOrEmpty() && badgeImage != null) {
                    Glide.with(context)
                        .load(avatarUrl)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .skipMemoryCache(true)
                        .apply(RequestOptions.circleCropTransform())
                        .apply(RequestOptions.signatureOf(ObjectKey(TimeUtil.dayOfYear())))
                        .into(badgeImage)
                }
            }

            val textFormatted = when (layoutType) {
                LAYOUT_LIVE_TEXT -> formatIsNowLive(message ?: "")
                LAYOUT_PLAIN -> formatPlain(message ?: "")
                LAYOUT_GIFTED_SUB -> formatGiftedSub(message ?: "")
                LAYOUT_DETAILED -> {
                    val login = channelInfo?.displayName ?: channelInfo?.login ?: ""
                    formatIsNowLive(login)
                }
                else -> formatPlain(message ?: "")
            }

            textView?.text = textFormatted
            if (textView?.text.isNullOrEmpty()) return

            val t = Toast(context)
            val paddingPx = UiUtils.dpToPx(30, context)
            val gravity = when (settings.position) {
                0 -> Gravity.TOP or Gravity.START
                1 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                2 -> Gravity.TOP or Gravity.END
                3 -> Gravity.CENTER
                4 -> Gravity.BOTTOM or Gravity.START
                5 -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                6 -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            val xOffset = if (gravity and Gravity.CENTER_HORIZONTAL != 0) 0 else paddingPx
            t.setGravity(gravity, xOffset, paddingPx)
            t.duration = Toast.LENGTH_LONG
            t.view = view
            t.show()
            this.toast = t

            val durationMs = settings.durationMs
            if (durationMs > 0) {
                timer = object : CountDownTimer(durationMs, 1000L) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (Build.VERSION.SDK_INT < 26) {
                            t.show()
                        }
                    }

                    override fun onFinish() {
                        t.cancel()
                    }
                }.start()
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private fun formatIsNowLive(channelName: String): CharSequence {
        val ssb = SpannableStringBuilder()
        try {
            val color = ThemeSettings.theme.value.colors.accent.toArgb()
            val text = context.getString(R.string.notice_is_live, channelName)
            ssb.append(text)
            ssb.setSpan(ForegroundColorSpan(Color.WHITE), 0, ssb.length, 33)
            val start = text.indexOf(channelName)
            if (start >= 0) {
                ssb.setSpan(ForegroundColorSpan(color), start, start + channelName.length, 33)
            }
        } catch (e: Exception) {
            ssb.append(channelName)
        }
        return ssb
    }

    private fun formatGiftedSub(gifter: String): CharSequence {
        val ssb = SpannableStringBuilder()
        try {
            val text = context.getString(R.string.notice_gifted_sub, gifter)
            ssb.append(text)
            val start = text.indexOf(gifter)
            if (start >= 0) {
                ssb.setSpan(ForegroundColorSpan(Color.parseColor("#18fbff")), start, start + gifter.length, 0)
            }
            val you = context.getString(R.string.notice_gifted_sub_you)
            val youStart = text.indexOf(you, if (start >= 0) start + gifter.length else 0)
            if (youStart >= 0) {
                ssb.setSpan(ForegroundColorSpan(Color.parseColor("#ff5218")), youStart, youStart + you.length, 0)
            }
        } catch (e: Exception) {
            ssb.append(gifter)
        }
        return ssb
    }

    private fun formatPlain(str: String): CharSequence {
        val ssb = SpannableStringBuilder(str)
        ssb.setSpan(ForegroundColorSpan(Color.WHITE), 0, ssb.length, 33)
        return ssb
    }
}
