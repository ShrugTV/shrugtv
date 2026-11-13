package com.shrug.shrugtv.session

import android.content.Context
import com.shrug.shrugtv.model.ChatSizePreset
import com.shrug.shrugtv.remotesettings.RemoteSettings
import java.time.ZonedDateTime

/**
 * Process-wide application state and static lookup tables (replaces decompiled AppState.java,
 * ). Backed by [AppSettings] and [SessionStore].
 */
object AppState {
    @JvmField
    var storedScopes: List<String> = emptyList()
    fun loadScopes(context: Context) { storedScopes = context.getSharedPreferences("User", 0).getStringSet("SCOPES", emptySet())?.toList() ?: emptyList() }

    @JvmField var showDevErrors: Boolean = false
    @JvmField var previewImageWidth: Int = 800
    @JvmField var previewImageHeight: Int = 450
    @JvmField var boxArtWidth: Int = 245
    @JvmField var boxArtHeight: Int = 342
    @JvmField var returnedFromBackground: Boolean = false
    @JvmField var appStartTime: ZonedDateTime? = null
    @JvmField var favoritesInFrontOfFollowed: Boolean = false
    @JvmField var languageFlags: HashMap<String, Array<Int>>? = null
    @JvmField var filterChannels: Array<String>? = null
    @JvmField var filterCategories: Array<String>? = null
    @JvmField var streamPreviewResolution: IntArray? = null

    // Written by the token refresh thread and the profile callback, read by OkHttp, chat and UI
    // threads: @Volatile so a refreshed token is seen everywhere at once (docs/bug_review.md B21)
    @Volatile @JvmField var accessToken: String = ""
    @Volatile @JvmField var userId: String = ""
    @Volatile @JvmField var userLogin: String = ""
    @Volatile @JvmField var userDisplayName: String = ""
    @JvmField var hashedUserId: String = ""
    @Volatile @JvmField var userProfileLoaded: Boolean = false
    @JvmField var userLoggedOut: Boolean = false
    @JvmField var userProfileImageUrl: String = ""
    @JvmField var isTurbo: Boolean = false
    @JvmField var loginVersionName: String? = null
    @JvmField var loginVersionCode: Int = 0

    @JvmField var streamLanguageName: String = ""
    @JvmField var streamLanguageCode: String = ""
    @JvmField var showLanguageRow: Boolean = false
    @JvmField var showLanguageRowGames: Boolean = false
    @JvmField var languageRowAsPrimary: Boolean = false
    @JvmField var showTopGames: Boolean = true
    @JvmField var showTopStreamers: Boolean = true
    @JvmField var showFollowed: Boolean = true
    @JvmField var showLiveStreamHistory: Boolean = true
    @JvmField var showFollowedGames: Boolean = true
    @JvmField var showFollowedChannels: Boolean = true
    @JvmField var showPastBroadcasts: Boolean = true
    @JvmField var showVodHistory: Boolean = false
    @JvmField var autoRefreshEnabled: Boolean = false
    @JvmField var autoRefreshMinutes: Int = 0
    @JvmField var favoriteChannelIds: MutableList<String> = ArrayList()
    @JvmField var remoteSettings: RemoteSettings? = null
    @JvmField var filteringEnabled: Boolean = false
    @JvmField var filterReruns: Boolean = false
    @JvmField var titlePrefixCodePoint: Int = 11088

    @JvmField var debugServerAds: String = ""
    @JvmField var debugShowAds: String = ""
    @JvmField var debugPlatform: String = ""
    @JvmField var debugPlayerType: String = ""
    @JvmField var debugParam: String = ""

    @JvmStatic
    fun chatSizePresets(): HashMap<String, ChatSizePreset> = hashMapOf(
        "12" to ChatSizePreset(22.0f, 64, 64, 353),
        "11" to ChatSizePreset(21.0f, 61, 61, 337),
        "10" to ChatSizePreset(20.0f, 58, 58, 321),
        "9" to ChatSizePreset(19.0f, 55, 55, 305),
        "8" to ChatSizePreset(18.0f, 52, 52, 289),
        "7" to ChatSizePreset(17.0f, 49, 49, 273),
        "0" to ChatSizePreset(16.0f, 46, 46, 257),
        "1" to ChatSizePreset(15.0f, 43, 43, 241),
        "2" to ChatSizePreset(14.0f, 40, 40, 225),
        "3" to ChatSizePreset(13.0f, 37, 37, 209),
        "4" to ChatSizePreset(12.0f, 34, 34, 193),
        "5" to ChatSizePreset(11.0f, 31, 31, 177),
        "6" to ChatSizePreset(10.0f, 28, 28, 161),
    )

    @JvmStatic
    fun isReloginRequired(): Boolean =
        (loginVersionName != "1.5.10" && 142 > loginVersionCode) ||
        (accessToken.isNotEmpty() && !storedScopes.contains("user:read:chat")) // EventSub chat #135

    @JvmStatic
    fun loadAccessToken(context: Context) {
        val prefs = context.getSharedPreferences("User", 0)
        if (prefs != null) {
            accessToken = prefs.getString("TOKEN", "") ?: ""
        }
    }

    @JvmStatic
    fun buildLanguageFlags(): HashMap<String, Array<Int>> = hashMapOf(
        "en" to arrayOf(127468, 127463),
        "da" to arrayOf(127465, 127472),
        "de" to arrayOf(127465, 127466),
        "es" to arrayOf(127466, 127480),
        "fr" to arrayOf(127467, 127479),
        "it" to arrayOf(127470, 127481),
        "hu" to arrayOf(127469, 127482),
        "nl" to arrayOf(127475, 127473),
        "no" to arrayOf(127475, 127476),
        "pl" to arrayOf(127477, 127473),
        "pt" to arrayOf(127477, 127481),
        "ro" to arrayOf(127479, 127476),
        "sk" to arrayOf(127480, 127472),
        "fi" to arrayOf(127467, 127470),
        "sv" to arrayOf(127480, 127466),
        "vi" to arrayOf(127483, 127475),
        "tr" to arrayOf(127481, 127479),
        "cs" to arrayOf(127464, 127487),
        "el" to arrayOf(127468, 127479),
        "bg" to arrayOf(127463, 127468),
        "ru" to arrayOf(127479, 127482),
        "ar" to arrayOf(127480, 127462),
        "th" to arrayOf(127481, 127469),
        "zh" to arrayOf(127464, 127475),
        "zh-hk" to arrayOf(127469, 127472),
        "ja" to arrayOf(127471, 127477),
        "ko" to arrayOf(127472, 127479),
        "hi" to arrayOf(127470, 127475),
        "asl" to emptyArray(),
        "other" to emptyArray(),
    )

    @JvmStatic
    fun buildLanguageNames(): HashMap<String, String> = hashMapOf(
        "en" to "English",
        "da" to "Dansk",
        "de" to "Deutsch",
        "es" to "Español",
        "fr" to "Français",
        "it" to "Italiano",
        "hu" to "Magyar",
        "nl" to "Netherlands",
        "no" to "Norsk",
        "pl" to "Polski",
        "pt" to "Português",
        "ro" to "Română",
        "sk" to "Slovenčina",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "vi" to "Tiếng Việt",
        "tr" to "Türkçe",
        "cs" to "Čeština",
        "el" to "Ελληνικά",
        "bg" to "Български",
        "ru" to "Русский",
        "ar" to "العربية",
        "th" to "ภาษาไทย",
        "zh" to "中文",
        "zh-hk" to "中文(粵語)",
        "ja" to "日本語",
        "ko" to "한국어",
        "asl" to "American Sign Language",
        "other" to "Other",
    )
}
