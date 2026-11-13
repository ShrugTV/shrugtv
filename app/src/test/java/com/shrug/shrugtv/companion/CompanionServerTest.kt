package com.shrug.shrugtv.companion

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.shrug.shrugtv.ui.tv.settings.Entry
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The companion server on a real local port (docs/companion_plan.md 3). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class CompanionServerTest {
    private val port = 18765
    private val pairing = Pairing()
    private lateinit var server: CompanionServer
    private val client = OkHttpClient.Builder().followRedirects(false).build()
    private val base = "http://127.0.0.1:$port"

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        // These tests are about pairing: PIN on (the default is off since )
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(CompanionServer.PREF_REQUIRE_PIN, true).commit()
        server = CompanionServer(ctx, port, pairing) {}
        server.start(0, true)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun get(path: String, cookie: String? = null): Response =
        client.newCall(Request.Builder().url(base + path).apply { if (cookie != null) header("Cookie", cookie) }.build()).execute()

    private fun pairedCookie(): String {
        val offer = pairing.newOffer()
        val r = get("/p/${offer.token}")
        assertEquals(303, r.code)
        assertEquals("/", r.header("Location"))
        val setCookie = r.header("Set-Cookie")!!
        assertTrue(setCookie.contains("HttpOnly") && setCookie.contains("SameSite=Strict"))
        return setCookie.substringBefore(";")
    }

    @Test
    fun `api needs pairing, the QR link pairs`() {
        assertEquals(401, get("/api/state").code)
        val cookie = pairedCookie()
        val r = get("/api/state", cookie)
        assertEquals(200, r.code)
        val json = JsonParser.parseString(r.body!!.string()).asJsonObject
        assertEquals("none", json.get("kind").asString)
        assertTrue(json.has("chat_allowed"))
    }

    @Test
    fun `without the PIN switch every LAN request is allowed`() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
            .edit().putBoolean(CompanionServer.PREF_REQUIRE_PIN, false).commit()
        assertEquals(200, get("/api/state").code)
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
            .edit().putBoolean(CompanionServer.PREF_REQUIRE_PIN, true).commit()
        assertEquals(401, get("/api/state").code)
    }

    @Test
    fun `used or wrong tokens redirect to the expired page`() {
        val offer = pairing.newOffer()
        get("/p/${offer.token}").close()
        assertEquals("/?expired=1", get("/p/${offer.token}").header("Location"))
        assertEquals("/?expired=1", get("/p/nonsense").header("Location"))
    }

    @Test
    fun `wrong PIN is refused, right PIN pairs`() {
        val offer = pairing.newOffer()
        val wrong = client.newCall(Request.Builder().url("$base/api/pin").post("""{"pin":"000000"}""".toRequestBody()).build()).execute()
        assertEquals(401, wrong.code)
        val right = client.newCall(Request.Builder().url("$base/api/pin").post("""{"pin":"${offer.pin}"}""".toRequestBody()).build()).execute()
        assertEquals(200, right.code)
        assertNotNull(right.header("Set-Cookie"))
    }

    @Test
    fun `web app is served, unknown files are not`() {
        val r = get("/")
        assertEquals(200, r.code)
        assertTrue(r.body!!.string().contains("<script src=\"/app.js\">"))
        assertEquals(200, get("/app.js").code)
        assertEquals(404, get("/../secrets").code)
    }

    @Test
    fun `favicon is served as png under both addresses`() {
        for (path in listOf("/favicon.png", "/favicon.ico")) {
            val r = get(path)
            assertEquals(200, r.code)
            assertEquals("image/png", r.header("Content-Type"))
            val bytes = r.body!!.bytes()
            assertTrue("PNG signature", bytes.size > 8 && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte())
        }
    }

    @Test
    fun `websocket sends the state to a paired phone and closes for others`() {
        val cookie = pairedCookie()
        val got = CountDownLatch(1)
        var first: String? = null
        val ws = client.newWebSocket(Request.Builder().url("ws://127.0.0.1:$port/ws").header("Cookie", cookie).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { first = text; got.countDown() }
        })
        assertTrue("state within 5 s", got.await(5, TimeUnit.SECONDS))
        assertEquals("state", JsonParser.parseString(first).asJsonObject.get("t").asString)
        ws.close(1000, null)

        val closed = CountDownLatch(1)
        var code = 0
        client.newWebSocket(Request.Builder().url("ws://127.0.0.1:$port/ws").build(), object : WebSocketListener() {
            override fun onClosing(webSocket: WebSocket, c: Int, reason: String) { code = c; closed.countDown() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { closed.countDown() }
        })
        assertTrue(closed.await(5, TimeUnit.SECONDS))
        assertEquals(1008, code)
    }

    // ------------------------------------------------------------------ settings from the browser

    private fun post(path: String, body: String, cookie: String?, origin: String? = null): Response =
        client.newCall(Request.Builder().url(base + path).post(body.toRequestBody()).apply {
            if (cookie != null) header("Cookie", cookie)
            if (origin != null) header("Origin", origin)
        }.build()).execute()

    private fun leaves(e: com.google.gson.JsonElement, out: MutableList<JsonObject> = ArrayList()): List<JsonObject> {
        val o = e.asJsonObject
        if (o.has("children")) o.getAsJsonArray("children").forEach { leaves(it, out) } else out.add(o)
        return out
    }

    private fun allLeaves(tree: JsonObject) = tree.getAsJsonArray("pages").flatMap { leaves(it) }

    @Test
    fun `settings need pairing and come as the same tree as on the TV`() {
        assertEquals(401, get("/api/settings").code)
        val cookie = pairedCookie()
        val r = get("/api/settings?lang=en", cookie)
        assertEquals(200, r.code)
        val tree = JsonParser.parseString(r.body!!.string()).asJsonObject
        val keys = allLeaves(tree).mapNotNull { it.get("key")?.asString }
        assertTrue(keys.contains("pref_companion_settings"))
        assertTrue(keys.contains("pref_chat_layout"))
        assertFalse("developer page hidden", tree.getAsJsonArray("pages").any { it.asJsonObject.get("key")?.asString == "pref_developer" })
        val password = allLeaves(tree).first { it.get("key")?.asString == "pref_mqtt_password" }
        assertTrue(password.get("secret").asBoolean)
        assertNull("password never sent", password.get("value"))
        assertTrue("MQTT group has the connection card", tree.toString().contains("\"panel\":\"mqtt\""))
        val mqtt = JsonParser.parseString(get("/api/mqtt", cookie).body!!.string()).asJsonObject
        assertEquals("off", mqtt.get("status").asString)
        assertTrue(mqtt.getAsJsonObject("counts").get("controls").asInt > 5)
        assertEquals(401, get("/api/mqtt").code)
    }

    @Test
    fun `valid values are stored, wrong ones refused`() {
        val cookie = pairedCookie()
        val tree = JsonParser.parseString(get("/api/settings", cookie).body!!.string()).asJsonObject
        val list = allLeaves(tree).first { it.get("type")?.asString == "list" && it.getAsJsonArray("options").size() > 1 }
        val key = list.get("key").asString
        val option = list.getAsJsonArray("options")[1].asJsonArray[0].asString
        val body = """{"values":{"$key":"$option","pref_companion_chat":false,"pref_chat_layout":"nonsense","pref_unknown":true,"pref_developer_mode":true}}"""
        val res = JsonParser.parseString(post("/api/settings", body, cookie).body!!.string()).asJsonObject
        assertEquals(2, res.get("stored").asInt)
        val rejected = res.getAsJsonArray("rejected").map { it.asString }.toSet()
        assertEquals(setOf("pref_chat_layout", "pref_unknown", "pref_developer_mode"), rejected)
        val prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(option, prefs.getString(key, null))
        assertFalse(prefs.getBoolean("pref_companion_chat", true))
        assertFalse(prefs.getBoolean("pref_developer_mode", false))
    }

    @Test
    fun `foreign origins and switched off settings are refused`() {
        val cookie = pairedCookie()
        assertEquals(403, post("/api/settings", """{"values":{"pref_companion_chat":false}}""", cookie, "http://evil.example").code)
        assertEquals(200, post("/api/settings", """{"values":{"pref_companion_chat":false}}""", cookie, "http://127.0.0.1:$port").code)
        assertEquals(200, post("/api/settings/action", """{"key":"pref_mqtt_status"}""", cookie).code)
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
            .edit().putBoolean(SettingsApi.ALLOW_KEY, false).commit()
        assertEquals(403, get("/api/settings", cookie).code)
        assertEquals(403, post("/api/settings", """{"values":{"pref_companion_chat":true}}""", cookie).code)
    }

    @Test
    fun `backups leave out MQTT and companion settings`() {
        val cookie = pairedCookie()
        val tree = JsonParser.parseString(get("/api/settings", cookie).body!!.string()).asJsonObject
        val byKey = allLeaves(tree).filter { it.has("key") }.associateBy { it.get("key").asString }
        assertTrue(byKey.getValue("pref_mqtt_url").get("noBackup").asBoolean)
        assertTrue(byKey.getValue("pref_companion_chat").get("noBackup").asBoolean)
        assertNull(byKey.getValue("pref_chat_layout").get("noBackup"))
        val body = """{"backup":true,"values":{"pref_mqtt_url":"10.0.0.9","pref_companion_chat":false,"pref_chat_layout":"overlay"}}"""
        val res = JsonParser.parseString(post("/api/settings", body, cookie).body!!.string()).asJsonObject
        assertEquals(1, res.get("stored").asInt)
        assertEquals(setOf("pref_mqtt_url", "pref_companion_chat"), res.getAsJsonArray("skipped").map { it.asString }.toSet())
        val prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
        assertEquals("overlay", prefs.getString("pref_chat_layout", null))
        assertTrue(prefs.getString("pref_mqtt_url", "").isNullOrEmpty())
    }

    @Test
    fun `log needs pairing and can be switched off`() {
        assertEquals(401, get("/api/log").code)
        val cookie = pairedCookie()
        val r = get("/api/log?after=0", cookie)
        assertEquals(200, r.code)
        val json = JsonParser.parseString(r.body!!.string()).asJsonObject
        assertTrue(json.has("lines") && json.has("first"))
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext<Context>())
            .edit().putBoolean("pref_companion_log", false).commit()
        assertEquals(403, get("/api/log", cookie).code)
    }

    @Test
    fun `remembered devices come back without a code after a restart`() {
        server.shutdown()
        val memory = object : TrustedDevices.Store {
            var json: String? = null
            override fun load() = json
            override fun save(json: String) { this.json = json }
        }
        val devices = TrustedDevices(memory)
        val ctx: Context = ApplicationProvider.getApplicationContext()
        server = CompanionServer(ctx, port, pairing, devices) {}
        server.start(0, true)

        val offer = pairing.newOffer()
        val setCookie = get("/p/${offer.token}").header("Set-Cookie")!!
        assertTrue("persistent cookie", setCookie.contains("Max-Age="))
        val cookie = setCookie.substringBefore(";")
        assertEquals(1, devices.list().size)

        // App restart: sessions in memory are gone, the device key still works
        pairing.disconnectAll()
        assertEquals(200, get("/api/state", cookie).code)
        val list = JsonParser.parseString(get("/api/devices", cookie).body!!.string()).asJsonObject
        assertEquals(list.getAsJsonArray("devices")[0].asJsonObject.get("id").asString, list.get("current").asString)

        // Removed on the TV: the code is needed again
        devices.remove(devices.list()[0].id)
        assertEquals(401, get("/api/state", cookie).code)

        // "Remember connected devices" off: a plain session cookie, gone after a restart
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean("pref_companion_remember", false).commit()
        val offer2 = pairing.newOffer()
        val plain = get("/p/${offer2.token}").header("Set-Cookie")!!
        assertFalse(plain.contains("Max-Age="))
        pairing.disconnectAll()
        assertEquals(401, get("/api/state", plain.substringBefore(";")).code)
    }

    @Test
    fun `validation follows the entry type`() {
        val toggle = Entry.Toggle("k", "t", null, false, null, null, null)
        val list = Entry.ListChoice("k", "t", null, listOf("A", "B"), listOf("a", "b"), "a", null)
        val text = Entry.TextField("k", "t", null, "", null, null)
        assertEquals(true, SettingsApi.validate(toggle, JsonPrimitive(true)))
        assertNull(SettingsApi.validate(toggle, JsonPrimitive("true")))
        assertEquals("b", SettingsApi.validate(list, JsonPrimitive("b")))
        assertNull(SettingsApi.validate(list, JsonPrimitive("c")))
        assertEquals("hello", SettingsApi.validate(text, JsonPrimitive("hello")))
        assertNull(SettingsApi.validate(text, JsonPrimitive("x".repeat(SettingsApi.MAX_TEXT + 1))))
        assertNull(SettingsApi.validate(text, JsonPrimitive("line\nbreak")))
        assertNull(SettingsApi.validate(text, JsonObject()))
        val number = Entry.NumberField("k", "t", null, 78, 20, 300, 5, null)
        assertEquals("120", SettingsApi.validate(number, JsonPrimitive(120)))
        assertEquals("120", SettingsApi.validate(number, JsonPrimitive("120")))
        assertNull(SettingsApi.validate(number, JsonPrimitive(19)))
        assertNull(SettingsApi.validate(number, JsonPrimitive(301)))
        assertNull(SettingsApi.validate(number, JsonPrimitive(12.5)))
        assertNull(SettingsApi.validate(number, JsonPrimitive("abc")))
        assertNull(SettingsApi.validate(number, JsonPrimitive(true)))
    }
}
