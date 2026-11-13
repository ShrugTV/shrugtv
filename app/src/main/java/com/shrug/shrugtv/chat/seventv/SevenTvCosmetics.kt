package com.shrug.shrugtv.chat.seventv

import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

/** A 7TV name paint: a gradient (or one colour) drawn over the user name. */
data class SevenTvPaint(
    val id: String,
    /** ARGB colours of the gradient stops; one entry = solid colour. */
    val colors: IntArray,
    /** Positions 0..1 of the stops, same length as [colors]. */
    val positions: FloatArray,
    /** CSS angle in degrees (0 = bottom to top, 90 = left to right). */
    val angle: Float = 90f,
    val radial: Boolean = false,
    val repeat: Boolean = false
) {
    override fun equals(other: Any?) = other is SevenTvPaint && other.id == id
    override fun hashCode() = id.hashCode()
}

/**
 * 7TV cosmetics of the chatters in the current channel (paints, badges, personal emote sets), as
 * the 7TV browser extension shows them. Filled from the 7TV event API (`cosmetic.*`,
 * `entitlement.*` with the channel as context, see EmoteLiveUpdates); pure parsing, so it is
 * unit-tested.
 */
class SevenTvCosmetics {
    val paints = ConcurrentHashMap<String, SevenTvPaint>()
    /** Badge id -> image URL. */
    val badgeUrls = ConcurrentHashMap<String, String>()
    private val userPaint = ConcurrentHashMap<String, String>()
    private val userBadge = ConcurrentHashMap<String, String>()
    private val userEmoteSet = ConcurrentHashMap<String, String>()

    fun paintOf(twitchUserId: String?): SevenTvPaint? = twitchUserId?.let { userPaint[it] }?.let { paints[it] }
    fun badgeUrlOf(twitchUserId: String?): String? = twitchUserId?.let { userBadge[it] }?.let { badgeUrls[it] }
    fun emoteSetOf(twitchUserId: String?): String? = twitchUserId?.let { userEmoteSet[it] }

    /** `cosmetic.create`: body.object = { id, kind: PAINT|BADGE, data }. */
    fun onCosmetic(obj: JsonObject?) {
        obj ?: return
        val kind = obj.str("kind")
        val data = obj.getAsJsonObject("data") ?: return
        val id = data.str("id") ?: obj.str("id") ?: return
        when (kind) {
            "PAINT" -> parsePaint(id, data)?.let { paints[id] = it }
            "BADGE" -> badgeUrl(data)?.let { badgeUrls[id] = it }
        }
    }

    /**
     * `entitlement.create` / `entitlement.delete`: body.object = { kind: PAINT|BADGE|EMOTE_SET,
     * ref_id, user: { connections: [{ platform: TWITCH, id }] } }. Returns the Twitch user id and
     * the emote set id for a new personal emote set (to be loaded), otherwise null.
     */
    fun onEntitlement(obj: JsonObject?, created: Boolean): Pair<String, String>? {
        obj ?: return null
        val kind = obj.str("kind") ?: return null
        val ref = obj.str("ref_id") ?: return null
        val twitchId = twitchIdOf(obj.getAsJsonObject("user")) ?: return null
        val map = when (kind) {
            "PAINT" -> userPaint
            "BADGE" -> userBadge
            "EMOTE_SET" -> userEmoteSet
            else -> return null
        }
        if (created) {
            map[twitchId] = ref
            if (kind == "EMOTE_SET") return twitchId to ref
        } else if (map[twitchId] == ref) {
            map.remove(twitchId)
        }
        return null
    }

    companion object {
        private fun JsonObject.str(k: String): String? = get(k)?.takeIf { it.isJsonPrimitive }?.asString

        /** 7TV colours are packed RGBA (signed 32 bit); Android wants ARGB. */
        @JvmStatic
        fun argb(rgba: Long): Int {
            val v = rgba and 0xFFFFFFFFL
            val a = (v and 0xFF).toInt()
            val rgb = ((v ushr 8) and 0xFFFFFF).toInt()
            return (a shl 24) or rgb
        }

        @JvmStatic
        fun twitchIdOf(user: JsonObject?): String? {
            val conns = user?.getAsJsonArray("connections") ?: return null
            for (c in conns) {
                val o = c.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                if (o.str("platform") == "TWITCH") return o.str("id")
            }
            return null
        }

        /** v3 paint: function LINEAR_GRADIENT | RADIAL_GRADIENT | URL, color, stops[{at, color}], angle, repeat. */
        @JvmStatic
        fun parsePaint(id: String, d: JsonObject): SevenTvPaint? {
            val stops = d.getAsJsonArray("stops")
            val function = d.str("function") ?: "LINEAR_GRADIENT"
            if (stops != null && stops.size() > 1 && function != "URL") {
                val colors = IntArray(stops.size())
                val positions = FloatArray(stops.size())
                stops.forEachIndexed { i, e ->
                    val o = e.asJsonObject
                    colors[i] = argb(o.get("color")?.asLong ?: 0L)
                    positions[i] = (o.get("at")?.asFloat ?: (i.toFloat() / (stops.size() - 1))).coerceIn(0f, 1f)
                }
                return SevenTvPaint(id, colors, positions,
                    angle = d.get("angle")?.takeIf { it.isJsonPrimitive }?.asFloat ?: 90f,
                    radial = function == "RADIAL_GRADIENT",
                    repeat = d.get("repeat")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false)
            }
            // Image paints and single colours: the paint's colour as a solid name colour
            val c = d.get("color")?.takeIf { it.isJsonPrimitive }?.asLong
                ?: stops?.firstOrNull()?.asJsonObject?.get("color")?.asLong
                ?: return null
            return SevenTvPaint(id, intArrayOf(argb(c)), floatArrayOf(0f))
        }

        /** Badge image: host { url, files[] } (2x WEBP preferred), older payloads had urls[[size, url]]. */
        @JvmStatic
        fun badgeUrl(d: JsonObject): String? {
            d.getAsJsonObject("host")?.let { host ->
                val base = host.str("url") ?: return@let
                val files = host.getAsJsonArray("files")?.map { it.asJsonObject } ?: emptyList()
                val file = files.firstOrNull { it.str("name") == "2x.webp" } ?: files.firstOrNull { it.str("format") == "WEBP" } ?: files.firstOrNull()
                val name = file?.str("name") ?: "2x.webp"
                return (if (base.startsWith("//")) "https:$base" else base) + "/" + name
            }
            d.getAsJsonArray("urls")?.let { urls ->
                val pick = urls.map { it.asJsonArray }.firstOrNull { it[0].asString == "2" } ?: urls.firstOrNull()?.asJsonArray
                return pick?.get(1)?.asString
            }
            return null
        }
    }
}
