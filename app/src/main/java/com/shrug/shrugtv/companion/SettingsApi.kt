package com.shrug.shrugtv.companion

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ha.MqttBridge
import com.shrug.shrugtv.ha.MqttSettings
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.ui.tv.settings.Entry
import com.shrug.shrugtv.ui.tv.settings.SettingsActions
import com.shrug.shrugtv.ui.tv.settings.SettingsModel
import com.shrug.shrugtv.util.AppLanguage
import java.util.Locale

/**
 * All app settings in the browser (docs/companion_plan.md, section 8). Reads the same
 * res/xml/settings.xml as the TV settings screen, so the browser shows exactly the same groups,
 * texts, choices and dependencies, and writes the same SharedPreferences. Every value is checked
 * against its entry: switches take booleans, lists only their own values, text fields at most
 * [MAX_TEXT] characters; keys that are not on a visible page are refused.
 */
object SettingsApi {
    const val ALLOW_KEY = "pref_companion_settings"
    const val MAX_TEXT = 500
    private const val MAX_VALUES = 400

    /** Shown empty in the browser, write-only. */
    private val SECRET_KEYS = com.shrug.shrugtv.ui.tv.settings.SettingsModel.SECRET_KEYS

    /**
     * Left out of backups (export and import), like Glasshouse does: MQTT (device name and id must
     * differ per TV, otherwise two TVs clash in Home Assistant) and who may reach the TV (companion).
     */
    private val NO_BACKUP_PREFIXES = listOf("pref_mqtt_", "pref_companion_")

    internal fun inBackup(key: String) = NO_BACKUP_PREFIXES.none { key.startsWith(it) } && key !in SECRET_KEYS

    /** Buttons that also make sense from the browser; all other buttons only work on the TV. */
    val BROWSER_ACTIONS = setOf("pref_notification_test", "pref_live_channel_history_clear_history", "pref_mqtt_status",
        com.shrug.shrugtv.ui.tv.theme.ThemeSettings.ACTION_RESET)

    private val main = Handler(Looper.getMainLooper())

    fun allowed(context: Context): Boolean = prefs(context).getBoolean(ALLOW_KEY, true)

    private fun prefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /** Languages the app has texts for; the phone page asks in its own language. */
    internal val LANGUAGES = setOf("en", "de", "es", "fr", "pt", "it", "pl", "tr", "ru", "ja", "ko", "zh")

    /** Texts in the browser's language when the app has it, otherwise in the app language. */
    private fun localised(context: Context, lang: String?): Context {
        if (lang == null || lang !in LANGUAGES) return AppLanguage.wrap(context)
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return context.createConfigurationContext(config)
    }

    /** Groups as on the TV, but with the advanced settings always shown (a browser has room); the developer page only in developer mode. */
    internal fun visiblePages(root: Entry.Page, developer: Boolean): List<Entry.Page> =
        SettingsModel.visible(root, developer, advanced = true)

    /** Every changeable entry on the visible pages, by key. */
    internal fun editable(pages: List<Entry>): Map<String, Entry> {
        val out = LinkedHashMap<String, Entry>()
        fun walk(list: List<Entry>) {
            for (e in list) when (e) {
                is Entry.Page -> walk(e.children)
                is Entry.Category -> walk(e.children)
                is Entry.Toggle, is Entry.ListChoice, is Entry.TextField, is Entry.NumberField,
                is Entry.ColorChoice, is Entry.SkinChoice -> e.key?.let { out[it] = e }
                is Entry.Action, is Entry.Preview -> Unit
            }
        }
        walk(pages)
        return out
    }

