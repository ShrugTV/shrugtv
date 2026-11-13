package com.shrug.shrugtv.ui.tv.theme

import android.content.SharedPreferences
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Own skins, export and import. An own skin is a ready-made skin as base plus the values of the
 * "Appearance" page (`pref_theme_*`) that differ from it; at most [MAX] of them live as JSON in [ThemeSettings.PREF_CUSTOM].
 * The pure functions take a reader like `SharedPreferences.getString`, so they run in unit tests.
 */
data class CustomSkin(val id: String, val name: String, val base: String, val values: Map<String, String>)

object CustomSkins {
    const val MAX = 10
    const val PREFIX = "custom_"
    const val FORMAT = "shrugtv_theme"
    private const val MAX_NAME = 40

    fun isCustom(id: String?) = id?.startsWith(PREFIX) == true

    // ------------------------------------------------------------------ values

    /** True if [value] is something `ThemeSettings.resolve` accepts for [key] ("skin" counts as nothing). */
    fun valid(key: String, value: String): Boolean {
        if (key !in ThemeSettings.OVERRIDES || value == ThemeSettings.FROM_SKIN) return false
        val n = value.toFloatOrNull()
        return when (key) {
            ThemeSettings.PREF_ACCENT, ThemeSettings.PREF_BACKGROUND, ThemeSettings.PREF_SURFACE, ThemeSettings.PREF_TEXT,
            ThemeSettings.PREF_TEXT_MUTED, ThemeSettings.PREF_LIVE, ThemeSettings.PREF_FOCUS -> ShrugTheme.parseHex(value) != null
            ThemeSettings.PREF_CORNERS -> n != null && n in 0f..3f
            ThemeSettings.PREF_FOCUS_SCALE -> n != null && n in 1f..1.2f
            ThemeSettings.PREF_BORDER -> n != null && n in 0f..6f
            ThemeSettings.PREF_GLASS -> n != null && n in 0f..2f
            ThemeSettings.PREF_HERO_DIM -> n != null && n in 0.4f..1.2f
            ThemeSettings.PREF_TEXT_SCALE -> n != null && n in 0.8f..1.4f
            ThemeSettings.PREF_FOCUS_STYLE -> FocusStyle.entries.any { it.name.equals(value, true) }
            ThemeSettings.PREF_BACKGROUND_STYLE -> BackgroundStyle.entries.any { it.name.equals(value, true) }
            ThemeSettings.PREF_FONT -> FontChoice.entries.any { it.name.equals(value, true) }
            ThemeSettings.PREF_ANIMATIONS -> value == "on" || value == "off"
            else -> false
        }
    }

    /** The own values of a reader: only valid ones, "skin" and empty left out. */
    fun ownValues(read: (String, String?) -> String?): Map<String, String> =
        ThemeSettings.OVERRIDES.mapNotNull { k -> read(k, null)?.takeIf { valid(k, it) }?.let { k to it } }.toMap()

    // ------------------------------------------------------------------ list

