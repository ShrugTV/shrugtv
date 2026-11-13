package com.shrug.shrugtv.chat.emote

import android.graphics.drawable.Drawable
import pl.droidsonroids.gif.GifDrawable

/**
 * Represents a Twitch, 7TV, BTTV or FFZ emote.
 * Fully modernised in Kotlin for stage 2 (#108).
 */
data class Emote(
    val id: String = "",
    val code: String = "",
    val url: String = "",
    val provider: Provider = Provider.TWITCH,
    val isZeroWidth: Boolean = false,
    var isAnimated: Boolean = false,
    val isWebp: Boolean = false,
    var isGif: Boolean = false,
    val positions: MutableList<String> = mutableListOf()
) {
    var drawable: Drawable? = null
    var gifDrawable: GifDrawable? = null

    fun addPosition(position: String) {
        positions.add(position)
    }

    enum class Provider {
        BTTV,
        FFZ,
        TWITCH,
        SEVEN_TV
    }

    class Builder {
        private var id: String = ""
        private var code: String = ""
        private var url: String = ""
        private var provider: Provider = Provider.TWITCH
        private var isZeroWidth: Boolean = false
        private var isAnimated: Boolean = false
        private var isWebp: Boolean = false
        private var isGif: Boolean = false
        private var positions: MutableList<String> = mutableListOf()

        fun id(id: String) = apply { this.id = id }
        fun code(code: String) = apply { this.code = code }
        fun url(url: String) = apply { this.url = url }
        fun provider(provider: Provider) = apply { this.provider = provider }
        fun zeroWidth(zeroWidth: Boolean) = apply { this.isZeroWidth = zeroWidth }
        fun animated(animated: Boolean) = apply { this.isAnimated = animated }
        fun webp(webp: Boolean) = apply { this.isWebp = webp }
        fun gif(gif: Boolean) = apply {
            this.isGif = gif
            this.isAnimated = gif
        }
        fun positions(positions: List<String>) = apply {
            this.positions = positions.toMutableList()
        }

        fun build(): Emote {
            val resolvedUrl = if (url.startsWith("//")) "https:$url" else url
            return Emote(
                id = id,
                code = code,
                url = resolvedUrl,
                provider = provider,
                isZeroWidth = isZeroWidth,
                isAnimated = isAnimated,
                isWebp = isWebp,
                isGif = isGif,
                positions = positions
            )
        }
    }
}
