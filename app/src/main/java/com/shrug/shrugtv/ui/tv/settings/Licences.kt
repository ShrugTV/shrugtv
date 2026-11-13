package com.shrug.shrugtv.ui.tv.settings

/**
 * Third-party components bundled in the APK and their licences, shown on the "Licences" info page
 *. Licences read from the Maven POMs in the Gradle cache on 2026-11-01;
 * when app/build.gradle gains or drops a library, change this list too.
 */
object Licences {
    data class Component(val name: String, val licence: String, val note: String? = null)

    val components = listOf(
        Component("AndroidX (Activity, AppCompat, CardView, ConstraintLayout, Core, Lifecycle, Palette, Preference, RecyclerView, Room, TV Provider, WorkManager)", "Apache License 2.0"),
        Component("Jetpack Compose and Compose for TV", "Apache License 2.0"),
        Component("AndroidX Media3 (ExoPlayer, HLS, UI)", "Apache License 2.0",
            "TwitchHlsPlaylistParser is a modified copy of the Media3 HlsPlaylistParser."),
        Component("Material Components for Android", "Apache License 2.0"),
        Component("Kotlin standard library, kotlinx.coroutines, kotlinx.serialization", "Apache License 2.0"),
        Component("OkHttp", "Apache License 2.0"),
        Component("Gson", "Apache License 2.0"),
        Component("Coil", "Apache License 2.0"),
        Component("Glide", "BSD 2-Clause and Apache License 2.0"),
        Component("GlideWebpDecoder (webpdecoder)", "BSD 2-Clause and Apache License 2.0"),
        Component("glide-transformations", "Apache License 2.0"),
        Component("android-gif-drawable", "MIT License"),
        Component("ZXing core", "Apache License 2.0"),
        Component("Eclipse Paho MQTT client", "Eclipse Public License 2.0"),
        Component("NanoHTTPD", "BSD 3-Clause License"),
        Component("Inter typeface", "SIL Open Font License 1.1"),
        Component("Lucide icons", "ISC License"),
    )
}
