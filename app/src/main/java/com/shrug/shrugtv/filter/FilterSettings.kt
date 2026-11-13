package com.shrug.shrugtv.filter

import android.content.Context
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.session.AppSettings
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import java.util.Locale

/**
 * Channel, category, and rerun filter settings backed by SharedPreferences and O(1)
 * case-insensitive HashSet lookups (replaces decompiled FilterSettings.java).
 */
object FilterSettings {

    private enum class FilterKind { CHANNEL, CATEGORY }

    @Volatile
    private var channelSet: Set<String> = emptySet()

    @Volatile
    private var categorySet: Set<String> = emptySet()

    @JvmStatic
    fun load(context: Context?) {
        if (context == null) return
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context) ?: return
            val channelsRaw = prefs.getString("pref_filtering_channels", "") ?: ""
            val categoriesRaw = prefs.getString("pref_filtering_categories", "") ?: ""
            AppState.filteringEnabled = prefs.getBoolean("pref_filtering_use_filtering", false)
            AppState.filterReruns = prefs.getBoolean("pref_filtering_reruns", false)

            val channels = parseList(channelsRaw)
            val categories = parseList(categoriesRaw)
            AppState.filterChannels = if (channels.isEmpty()) null else channels.toTypedArray()
            AppState.filterCategories = if (categories.isEmpty()) null else categories.toTypedArray()
            channelSet = channels.mapTo(HashSet(channels.size * 2)) { it.lowercase(Locale.ROOT) }
            categorySet = categories.mapTo(HashSet(categories.size * 2)) { it.lowercase(Locale.ROOT) }
            AppSettings.sync(context)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private fun parseList(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(";").filter { it.isNotEmpty() }

    @JvmStatic
    fun addCategory(context: Context, name: String) {
        modify(context, FilterKind.CATEGORY, name, add = true)
    }

    @JvmStatic
    fun addChannel(context: Context, name: String) {
        modify(context, FilterKind.CHANNEL, name, add = true)
    }

    @JvmStatic
    fun removeCategory(context: Context, name: String) {
        modify(context, FilterKind.CATEGORY, name, add = false)
    }

    @JvmStatic
    fun removeChannel(context: Context, name: String) {
        modify(context, FilterKind.CHANNEL, name, add = false)
    }

    @JvmStatic
    fun isCategoryFiltered(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val currentArray = AppState.filterCategories ?: return false
        if (categorySet.isNotEmpty()) {
            return categorySet.contains(name.lowercase(Locale.ROOT))
        }
        return currentArray.any { it.equals(name, ignoreCase = true) }
    }

    @JvmStatic
    fun isChannelFiltered(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val currentArray = AppState.filterChannels ?: return false
        if (channelSet.isNotEmpty()) {
            return channelSet.contains(name.lowercase(Locale.ROOT))
        }
        return currentArray.any { it.equals(name, ignoreCase = true) }
    }

    @JvmStatic
    fun isFilteredRerun(streamType: String?): Boolean =
        AppState.filterReruns && streamType == "rerun"

    @JvmStatic
    fun addIfNotFiltered(list: ArrayList<com.shrug.shrugtv.model.Movie>?, movie: com.shrug.shrugtv.model.Movie?) {
        if (list == null || movie == null) return
        try {
            if (!AppState.filteringEnabled) {
                list.add(movie)
                return
            }
            if (isChannelFiltered(movie.displayName) ||
                isCategoryFiltered(movie.gameName) ||
                isFilteredRerun(movie.streamType)
            ) {
                return
            }
            list.add(movie)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private fun modify(context: Context, kind: FilterKind, name: String, add: Boolean) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context) ?: return
            val key = when (kind) {
                FilterKind.CHANNEL -> "pref_filtering_channels"
                FilterKind.CATEGORY -> "pref_filtering_categories"
            }
            val current = parseList(prefs.getString(key, "") ?: "").toMutableList()
            if (add) {
                if (current.none { it.equals(name, ignoreCase = true) }) {
                    current.add(name)
                }
            } else {
                current.removeAll { it.equals(name, ignoreCase = true) }
            }
            prefs.edit().putString(key, current.joinToString(";")).apply()
            load(context)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }
}
