package com.shrug.shrugtv.ha

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shrug.shrugtv.hub.PlaybackSnapshot
import com.shrug.shrugtv.hub.PlaybackStateHub
import com.shrug.shrugtv.hub.PlayerCommands
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/** MqttBridge against an embedded broker: discovery, state, commands, Last Will (docs/mqtt_ha_plan.md). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class MqttBridgeTest {
    private val port = 18883
    private lateinit var broker: Server
    private lateinit var listener: MqttClient
    private val received = ConcurrentHashMap<String, String>()
    private val calls = mutableListOf<String>()

    private val fakePlayer = object : PlayerCommands {
        override fun playPause() { calls += "play_pause" }
        override fun play() { calls += "play" }
        override fun pause() { calls += "pause" }
        override fun jumpToLive() { calls += "live" }
        override fun zap(direction: Int) { calls += "zap $direction" }
        override fun close() { calls += "close" }
        override fun watchLogin(login: String) { calls += "watch $login" }
        override fun watchVod(videoId: Long, positionSeconds: Int?) { calls += "vod $videoId" }
        override fun selectQualityByName(name: String) { calls += "quality $name" }
        override fun setChatVisible(visible: Boolean) { calls += "chat $visible" }
        override fun setChatLayout(value: String) { calls += "layout $value" }
        override fun setVolume(percent: Int) { calls += "volume $percent" }
        override fun sendChat(text: String) { calls += "say $text" }
        override fun showNotice(title: String, text: String?, imageUrl: String?, seconds: Int) { calls += "notice $title" }
    }

    @Before
    fun setUp() {
        broker = Server()
        broker.startServer(MemoryConfig(Properties().apply {
            setProperty("port", port.toString())
            setProperty("host", "127.0.0.1")
            setProperty("allow_anonymous", "true")
            setProperty("persistence_enabled", "false")
        }))
        listener = MqttClient("tcp://127.0.0.1:$port", "test-listener", MemoryPersistence())
        listener.connect(MqttConnectOptions().apply { isCleanSession = true })
        listener.subscribe("#", 1) { topic, msg: MqttMessage -> received[topic] = String(msg.payload) }

        val ctx: Context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
            .putBoolean("pref_mqtt_enabled", true)
            .putString("pref_mqtt_url", "127.0.0.1:$port")
            .putString("pref_mqtt_device_name", "Testzimmer")
            .putBoolean("pref_mqtt_commands", true)
            .commit()
        PlaybackStateHub.commands = fakePlayer
        PlaybackStateHub.publish(PlaybackSnapshot(status = "playing", kind = "live", channel = "Papaplatte", login = "papaplatte", qualities = listOf("Auto", "720p60")))
        MqttBridge.init(ctx)
    }

    @After
    fun tearDown() {
        MqttBridge.stop()
        PlaybackStateHub.clear(fakePlayer)
        try { listener.disconnect() } catch (_: Exception) {}
        broker.stopServer()
    }

    private fun waitFor(what: String, timeoutMs: Long = 8_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (cond()) return
            Thread.sleep(50)
        }
        throw AssertionError("timeout: $what; received topics: ${received.keys}")
    }

    @Test
    fun `announces the TV, publishes the state and takes commands`() {
        MqttBridge.start()
        waitFor("online") { received["shrugtv/testzimmer/availability"] == "online" }
        waitFor("discovery") { received.keys.any { it == "homeassistant/sensor/shrugtv_testzimmer/status/config" } }
        waitFor("state") { received["shrugtv/testzimmer/state"]?.contains("Papaplatte") == true }
        waitFor("quality select") { received["homeassistant/select/shrugtv_testzimmer/quality/config"]?.contains("720p60") == true }

        listener.publish("shrugtv/testzimmer/cmd/play_pause", MqttMessage("PRESS".toByteArray()))
        listener.publish("shrugtv/testzimmer/cmd/watch", MqttMessage("Bonjwa".toByteArray()))
        listener.publish("shrugtv/testzimmer/cmd/watch", MqttMessage("not a login!".toByteArray()))
        waitFor("commands") { calls.size >= 2 }
        assertEquals(listOf("play_pause", "watch bonjwa"), calls.take(2))
        assertEquals("connected", MqttBridge.status)

        MqttBridge.stop()
        waitFor("offline") { received["shrugtv/testzimmer/availability"] == "offline" }
    }

    @Test
    fun `a name used by another TV gets a code, the other TV is not touched`() {
        // Another TV called "Testzimmer" is already on the broker
        listener.publish("shrugtv/testzimmer/owner", MqttMessage("othertv1".toByteArray()).apply { isRetained = true })
        listener.publish("shrugtv/testzimmer/availability", MqttMessage("online".toByteArray()).apply { isRetained = true })
        received.clear()
        MqttBridge.start()
        val own = Regex("shrugtv/testzimmer_[0-9a-f]{4}/state")
        waitFor("state under the new id") { received.keys.any { own.matches(it) && received[it]!!.contains("Papaplatte") } }
        assertEquals("online", received["shrugtv/testzimmer/availability"] ?: "online")
        assertTrue(received.keys.none { it.startsWith("homeassistant/") && "/shrugtv_testzimmer/" in it })
        assertTrue(received.keys.any { Regex("homeassistant/sensor/shrugtv_testzimmer_[0-9a-f]{4}/status/config").matches(it) })
        MqttBridge.stop()
        Thread.sleep(500)
        assertEquals("online", received["shrugtv/testzimmer/availability"] ?: "online")
    }
}