    /** The value to store, or null if [v] does not fit the entry. */
    internal fun validate(e: Entry, v: JsonElement?): Any? {
        if (v == null || !v.isJsonPrimitive) return null
        val p = v.asJsonPrimitive
        return when (e) {
            is Entry.Toggle -> if (p.isBoolean) p.asBoolean else null
            is Entry.ListChoice -> if (p.isString && p.asString in e.values) p.asString else null
            is Entry.TextField -> if (p.isString && p.asString.length <= MAX_TEXT && p.asString.none { it < ' ' }) p.asString else null
            // A whole number inside the limits (the TV stores it as text, like all values of this kind)
            is Entry.NumberField -> {
                val n = if (p.isNumber) p.asDouble.takeIf { it == Math.floor(it) }?.toInt() else if (p.isString) p.asString.trim().toIntOrNull() else null
                if (n != null && n in e.min..e.max) n.toString() else null
            }
            // A colour as "#RRGGBB" or "skin"; a skin by its id
            is Entry.ColorChoice -> if (p.isString && (p.asString == com.shrug.shrugtv.ui.tv.theme.ThemeSettings.FROM_SKIN ||
                com.shrug.shrugtv.ui.tv.theme.ShrugTheme.parseHex(p.asString) != null)) p.asString else null
            is Entry.SkinChoice -> if (p.isString && (com.shrug.shrugtv.ui.tv.theme.Skins.all.any { it.id == p.asString } ||
                com.shrug.shrugtv.ui.tv.theme.CustomSkins.parse(prefsForValidation?.getString(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.PREF_CUSTOM, null)).any { it.id == p.asString })) p.asString else null
            else -> null
        }
    }

    // ------------------------------------------------------------------ reading

    /** Set while [tree] runs: names of the skins in the asked language. */
    private var localisedContext: Context? = null
    /** Set while [apply] runs: own skins are valid skin values too. */
    private var prefsForValidation: SharedPreferences? = null

    @Synchronized
    fun tree(context: Context, lang: String?): JsonObject {
        val standards = mutableMapOf<String, Any?>()
        val local = localised(context, lang)
        localisedContext = local
        val root = SettingsModel.load(local, R.xml.settings, standards)
        val p = prefs(context)
        val pages = visiblePages(root, p.getBoolean("pref_developer_mode", false))
        return JsonObject().apply {
            addProperty("version", BuildConfig.VERSION_NAME)
            add("pages", JsonArray().apply { pages.forEach { add(node(it, p, standards)) } })
            // Values of all switches, so dependencies on keys outside the visible pages work too
            add("standards", JsonObject().apply {
                standards.forEach { (k, v) -> if (v is Boolean) addProperty(k, p.safeBoolean(k, v)) }
            })
        }
    }