    fun parse(json: String?): List<CustomSkin> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            JsonParser.parseString(json).asJsonArray.mapNotNull { fromJson(it.asJsonObject) }.take(MAX)
        } catch (e: Exception) { emptyList() }
    }

    fun toJson(list: List<CustomSkin>): String = JsonArray().apply { list.forEach { add(toJsonObject(it)) } }.toString()

    private fun toJsonObject(s: CustomSkin) = JsonObject().apply {
        addProperty("id", s.id)
        addProperty("name", s.name)
        addProperty("base", s.base)
        add("values", JsonObject().apply { s.values.forEach { (k, v) -> addProperty(k, v) } })
    }

    private fun fromJson(o: JsonObject): CustomSkin? {
        val id = o.get("id")?.asString?.takeIf { isCustom(it) } ?: return null
        val name = cleanName(o.get("name")?.asString) ?: return null
        val base = o.get("base")?.asString?.takeIf { b -> Skins.all.any { it.id == b } } ?: Skins.SHRUGTV.id
        val values = o.getAsJsonObject("values")?.entrySet()
            ?.mapNotNull { (k, v) -> v.takeIf { it.isJsonPrimitive }?.asString?.takeIf { valid(k, it) }?.let { k to it } }?.toMap() ?: emptyMap()
        return CustomSkin(id, name, base, values)
    }

    fun cleanName(name: String?): String? = name?.trim()?.filter { it >= ' ' }?.take(MAX_NAME)?.takeIf { it.isNotEmpty() }

    // ------------------------------------------------------------------ theme of a skin

    /** The skin as theme: its base with its values. */
    fun theme(s: CustomSkin): ShrugTheme =
        ThemeSettings.resolve { k, d -> if (k == ThemeSettings.PREF_SKIN) s.base else s.values[k] ?: d }.copy(id = s.id)

    /** Base and values of what is in effect now: a custom skin's values with the own changes on top. */
    fun current(read: (String, String?) -> String?): Pair<String, Map<String, String>> {
        val skin = read(ThemeSettings.PREF_SKIN, null)
        val custom = if (isCustom(skin)) parse(read(ThemeSettings.PREF_CUSTOM, null)).firstOrNull { it.id == skin } else null
        val base = custom?.base ?: Skins.byId(skin).id
        return base to ((custom?.values ?: emptyMap()) + ownValues(read))
    }

    // ------------------------------------------------------------------ export / import

    fun export(read: (String, String?) -> String?, name: String): String {
        val (base, values) = current(read)
        return JsonObject().apply {
            addProperty(FORMAT, 1)
            addProperty("name", cleanName(name) ?: "ShrugTV")
            addProperty("base", base)
            add("values", JsonObject().apply { values.forEach { (k, v) -> addProperty(k, v) } })
        }.toString()
    }

    /** A file from [export] as an own skin (new id); null if it is no ShrugTV design. Unknown values are dropped. */
    fun fromExport(text: String, newId: String): CustomSkin? = try {
        val o = JsonParser.parseString(text).asJsonObject
        if (o.get(FORMAT)?.asInt != 1) null else fromJson(o.deepCopy().apply { addProperty("id", newId) })
    } catch (e: Exception) { null }

    fun newId(now: Long = System.currentTimeMillis()) = PREFIX + now.toString(36)

    // ------------------------------------------------------------------ storing

    /** Saves what is in effect now under [name] and switches to it. Null if the list is full or the name empty. */
    fun saveCurrent(prefs: SharedPreferences, name: String): CustomSkin? {
        val clean = cleanName(name) ?: return null
        val list = parse(prefs.getString(ThemeSettings.PREF_CUSTOM, null))
        if (list.size >= MAX) return null
        val (base, values) = current(prefs::getString)
        val skin = CustomSkin(newId(), clean, base, values)
        store(prefs, list + skin, select = skin.id)
        return skin
    }

    /** Adds an imported skin and switches to it. Null if it is no ShrugTV design or the list is full. */
    fun import(prefs: SharedPreferences, text: String): CustomSkin? {
        val list = parse(prefs.getString(ThemeSettings.PREF_CUSTOM, null))
        if (list.size >= MAX) return null
        val skin = fromExport(text, newId()) ?: return null
        store(prefs, list + skin, select = skin.id)
        return skin
    }

    fun delete(prefs: SharedPreferences, id: String): Boolean {
        val list = parse(prefs.getString(ThemeSettings.PREF_CUSTOM, null))
        val gone = list.firstOrNull { it.id == id } ?: return false
        val selected = prefs.getString(ThemeSettings.PREF_SKIN, null) == id
        prefs.edit().apply {
            putString(ThemeSettings.PREF_CUSTOM, toJson(list - gone))
            // The deleted skin was in use: its base takes over, with its values as own changes, so nothing jumps
            if (selected) {
                putString(ThemeSettings.PREF_SKIN, gone.base)
                gone.values.forEach { (k, v) -> if (prefs.getString(k, null).isNullOrBlank() || prefs.getString(k, null) == ThemeSettings.FROM_SKIN) putString(k, v) }
            }
        }.apply()
        return true
    }

    private fun store(prefs: SharedPreferences, list: List<CustomSkin>, select: String) {
        prefs.edit().apply {
            putString(ThemeSettings.PREF_CUSTOM, toJson(list))
            ThemeSettings.OVERRIDES.forEach { remove(it) }
            putString(ThemeSettings.PREF_SKIN, select)
        }.apply()
    }
}
