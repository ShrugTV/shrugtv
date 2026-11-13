package com.shrug.shrugtv.companion

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Remembered phones and computers of the companion (setting "Remember connected devices"): after
 * the first pairing the browser keeps a long-lived secret key as its cookie, so it reconnects
 * without a new code, also after the app restarted. Only a SHA-256 hash of the key is stored on the
 * TV (in `CompanionDevices.xml`, excluded from backups). Pure apart from [Store], so it is
 * unit-tested.
 */
class TrustedDevices(
    private val store: Store,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val random: SecureRandom = SecureRandom()
) {
    interface Store {
        fun load(): String?
        fun save(json: String)
    }

    data class Device(
        val id: String,
        val keyHash: String,
        val name: String,
        val pairedAt: Long,
        val lastSeen: Long,
        val lastIp: String? = null
    )

    companion object {
        const val MAX_DEVICES = 10
        /** "last seen" is written at most this often (every request would wear the storage). */
        private const val SEEN_SAVE_MS = 60_000L

        @JvmStatic
        fun hash(key: String): String =
            MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }

        /** "Chrome · Windows", "Safari · iPhone", "Firefox · Android" from a user agent. */
        @JvmStatic
        fun deviceName(userAgent: String): String {
            val ua = userAgent
            val browser = when {
                "Edg/" in ua -> "Edge"
                "OPR/" in ua || "Opera" in ua -> "Opera"
                "SamsungBrowser" in ua -> "Samsung Internet"
                "Firefox/" in ua || "FxiOS" in ua -> "Firefox"
                "CriOS" in ua || ("Chrome/" in ua && "Chromium" !in ua) -> "Chrome"
                "Chromium" in ua -> "Chromium"
                "Safari/" in ua -> "Safari"
                else -> null
            }
            val system = when {
                "iPhone" in ua -> "iPhone"
                "iPad" in ua -> "iPad"
                "Android" in ua -> Regex("Android [0-9.]+; ([^;)]+?)(?: Build|\\))").find(ua)?.groupValues?.get(1)
                    ?.trim()?.takeIf { it.isNotEmpty() && it != "K" && !it.startsWith("wv") }?.let { "Android ($it)" } ?: "Android"
                "Windows" in ua -> "Windows"
                "Mac OS X" in ua || "Macintosh" in ua -> "Mac"
                "CrOS" in ua -> "ChromeOS"
                "Linux" in ua -> "Linux"
                else -> null
            }
            return listOfNotNull(browser, system).joinToString(" · ").ifEmpty { "Browser" }.take(60)
        }
    }

    private val gson = Gson()
    private val devices = LinkedHashMap<String, Device>()
    private var lastSave = 0L

    init {
        try {
            val json = store.load()
            if (!json.isNullOrEmpty()) {
                val list: List<Device> = gson.fromJson(json, object : TypeToken<List<Device>>() {}.type) ?: emptyList()
                list.forEach { devices[it.keyHash] = it }
            }
        } catch (e: Exception) {
            // Broken file: start without remembered devices
        }
    }

    /** Remembers a newly paired device; returns it and the secret key for its cookie. */
    @Synchronized
    fun add(userAgent: String, ip: String?): Pair<Device, String> {
        val key = randomHex(32)
        val t = now()
        val d = Device(randomHex(6), hash(key), deviceName(userAgent), t, t, ip)
        devices[d.keyHash] = d
        // Keep the list short: the device unused for the longest time goes first
        while (devices.size > MAX_DEVICES) devices.remove(devices.values.minByOrNull { it.lastSeen }!!.keyHash)
        save()
        return d to key
    }

    /** The device with this key (cookie), or null; updates "last seen". */
    @Synchronized
    fun find(key: String?, ip: String?): Device? {
        if (key.isNullOrEmpty() || key.length != 64) return null
        val h = hash(key)
        val d = devices[h] ?: return null
        val t = now()
        val updated = d.copy(lastSeen = t, lastIp = ip ?: d.lastIp)
        devices[h] = updated
        if (t - lastSave > SEEN_SAVE_MS) save()
        return updated
    }

    @Synchronized
    fun list(): List<Device> = devices.values.sortedByDescending { it.lastSeen }

    @Synchronized
    fun remove(id: String): Boolean {
        val gone = devices.values.removeAll { it.id == id }
        if (gone) save()
        return gone
    }

    @Synchronized
    fun clear() {
        devices.clear()
        save()
    }

    private fun save() {
        lastSave = now()
        store.save(gson.toJson(devices.values.toList()))
    }

    private fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }
}