    private fun node(e: Entry, p: SharedPreferences, standards: Map<String, Any?>): JsonObject = JsonObject().apply {
        e.key?.let { addProperty("key", it) }
        addProperty("title", e.title)
        e.text?.takeIf { it.isNotBlank() && !it.contains("%s") }?.let { addProperty("text", it) }
        e.dependent?.let { addProperty("dep", it) }
        when (e) {
            is Entry.Page -> {
                addProperty("type", "page")
                add("children", JsonArray().apply { e.children.forEach { add(node(it, p, standards)) } })
            }
            is Entry.Category -> {
                addProperty("type", "category")
                // The browser shows a live connection card in the MQTT group
                if (e.children.any { it.key == "pref_mqtt_enabled" }) addProperty("panel", "mqtt")
                // … and the list of remembered devices in the companion group
                if (e.children.any { it.key == "pref_companion_enabled" }) addProperty("panel", "devices")
                // … and own skins, export and import below the design settings
                if (e.children.any { it.key == com.shrug.shrugtv.ui.tv.theme.ThemeSettings.ACTION_RESET }) addProperty("panel", "design")
                add("children", JsonArray().apply { e.children.forEach { add(node(it, p, standards)) } })
            }
            is Entry.Toggle -> {
                if (!inBackup(e.key)) addProperty("noBackup", true)
                addProperty("type", "toggle")
                addProperty("value", p.safeBoolean(e.key, e.standard))
                e.textOn?.takeIf { it.isNotBlank() }?.let { addProperty("textOn", it) }
                e.textOff?.takeIf { it.isNotBlank() }?.let { addProperty("textOff", it) }
            }
            is Entry.ListChoice -> {
                if (!inBackup(e.key)) addProperty("noBackup", true)
                addProperty("type", "list")
                addProperty("value", p.safeString(e.key, e.standard))
                add("options", JsonArray().apply {
                    e.values.forEachIndexed { i, v -> add(JsonArray().apply { add(v); add(e.names.getOrElse(i) { v }) }) }
                })
            }
            is Entry.TextField -> {
                if (!inBackup(e.key)) addProperty("noBackup", true)
                addProperty("type", "text")
                if (e.key in SECRET_KEYS) {
                    addProperty("secret", true)
                    addProperty("isSet", !p.safeString(e.key, "").isNullOrEmpty())
                } else {
                    addProperty("value", p.safeString(e.key, e.standard) ?: "")
                }
            }
            is Entry.NumberField -> {
                if (!inBackup(e.key)) addProperty("noBackup", true)
                addProperty("type", "number")
                addProperty("value", (p.safeString(e.key, e.standard.toString())?.toIntOrNull() ?: e.standard).coerceIn(e.min, e.max))
                addProperty("min", e.min)
                addProperty("max", e.max)
                addProperty("step", e.step)
            }
            // A colour: the browser shows a colour field with "from the skin"
            is Entry.ColorChoice -> {
                addProperty("type", "color")
                addProperty("value", p.safeString(e.key, e.standard) ?: e.standard)
                addProperty("effective", com.shrug.shrugtv.ui.tv.theme.ShrugTheme.toHex(
                    com.shrug.shrugtv.ui.tv.settings.colorOf(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.resolve(p::getString), e.key)))
            }
            // Skins as tiles: id, name, background, surface, accent, text, own skin
            is Entry.SkinChoice -> {
                addProperty("type", "skin")
                addProperty("value", p.safeString(e.key, com.shrug.shrugtv.ui.tv.theme.Skins.SHRUGTV.id))
                val custom = com.shrug.shrugtv.ui.tv.theme.CustomSkins.parse(p.safeString(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.PREF_CUSTOM, null))
                val all = com.shrug.shrugtv.ui.tv.theme.Skins.all.map { it to (localisedContext?.getString(com.shrug.shrugtv.ui.tv.settings.skinName(it.id)) ?: it.id) to false } +
                    custom.map { com.shrug.shrugtv.ui.tv.theme.CustomSkins.theme(it) to it.name to true }
                add("options", JsonArray().apply {
                    all.forEach { (pair, own) ->
                        val (t, name) = pair
                        add(JsonArray().apply {
                            add(t.id); add(name)
                            listOf(t.colors.background, t.colors.surface, t.colors.accent, t.colors.text)
                                .forEach { add(com.shrug.shrugtv.ui.tv.theme.ShrugTheme.toHex(it)) }
                            add(own)
                        })
                    }
                })
                addProperty("max", com.shrug.shrugtv.ui.tv.theme.CustomSkins.MAX)
            }
            is Entry.Preview -> addProperty("type", "preview")
            is Entry.Action -> {
                if (e.key == null && !e.selectable) addProperty("type", "tip") else addProperty("type", "action")
                if (e.key in BROWSER_ACTIONS) addProperty("run", true)
                info(e.key)?.let { addProperty("info", it) }
            }
        }
    }

    /** Runtime values the TV shows as summary (page "App info"). */
    private fun info(key: String?): String? = when (key) {
        "pref_version_name" -> BuildConfig.VERSION_NAME
        "pref_version_code" -> BuildConfig.VERSION_CODE.toString()
        "pref_user_id" -> AppState.hashedUserId
        "pref_buildType" -> BuildConfig.BUILD_TYPE
        "pref_mqtt_status" -> MqttBridge.status
        else -> null
    }

    private fun SharedPreferences.safeBoolean(k: String, d: Boolean): Boolean =
        try { getBoolean(k, d) } catch (e: ClassCastException) { all[k]?.toString()?.toBooleanStrictOrNull() ?: d }

    private fun SharedPreferences.safeString(k: String, d: String?): String? =
        try { getString(k, d) } catch (e: ClassCastException) { all[k]?.toString() ?: d }

    // ------------------------------------------------------------------ writing

