package com.shrug.shrugtv.update

import com.google.gson.JsonParser

/**
 * Update check of the public edition: is there a newer release than the running one?
 * Pure logic, no Android and no network, so it can be tested with sample answers. The GitHub and Gitea
 * (Codeberg) release APIs share the fields `tag_name`, `html_url`, `body`, `draft` and `prerelease`.
 */
object UpdateCheck {

    data class Release(val tag: String, val version: String, val url: String, val notes: String)

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
        else Release(tag, tag.removePrefix("v"), str("html_url"), str("body"))
    } catch (e: Exception) {
        null
    }

    /** The release if it is newer than [local] and was not skipped, otherwise null. */
    fun available(local: String, json: String, skipped: String?): Release? {
        val r = parseRelease(json) ?: return null
        return if (isNewer(local, r.tag) && r.version != skipped) r else null
    }
}
