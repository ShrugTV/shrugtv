package com.shrug.shrugtv.ui.tv.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shrug.shrugtv.R
import com.shrug.shrugtv.activities.LoginActivity
import com.shrug.shrugtv.activities.PlayerActivity
import com.shrug.shrugtv.model.Enums
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.TvHomeTheme
import com.shrug.shrugtv.ui.tv.components.Destinations
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Home screen. Since step 7 the only one: the Leanback home screen (MainFragment)
 * is removed, MainActivity only forwards here. What it did globally on the side is now done by
 * GlobalSettings.load(). Player, channel page and search are the existing activities.
 */
class HomeActivity : ComponentActivity() {

    /** App language: resources follow the language chosen in the settings. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.shrug.shrugtv.util.AppLanguage.wrap(newBase))
    }

    private var state by mutableStateOf(HomeState())
    /** Card of the last 30 days at the start of a month; separate from [state] on purpose. */
    private var statsCard by mutableStateOf<com.shrug.shrugtv.stats.StatsCardData?>(null)
    /** Separate from [state], see there. */
    private var hero by mutableStateOf<Movie?>(null)
    private val heroClock = android.os.Handler(android.os.Looper.getMainLooper())
    private var loadCounter by mutableIntStateOf(0)
    private var lastNetworkLoadAt = 0L
    private var lastToken = ""
    private var forceNetworkReload = true
    private val eventReceiver = object : com.shrug.shrugtv.events.Events.Receiver {
        override fun onAppEvent(event: com.shrug.shrugtv.events.AppEvent) {
            forceNetworkReload = true
        }
        override fun liveHistoryRemoved(event: com.shrug.shrugtv.events.LiveHistoryChannelRemovedEvent) {
            forceNetworkReload = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.shrug.shrugtv.events.Events.register(eventReceiver)
        window.setFlags(1024, 1024) // full screen like the other activities
        // No window background: Compose paints the background colour full-screen anyway. Otherwise the
        // theme filled the whole screen once more every frame, expensive on the Fire TV.
        window.setBackgroundDrawable(null)
        val actions = HomeActions(
            watch = ::watch,
            toChannel = ::toChannel,
            multiview = ::multiview,
            open = ::open,
            selectHero = ::selectHero,
            scrolls = { listScrolls = it },
            search = { startActivity(Intent(this, com.shrug.shrugtv.ui.tv.search.SearchActivity::class.java)) },
            stats = { startActivity(Intent(this, com.shrug.shrugtv.ui.tv.stats.StatsActivity::class.java)) },
            settings = { startActivity(Intent(this, com.shrug.shrugtv.ui.tv.settings.SettingsActivity::class.java)) },
            profile = {
                val target = if (state.loginState is Login.LoggedIn) com.shrug.shrugtv.ui.tv.dialogs.LogoutActivity::class.java else LoginActivity::class.java
                startActivity(Intent(this, target))
            },
            contextMenu = { Destinations.contextMenu(this, it) },
        )
        // Splash on a cold start; the start window of the system shows the same logo before
        val splash = if (SplashRules.claim(savedInstanceState == null)) {
            SplashRules.read(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        } else null
        setContent {
            TvHomeTheme {
                LaunchedEffect(loadCounter) { load() }
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                    HomeScreen(state, { hero }, actions, { statsCard })
                    if (splash != null) {
                        StartSplash(
                            splash,
                            ready = { !state.loading || state.rows.isNotEmpty() },
                            name = { com.shrug.shrugtv.session.AppState.userDisplayName.ifBlank { com.shrug.shrugtv.session.AppState.userLogin } },
                        )
                    }
                }
            }
        }
        maybeShowStatsCard()
        resumeLastChannel(savedInstanceState == null)
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            Log.i("Start", "HomeActivity.onCreate ready after ${android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()} ms since process start")
        }
        window.decorView.post {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                Log.i("Start", "First frame after ${android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()} ms since process start")
            }
        }
    }

    /** Setting "Open the last channel on start": if that channel is live, go straight to the player. */
    private fun resumeLastChannel(fresh: Boolean) {
        val login = com.shrug.shrugtv.startup.StartResume.claim(this, fresh, intent?.action) ?: return
        val app = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val stream = try { com.shrug.shrugtv.startup.StartResume.liveStream(app, login) } catch (e: Exception) { null }
            Log.i("Start", "Last channel $login: " + if (stream != null) "live, opening the player" else "offline, staying on the home screen")
            if (stream != null) kotlinx.coroutines.withContext(Dispatchers.Main) { if (!isFinishing) watch(stream) }
        }
    }

    /**
     * Only change the hero once focus stays on a card for [HERO_IDLE_MS]: when
     * scrolling with a held key, every card passed over reloaded hero image and texts, in the middle of
     * the scroll animation.
     */
    private fun selectHero(m: Movie) {
        heroSelected = true
        heroClock.removeCallbacksAndMessages(null)
        heroClock.postDelayed({ applyHero(m) }, HERO_IDLE_MS)
    }

    /** If the row list is still scrolling, wait briefly: the hero change redraws texts
     *  and image (Fire TV 5-17 ms) and dropped frames in the middle of the scroll animation. */
    private fun applyHero(m: Movie) {
        if (listScrolls) heroClock.postDelayed({ applyHero(m) }, 60L) else hero = m
    }

    private var listScrolls = false

    /** In the first days of a month: one card with the last 30 days, then not again that month. */
    private fun maybeShowStatsCard() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean("pref_watch_stats", true) || !prefs.getBoolean("pref_watch_stats_card", true)) return
        val today = java.time.LocalDate.now()
        if (!com.shrug.shrugtv.stats.StatsCardGate.due(today, prefs.getString("watch_stats_card_month", null))) return
        com.shrug.shrugtv.util.Threads.doInBackground {
            try {
                val range = com.shrug.shrugtv.stats.StatsRange.DAYS_30
                val (rows, oldest) = com.shrug.shrugtv.stats.WatchStatsStore.load(applicationContext, range, today)
                val card = com.shrug.shrugtv.stats.StatsCardGate.cardFor(com.shrug.shrugtv.stats.WatchStats.compute(rows, range, today, oldest))
                // Checked for this month, also when there was nothing to say: no new try at every start
                prefs.edit().putString("watch_stats_card_month", com.shrug.shrugtv.stats.StatsCardGate.monthKey(today)).apply()
                if (card != null) runOnUiThread {
                    statsCard = card
                    heroClock.postDelayed({ statsCard = null }, com.shrug.shrugtv.stats.StatsCardGate.SHOW_MS)
                }
            } catch (e: Exception) {
                com.shrug.shrugtv.telemetry.CrashReporter.recordException(e)
            }
        }
    }

    override fun onDestroy() {
        com.shrug.shrugtv.events.Events.unregister(eventReceiver)
        heroClock.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        loadCounter++ // reload after login, logout or returning from the player
    }

    override fun onStart() {
        super.onStart()
        state = state.copy(active = true)
    }

    override fun onStop() {
        state = state.copy(active = false) // pause the live preview while the player runs
        super.onStop()
    }

    /**
     * One row of the home screen: stable id (key for cache and merging, independent of the app language),
     * title resource, switch from the settings, load function. The title is resolved only when a row is
     * built, with the activity's context, so it follows the app language.
     */
    private class RowKind(
        val id: String,
        @androidx.annotation.StringRes val title: Int,
        val on: () -> Boolean,
        val load: suspend (android.content.Context) -> List<Movie>,
    )

    // Order as on the old home screen: live and history first, discovery last.
    // Switches from settings → "Show/hide rows on Main screen" (AppState, GlobalSettings)
    private val builtInRows = listOf(
        RowKind("followed_live", R.string.home_row_followed_live, { AppState.showFollowed }, HomeData::followedLive),
        RowKind("live_history", R.string.home_row_live_history, { AppState.showLiveStreamHistory }, HomeData::liveHistory),
        RowKind("continue_watching", R.string.home_row_continue_watching, { AppState.showVodHistory }, HomeData::continueWatching),
        RowKind("followed_channels", R.string.home_row_followed_channels, { AppState.showFollowedChannels }, HomeData::followedChannels),
        RowKind("followed_categories", R.string.home_row_followed_categories, { AppState.showFollowedGames }, HomeData::followedGames),
        RowKind("past_broadcasts", R.string.home_row_past_broadcasts, { AppState.showPastBroadcasts }, HomeData::pastBroadcasts),
        RowKind("top_categories", R.string.home_row_top_categories, { AppState.showTopGames }, HomeData::topGames),
        RowKind("top_streams", R.string.home_row_top_streams, { AppState.showTopStreamers }, HomeData::topStreams),
    )

    /** The rows in the order of the settings page "Row order". */
    private val rowKinds: List<RowKind> get() {
        val saved = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getString(HomeRowOrder.PREF, null)
        return HomeRowOrder.order(builtInRows.map { it.id }, saved).mapNotNull { id -> builtInRows.firstOrNull { it.id == id } }
    }

    private fun buildRow(kind: RowKind, entries: List<Movie>) = HomeRow(kind.id, getString(kind.title), entries)

    /** If the user picked the hero (focus on a card), it stays when rows reload. */
    private var heroSelected = false

    private fun firstStream(rows: List<HomeRow>) =
        rows.flatMap { it.entries }.firstOrNull { it.itemType == Enums.ContentType.STREAM }

    /**
     * Loads the home screen: first the one saved last time, then each fresh row as
     * soon as it arrives. The profile loads in parallel; the user id comes from the login. Before,
     * everything waited for the profile first (0.3-0.45 s) and then for the slowest row.
     */
    private suspend fun load() {
        val ctx = applicationContext
        val tStart = android.os.SystemClock.elapsedRealtime()
        fun since() = android.os.SystemClock.elapsedRealtime() - tStart
        com.shrug.shrugtv.session.GlobalSettings.load(ctx)
        val prefs = ctx.getSharedPreferences("User", 0)
        if (AppState.userId.isNullOrEmpty()) {
            AppState.userId = prefs.getString("USER_ID", "") ?: ""
            if (AppState.userLogin.isNullOrEmpty()) AppState.userLogin = prefs.getString("USER_NAME", "") ?: ""
        }
        val user = AppState.userId ?: ""
        var probablyLoggedIn = !AppState.accessToken.isNullOrEmpty() && user.isNotEmpty()

        // 1. Show the saved home screen right away
        var old: Map<String, List<Movie>> = emptyMap()
        if (probablyLoggedIn) {
            val saved = withContext(Dispatchers.IO) { HomeCache.load(ctx, user) }
            old = saved.associate { it.id to it.entries }
            if (state.rows.isEmpty() && saved.isNotEmpty()) {
                state = state.copy(rows = visible(old, emptyMap()), loading = true)
                if (hero == null) hero = firstStream(saved)
                Log.i(TAG, "Visible (saved) after ${since()} ms")
            }
        }
        if (state.rows.isNotEmpty()) old = state.rows.associate { it.id to it.entries } + old.filterKeys { k -> state.rows.none { it.id == k } }

        val currentToken = AppState.accessToken ?: ""
        val now = android.os.SystemClock.elapsedRealtime()
        val hasAllEnabledNetworkRows = rowKinds.filter { it.on() }.all {
            it.id == "live_history" || it.id == "continue_watching" || it.id in old
        }
        if (!forceNetworkReload &&
            probablyLoggedIn &&
            state.loginState is Login.LoggedIn &&
            currentToken == lastToken &&
            lastNetworkLoadAt > 0L &&
            now - lastNetworkLoadAt < NETWORK_COOLDOWN_MS &&
            hasAllEnabledNetworkRows
        ) {
            // Within the 60 s cooldown after returning from Player/Channel/Search: refresh only the
            // local Room history rows immediately instead of firing 10-15 network calls again.
            val localRows = mutableMapOf<String, List<Movie>>()
            coroutineScope {
                for (kind in rowKinds) {
                    if (kind.on() && (kind.id == "live_history" || kind.id == "continue_watching")) {
                        launch { localRows[kind.id] = kind.load(ctx) }
                    }
                }
            }
            val rows = visible(old, localRows)
            state = state.copy(rows = rows, loading = false)
            if (!heroSelected) hero = firstStream(rows) ?: hero
            return
        }

        state = state.copy(loading = true)

        // 2. Profile and rows in parallel; each row appears as soon as it arrives
        val fresh = mutableMapOf<String, List<Movie>>()
        var firstFresh = true
        coroutineScope {
            val profile = async { HomeData.loadProfile(ctx) }
            fun startRows() = rowKinds.map { kind ->
                launch {
                    fresh[kind.id] = kind.load(ctx)
                    Log.i(TAG, "Row ${kind.id} after ${since()} ms")
                    val rows = visible(old, fresh)
                    if (!heroSelected) hero = firstStream(visible(emptyMap(), fresh)) ?: hero ?: firstStream(rows)
                    state = state.copy(rows = rows)
                    if (firstFresh && rows.isNotEmpty()) {
                        firstFresh = false
                        Log.i(TAG, "Visible (fresh) after ${since()} ms")
                    }
                }
            }
            var jobs = if (probablyLoggedIn) startRows() else emptyList()
            val loginState = profile.await()
            Log.i(TAG, "Profile after ${since()} ms")
            if (loginState !is Login.LoggedIn) {
                Log.i(TAG, "Not signed in: $loginState")
                jobs.forEach { it.cancel() }
                if (loginState is Login.LoggedOut) HomeCache.delete(ctx)
                state = HomeState(loginState = loginState, loading = false, active = state.active)
                hero = null
                return@coroutineScope
            }
            state = state.copy(loginState = loginState)
            // Remember for the next start so the rows do not wait for the profile; before, nobody wrote
            // USER_ID (only sign-out clears it)
            if (!AppState.userId.isNullOrEmpty()) {
                prefs.edit().putString("USER_ID", AppState.userId).putString("USER_NAME", AppState.userLogin ?: "").apply()
            }
            if (!probablyLoggedIn) { // user id only came with the profile
                probablyLoggedIn = true
                jobs = startRows()
            }
            jobs.joinAll()
            val rows = visible(emptyMap(), fresh)
            Log.i(TAG, "Rows: " + rows.joinToString { "${it.id}=${it.entries.size}" })
            state = state.copy(rows = rows, loading = false)
            if (!heroSelected) hero = firstStream(rows) ?: hero
            lastNetworkLoadAt = android.os.SystemClock.elapsedRealtime()
            lastToken = AppState.accessToken ?: ""
            forceNetworkReload = false
            val all = rowKinds.mapNotNull { kind -> fresh[kind.id]?.takeIf { it.isNotEmpty() }?.let { buildRow(kind, it) } }
            withContext(Dispatchers.IO) { HomeCache.save(ctx, AppState.userId ?: user, all) }
        }
    }

    /** Visible rows in fixed order: fresh where available, otherwise the saved one. Maps keyed by row id. */
    private fun visible(old: Map<String, List<Movie>>, fresh: Map<String, List<Movie>>): List<HomeRow> =
        rowKinds.filter { it.on() }.mapNotNull { kind ->
            (fresh[kind.id] ?: old[kind.id])?.takeIf { it.isNotEmpty() }?.let { buildRow(kind, it) }
        }

    private fun watch(m: Movie) {
        // Hand the running hero preview over to the player instead of reloading
        com.shrug.shrugtv.player.HeroHandover.prepare(m)
        Destinations.watch(this, m)
    }

    /** Selected stream plus the next followed live stream side by side. */
    private fun multiview(m: Movie) {
        val partner = state.rows.flatMap { it.entries }
            .firstOrNull { it.itemType == Enums.ContentType.STREAM && it.login != m.login }
        Destinations.watch(this, m, multiview = partner)
    }

    private fun toChannel(m: Movie) = Destinations.channel(this, m)

    private fun open(m: Movie) = Destinations.open(this, m)

    companion object {
        private const val TAG = "HomeActivity"
        private const val HERO_IDLE_MS = 250L
        private const val NETWORK_COOLDOWN_MS = 60_000L
    }
}
