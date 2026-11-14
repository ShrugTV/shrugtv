package com.shrug.shrugtv.ui.tv.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Stroke icons of the navigation bar, traced from the mockup (24 grid). Own vectors because the project
 * has no Material icons library and the existing drawables do not fit.
 */
object HomeIcons {
    private fun stroke(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Search = stroke("suche", "M4,11a7,7 0 1,0 14,0a7,7 0 1,0 -14,0", "M20,20l-3.5,-3.5")
    val Clips = stroke("clips", "M4,5H20V19H4Z", "M10,9L15,12L10,15Z")
    val Stats = stroke("statistik", "M5,20V12", "M12,20V5", "M19,20V15")
    val Start = stroke("start", "M3,11l9,-7 9,7v9a1,1 0 0,1 -1,1h-5v-6H9v6H4a1,1 0 0,1 -1,-1z")
    val Grid = stroke(
        "raster",
        "M5,3h3a2,2 0 0,1 2,2v3a2,2 0 0,1 -2,2h-3a2,2 0 0,1 -2,-2v-3a2,2 0 0,1 2,-2z",
        "M16,3h3a2,2 0 0,1 2,2v3a2,2 0 0,1 -2,2h-3a2,2 0 0,1 -2,-2v-3a2,2 0 0,1 2,-2z",
        "M5,14h3a2,2 0 0,1 2,2v3a2,2 0 0,1 -2,2h-3a2,2 0 0,1 -2,-2v-3a2,2 0 0,1 2,-2z",
        "M16,14h3a2,2 0 0,1 2,2v3a2,2 0 0,1 -2,2h-3a2,2 0 0,1 -2,-2v-3a2,2 0 0,1 2,-2z",
    )
    // Gear from Lucide "settings" (ISC licence, docs/lizenzen/Lucide-ISC.txt); before a circle with eight
    // rays, which looked like a sun
    val Settings = stroke(
        "einstellungen",
        "M12.22,2h-0.44a2,2 0 0,0 -2,2v0.18a2,2 0 0,1 -1,1.73l-0.43,0.25a2,2 0 0,1 -2,0l-0.15,-0.08a2,2 0 0,0 -2.73,0.73" +
            "l-0.22,0.38a2,2 0 0,0 0.73,2.73l0.15,0.1a2,2 0 0,1 1,1.72v0.51a2,2 0 0,1 -1,1.74l-0.15,0.09a2,2 0 0,0 -0.73,2.73" +
            "l0.22,0.38a2,2 0 0,0 2.73,0.73l0.15,-0.08a2,2 0 0,1 2,0l0.43,0.25a2,2 0 0,1 1,1.73V20a2,2 0 0,0 2,2h0.44" +
            "a2,2 0 0,0 2,-2v-0.18a2,2 0 0,1 1,-1.73l0.43,-0.25a2,2 0 0,1 2,0l0.15,0.08a2,2 0 0,0 2.73,-0.73l0.22,-0.39" +
            "a2,2 0 0,0 -0.73,-2.73l-0.15,-0.08a2,2 0 0,1 -1,-1.74v-0.5a2,2 0 0,1 1,-1.74l0.15,-0.09a2,2 0 0,0 0.73,-2.73" +
            "l-0.22,-0.38a2,2 0 0,0 -2.73,-0.73l-0.15,0.08a2,2 0 0,1 -2,0l-0.43,-0.25a2,2 0 0,1 -1,-1.73V4a2,2 0 0,0 -2,-2z",
        "M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0",
    )
    val Profile = stroke("logout", "M9,21H5a2,2 0 0,1 -2,-2V5a2,2 0 0,1 2,-2h4", "M16,17l5,-5 -5,-5", "M21,12H9")
    private fun filled(name: String, d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White))
        }.build()

    /** Followed channel (player info). */
    val Heart = filled("gefolgt", "M12,21s-7,-4.5 -9.5,-9A5.5,5.5 0 0,1 12,6a5.5,5.5 0 0,1 9.5,6c-2.5,4.5 -9.5,9 -9.5,9z")

    /** Favourite channel (player info). */
    val Star = filled("favorit", "M12,2.5l2.9,6.1 6.6,0.8 -4.9,4.6 1.3,6.6L12,17.3l-5.9,3.3 1.3,-6.6 -4.9,-4.6 6.6,-0.8z")

    val Play = ImageVector.Builder("abspielen", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString("M7,4v16l13,-8z").toNodes(), fill = SolidColor(Color.Black))
    }.build()
}