    /**
     * Stores [values] (key -> value) in one go; used for single changes and for importing a saved
     * file ([fromBackup]: keys left out of backups are skipped). Returns how many were stored, which
     * keys were refused and which were skipped.
     */
    @Synchronized
    fun apply(context: Context, values: JsonObject, fromBackup: Boolean = false): JsonObject {
        val p = prefs(context)
        prefsForValidation = p
        val root = SettingsModel.load(context, R.xml.settings, mutableMapOf())
        val known = editable(visiblePages(root, p.getBoolean("pref_developer_mode", false)))
        val rejected = JsonArray()
        val skipped = JsonArray()
        val switched = ArrayList<Pair<String, Boolean>>()
        var stored = 0
        var languageChanged = false
        val edit = p.edit()
        for ((k, v) in values.entrySet().take(MAX_VALUES)) {
            if (fromBackup && !inBackup(k)) { skipped.add(k); continue }
            val e = known[k]
            val value = e?.let { validate(it, v) }
            if (value == null) { rejected.add(k); continue }
            if (k in SECRET_KEYS && value == MqttSettings.MASK) continue // "unchanged"
            when (value) {
                is Boolean -> {
                    if (p.safeBoolean(k, !value) != value) switched += k to value
                    edit.putBoolean(k, value)
                }
                is String -> {
                    if (k == AppLanguage.PREF_KEY && p.safeString(k, "system") != value) languageChanged = true
                    edit.putString(k, value)
                }
            }
            stored++
        }
        edit.apply()
        switched.forEach { (k, on) -> SettingsActions.afterSwitch(context, k, on) }
        // Same as on the TV: a new app language restarts the app on the home screen
        if (languageChanged) main.post { KeyInjector.foreground?.let { AppLanguage.apply(it) } }
        return JsonObject().apply {
            addProperty("ok", rejected.size() == 0)
            addProperty("stored", stored)
            add("rejected", rejected)
            add("skipped", skipped)
        }
    }

    /** Runs one of [BROWSER_ACTIONS]; the others need the TV in front of you. */
    fun action(context: Context, key: String): JsonObject = JsonObject().apply {
        when {
            key !in BROWSER_ACTIONS -> { addProperty("ok", false); addProperty("error", "tv_only") }
            key == "pref_mqtt_status" -> { addProperty("ok", true); addProperty("message", MqttBridge.status) }
            // Needs no TV in front: only preferences, the theme follows them
            key == com.shrug.shrugtv.ui.tv.theme.ThemeSettings.ACTION_RESET -> {
                com.shrug.shrugtv.ui.tv.theme.ThemeSettings.resetOverrides(prefs(context))
                addProperty("ok", true)
            }
            KeyInjector.foreground == null -> { addProperty("ok", false); addProperty("error", "no_screen") }
            else -> {
                main.post { KeyInjector.foreground?.let { SettingsActions.execute(it, key) } }
                addProperty("ok", true)
            }
        }
    }

    // ------------------------------------------------------------------ design

    /** Own skins from the browser: save the current design, delete one, pick one, import a file. */
    @Synchronized
    fun design(context: Context, body: JsonObject): JsonObject = JsonObject().apply {
        val p = prefs(context)
        val op = body.get("op")?.takeIf { it.isJsonPrimitive }?.asString
        val CS = com.shrug.shrugtv.ui.tv.theme.CustomSkins
        when (op) {
            "save" -> {
                val s = CS.saveCurrent(p, body.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: "")
                addProperty("ok", s != null)
                if (s == null) addProperty("error", if (CS.parse(p.getString(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.PREF_CUSTOM, null)).size >= CS.MAX) "full" else "name")
            }
            "delete" -> addProperty("ok", CS.delete(p, body.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: ""))
            "pick" -> {
                val id = body.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                val known = com.shrug.shrugtv.ui.tv.theme.Skins.all.any { it.id == id } ||
                    CS.parse(p.getString(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.PREF_CUSTOM, null)).any { it.id == id }
                if (known) com.shrug.shrugtv.ui.tv.theme.ThemeSettings.pickSkin(p, id)
                addProperty("ok", known)
            }
            "import" -> {
                val text = body.get("json")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                val full = CS.parse(p.getString(com.shrug.shrugtv.ui.tv.theme.ThemeSettings.PREF_CUSTOM, null)).size >= CS.MAX
                val s = if (full || text.length > 20_000) null else CS.import(p, text)
                addProperty("ok", s != null)
                if (s != null) addProperty("name", s.name) else addProperty("error", if (full) "full" else "format")
            }
            "export" -> {
                addProperty("ok", true)
                addProperty("json", CS.export(p::getString, body.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: "ShrugTV"))
            }
            else -> { addProperty("ok", false); addProperty("error", "op") }
        }
    }
}
