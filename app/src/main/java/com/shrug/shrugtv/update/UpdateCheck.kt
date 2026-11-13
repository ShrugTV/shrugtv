package com.shrug.shrugtv.update

import com.google.gson.JsonParser

/**
 * Update check of the public edition: is there a newer release than the running one?
 * Pure logic, no Android and no network, so it can be tested with sample answers. The GitHub and Gitea
 * (Codeberg) release APIs share the fields `tag_name`, `html_url`, `body`, `draft` and `prerelease`.
 */
object UpdateCheck {

    /** [apkUrl] and [sha256] are empty when the release has no matching file or no checksum line in its text. */
    data class Release(val tag: String, val version: String, val url: String, val notes: String, val apkUrl: String = "", val sha256: String = "")

    /** The checksum the release script writes into the text: "SHA-256 of `shrugtv-2.1.2.apk`: `<64 hex digits>`". */
    fun checksumIn(body: String): String =
        Regex("SHA-256 of `[^`]*`: `([0-9a-fA-F]{64})`").find(body)?.groupValues?.get(1)?.lowercase().orEmpty()

    /** "v2.1.0" or "2.1.0" or a development state like "2.0.0-5-gabc123" to major.minor.patch; null for anything else. */
    fun baseVersion(name: String): Triple<Int, Int, Int>? {
        val m = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-.*)?$""").matchEntire(name.trim()) ?: return null
        return Triple(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }

    private fun Triple<Int, Int, Int>.value() = first.toLong() * 1_000_000 + second * 1_000 + third

    /**
     * True when [remote] is a plain release newer than [local]. A development state "2.0.0-5-gabc" counts as 2.0.0,
     * so it is older than 2.0.1 but not older than 2.0.0. A pre-release tag ("2.1.0-rc1") never counts as an update.
     */
    fun isNewer(local: String, remote: String): Boolean {
        val l = baseVersion(local) ?: return false
        if (remote.trim().removePrefix("v").contains('-')) return false
        val r = baseVersion(remote) ?: return false
        return r.value() > l.value()
    }

    /** The release in an API answer, or null for broken JSON, drafts, pre-releases and answers without a usable tag. */
    fun parseRelease(json: String): Release? = try {
        val o = JsonParser.parseString(json).asJsonObject
        fun str(k: String) = if (o.has(k) && !o.get(k).isJsonNull) o.get(k).asString else ""
        val tag = str("tag_name")
        if (o.has("draft") && o.get("draft").asBoolean || o.has("prerelease") && o.get("prerelease").asBoolean || baseVersion(tag) == null) null
        else {
            val version = tag.removePrefix("v")
            val assets = if (o.has("assets") && o.get("assets").isJsonArray) o.getAsJsonArray("assets").map { it.asJsonObject } else emptyList()
            fun asset(name: String) = assets.firstOrNull { it.has("name") && it.get("name").asString == name }
                ?.takeIf { it.has("browser_download_url") }?.get("browser_download_url")?.asString
            // The file named after the version first: it is the one the checksum in the text belongs to
            val apk = asset("shrugtv-$version.apk") ?: asset("shrugtv.apk") ?: ""
            Release(tag, version, str("html_url"), str("body"), apk, checksumIn(str("body")))
        }
    } catch (e: Exception) {
        null
    }

    /** The first lines of the release text without Markdown marks, short enough for the dialog; "" when there is none. */
    fun shortNotes(body: String, maxLines: Int = 6, maxChars: Int = 420): String {
        val lines = body.lines().map { it.trim().trimStart('#').trim().replace("`", "").replace("**", "") }
            .filter { it.isNotEmpty() && !it.startsWith("SHA-256") }
            .map { if (it.startsWith("- ") || it.startsWith("* ")) "\u2022 " + it.drop(2) else it }
        val text = lines.take(maxLines).joinToString("\n")
        return if (text.length > maxChars || lines.size > maxLines) text.take(maxChars).trimEnd() + "\u2026" else text
    }

    /** The release if it is newer than [local] and was not skipped, otherwise null. */
    fun available(local: String, json: String, skipped: String?): Release? {
        val r = parseRelease(json) ?: return null
        return if (isNewer(local, r.tag) && r.version != skipped) r else null
    }
}
