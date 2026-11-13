package com.shrug.shrugtv.ui.tv.settings

import android.content.Context
import android.content.res.XmlResourceParser
import com.shrug.shrugtv.BuildConfig
import org.xmlpull.v1.XmlPullParser

/**
 * The settings as a tree, read from the same res/xml/settings.xml as the old Leanback settings
 * (step 5). Keys, defaults, entries and dependencies therefore stay identical;
 * only the presentation is new. (minSdk 26: the one file res/xml/settings.xml; the old xml-v22 copy was dropped).
 * Titles and summaries are resources, so they follow the app language.
 */
sealed class Entry {
    abstract val key: String?
    abstract val title: String
    abstract val text: String?
    abstract val dependent: String?

    data class Page(override val key: String?, override val title: String, override val text: String?, val icon: Int,
                     val children: List<Entry>, override val dependent: String? = null) : Entry()
    data class Category(override val title: String, val children: List<Entry>) : Entry() {
        override val key: String? = null; override val text: String? = null; override val dependent: String? = null
    }
    data class Toggle(override val key: String, override val title: String, override val text: String?,
                        val standard: Boolean, val textOn: String?, val textOff: String?, override val dependent: String?) : Entry()
    data class ListChoice(override val key: String, override val title: String, override val text: String?,
                     val names: List<String>, val values: List<String>, val standard: String?, override val dependent: String?) : Entry()
    data class TextField(override val key: String, override val title: String, override val text: String?,
                        val standard: String?, val dialogTitle: String?, override val dependent: String?) : Entry()
    /** A whole number between [min] and [max]: typed, or Left/Right in steps of [step]. */
    data class NumberField(override val key: String, override val title: String, override val text: String?,
                        val standard: Int, val min: Int, val max: Int, val step: Int, override val dependent: String?) : Entry()
    data class Action(override val key: String?, override val title: String, override val text: String?,
                      val selectable: Boolean, override val dependent: String?) : Entry()
    /** A colour of the theme: "#RRGGBB" or "skin". */
    data class ColorChoice(override val key: String, override val title: String, override val text: String?,
                      val standard: String, override val dependent: String?) : Entry()
    /** The skin, as tiles. */
    data class SkinChoice(override val key: String, override val title: String, override val text: String?,
                      override val dependent: String?) : Entry()
    /** Live preview of the theme in effect. */
    data class Preview(override val title: String) : Entry() {
        override val key: String? = null; override val text: String? = null; override val dependent: String? = null
    }
}

object SettingsModel {
    /**
     * Rarely changed settings (leaves or pages), hidden until "Show advanced settings" is on.
     * Hiding changes nothing about the value; the companion page always shows everything.
     */
    val ADVANCED_KEYS = setOf(
        "pref_splash_min_ms", "pref_theme_focus_scale", "pref_theme_border", "pref_theme_hero_dim",
        "pref_chat_reply_chars", "pref_chat_merge_repeats", "pref_chat_zero_width", "pref_chat_7tv_paints",
        "pref_chat_7tv_badges", "pref_chat_7tv_personal", "pref_filter_predit_bots",
        "pref_player_replay_seconds", "pref_player_rest_after", "pref_player_self_heal", "pref_clock_date_format",
        "prefs_autorefresh_minutes", "pref_stream_preview_quality", "pref_mqtt_advanced", "pref_mqtt_interval",
        "pref_companion_log", "prefs_notification_font_size", "pref_notification_animation",
    )

    private fun isGuide(e: Entry) = e is Entry.Action && e.key == null && !e.selectable

    /**
     * The groups to show: the developer page only in developer mode, [ADVANCED_KEYS] only when [advanced], the group
     * "Advanced" when [advanced] or developer mode, and no empty categories or pages are left behind.
     */
    fun visible(root: Entry.Page, developer: Boolean, advanced: Boolean): List<Entry.Page> {
        fun keep(e: Entry): Entry? = when (e) {
            is Entry.Page -> when {
                e.key == "pref_developer" && !developer -> null
                e.key == "pref_advanced" && !(advanced || developer) -> null
                e.key in ADVANCED_KEYS && !advanced -> null
                else -> {
                    val kids = e.children.mapNotNull(::keep)
                    // A page that only has a preview or guides is empty
                    e.copy(children = kids).takeIf { kids.any { !isGuide(it) && it !is Entry.Preview } }
                }
            }
            is Entry.Category -> e.copy(children = e.children.mapNotNull(::keep)).takeIf { c -> c.children.any { !isGuide(it) } }
            else -> if (e.key != null && e.key in ADVANCED_KEYS && !advanced) null else e
        }
        return root.children.mapNotNull(::keep).filterIsInstance<Entry.Page>()
    }

    /** Text fields that hold a secret: the list shows dots, the dialog masks the input, the browser page never gets the value. */
    val SECRET_KEYS = setOf("pref_mqtt_password")

    private const val NS = "http://schemas.android.com/apk/res/android"

    /** Reads the root page; the defaults of all switches end up in [standards] (for dependencies). */
    /** Pages the public edition leaves out: the HLS proxy only exists to avoid ads. */
    private val hiddenPages: Set<String?> = if (BuildConfig.AD_FREE) emptySet() else setOf("pref_ProxyServer")

