package com.shrug.shrugtv.ui.tv.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.Preference

/*
 * Placeholders for the entries of the "Appearance" page in res/xml/settings.xml. The settings
 * are drawn by SettingsScreen (Compose); these classes only exist so that PreferenceManager.setDefaultValues, which
 * inflates the same XML, knows the tags.
 */

/** A colour: "#RRGGBB" or "skin" (from the skin). */
class ColorPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)
}

/** The skin, shown as a row of tiles. */
class SkinPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs) {
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)
}

/** A live preview of the theme in effect. */
class DesignPreviewPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Preference(context, attrs)