    /** Entries the private edition leaves out: it is rolled out by tools/deploy.sh and never asks for updates. */
    private val hiddenKeys: Set<String> = if (BuildConfig.UPDATE_CHECK) emptySet() else setOf("pref_update_check", "pref_update_check_now")

    fun load(context: Context, xmlRes: Int, standards: MutableMap<String, Any?>): Entry.Page {
        val p = context.resources.getXml(xmlRes)
        val stack = ArrayDeque<MutableList<Entry>>()
        val heads = ArrayDeque<(List<Entry>) -> Entry?>()
        stack.addLast(mutableListOf())
        heads.addLast { null }
        var root: Entry.Page? = null
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = p.name.substringAfterLast('.')
                    val key = p.getAttributeValue(NS, "key")
                    val title = text(context, p, "title") ?: ""
                    val summary = text(context, p, "summary")
                    val dep = p.getAttributeValue(NS, "dependency")
                    when (tag) {
                        "PreferenceScreen" -> {
                            val icon = p.getAttributeResourceValue(NS, "icon", 0)
                            stack.addLast(mutableListOf())
                            heads.addLast { children -> if (key in hiddenPages) null else Entry.Page(key, title, summary, icon, children, dep) }
                        }
                        "PreferenceCategory" -> {
                            stack.addLast(mutableListOf())
                            heads.addLast { children -> Entry.Category(title, children) }
                        }
                        else -> {
                            val e = if (key in hiddenKeys) null else leaf(context, p, tag, key, title, summary, dep)
                            if (e != null) {
                                stack.last().add(e)
                                if (key != null) standards[key] = defaultValue(e)
                            }
                            stack.addLast(mutableListOf()) // leaves have no children; END_TAG cleans up
                            heads.addLast { null }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val children = stack.removeLast()
                    val e = heads.removeLast()(children)
                    if (e != null) {
                        if (stack.size == 1 && e is Entry.Page) root = e else stack.last().add(e)
                    }
                }
            }
        }
        p.close()
        return root ?: Entry.Page(null, "", null, 0, stack.first())
    }

    private fun defaultValue(e: Entry): Any? = when (e) {
        is Entry.Toggle -> e.standard
        is Entry.ListChoice -> e.standard
        is Entry.TextField -> e.standard
        is Entry.NumberField -> e.standard.toString()
        is Entry.ColorChoice -> e.standard
        is Entry.SkinChoice -> com.shrug.shrugtv.ui.tv.theme.Skins.SHRUGTV.id
        else -> null
    }

    private fun leaf(c: Context, p: XmlResourceParser, tag: String, key: String?, title: String, summary: String?, dep: String?): Entry? {
        val standard = rawValue(c, p, "defaultValue")
        return when {
            key != null && (tag.endsWith("SwitchPreference") || tag == "CheckBoxPreference") ->
                Entry.Toggle(key, title, summary, standard?.toBooleanStrictOrNull() ?: false,
                    text(c, p, "summaryOn"), text(c, p, "summaryOff"), dep)
            key != null && tag == "ListPreference" -> {
                val names = field(c, p, "entries")
                val values = field(c, p, "entryValues")
                Entry.ListChoice(key, title, summary, names, values, standard, dep)
            }
            key != null && tag.endsWith("EditTextPreference") ->
                Entry.TextField(key, title, summary, standard, text(c, p, "dialogTitle"), dep)
            key != null && tag.endsWith("SeekBarPreference") -> {
                val min = p.getAttributeIntValue(NS, "min", 0)
                val max = p.getAttributeIntValue(NS, "max", 100)
                val step = p.getAttributeValue(NS, "stepSize")?.toFloatOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
                Entry.NumberField(key, title, summary, (standard?.toIntOrNull() ?: min).coerceIn(min, max), min, max, step, dep)
            }
            key != null && tag == "ColorPreference" -> Entry.ColorChoice(key, title, summary, standard ?: "skin", dep)
            key != null && tag == "SkinPreference" -> Entry.SkinChoice(key, title, summary, dep)
            tag == "DesignPreviewPreference" -> Entry.Preview(title)
            tag == "Preference" -> Entry.Action(key, title, summary, p.getAttributeBooleanValue(NS, "selectable", true), dep)
            else -> null
        }
    }

    private fun text(c: Context, p: XmlResourceParser, name: String): String? {
        val id = p.getAttributeResourceValue(NS, name, 0)
        if (id != 0) return try { c.getString(id) } catch (e: Exception) { null }
        return p.getAttributeValue(NS, name)
    }

    /** Default value as text, even if it points to a resource (bool/integer/string). */
    private fun rawValue(c: Context, p: XmlResourceParser, name: String): String? {
        val id = p.getAttributeResourceValue(NS, name, 0)
        if (id != 0) {
            return try {
                when (c.resources.getResourceTypeName(id)) {
                    "bool" -> c.resources.getBoolean(id).toString()
                    "integer" -> c.resources.getInteger(id).toString()
                    else -> c.getString(id)
                }
            } catch (e: Exception) { null }
        }
        return p.getAttributeValue(NS, name)
    }

    private fun field(c: Context, p: XmlResourceParser, name: String): List<String> {
        val id = p.getAttributeResourceValue(NS, name, 0)
        return if (id != 0) c.resources.getStringArray(id).toList() else emptyList()
    }
}
