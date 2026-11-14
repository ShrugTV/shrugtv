package com.shrug.shrugtv.ha

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaDiscoveryTest {

    private fun d(commands: Boolean = true) = HaDiscovery("wohnzimmer", "Wohnzimmer", "AFTKA", "1.9.2", commands)

    @Test
    fun `every entity has device, unique id and availability`() {
        val configs = d().configs(listOf("Auto", "1080p60 (source)", "720p60"))
        assertTrue(configs.size >= 15)
        val ids = HashSet<String>()
        for ((topic, payload) in configs) {
            assertTrue(topic, topic.startsWith("homeassistant/") && topic.endsWith("/config") && "/shrugtv_wohnzimmer/" in topic)
            val o = JsonParser.parseString(payload).asJsonObject
            assertTrue(ids.add(o.get("unique_id").asString))
            assertEquals("shrugtv/wohnzimmer/availability", o.get("availability_topic").asString)
            assertEquals("shrugtv_wohnzimmer", o.getAsJsonObject("device").getAsJsonArray("identifiers")[0].asString)
            assertTrue(o.get("unique_id").asString.startsWith("shrugtv_wohnzimmer_"))
        }
    }

    @Test
    fun `quality select lists the stream qualities`() {
        val quality = d().configs(listOf("Auto", "720p60")).first { it.first.contains("/select/shrugtv_wohnzimmer/quality/") }
        val o = JsonParser.parseString(quality.second).asJsonObject
        assertEquals(listOf("Auto", "720p60"), o.getAsJsonArray("options").map { it.asString })
        assertEquals("shrugtv/wohnzimmer/cmd/quality", o.get("command_topic").asString)
    }

    @Test
    fun `without commands the controls are removed`() {
        val configs = d(commands = false).configs(emptyList())
        val buttons = configs.filter { it.first.contains("/button/") || it.first.contains("/switch/") || it.first.contains("/text/") }
        assertTrue(buttons.isNotEmpty())
        assertTrue(buttons.all { it.second.isEmpty() })
        assertTrue(configs.first { it.first.contains("/sensor/shrugtv_wohnzimmer/status/") }.second.isNotEmpty())
    }

    @Test
    fun `device id from the name`() {
        assertEquals("wohnzimmer_tv", HaDiscovery.deviceIdFrom("Wohnzimmer TV", "ab12"))
        assertEquals("kueche", HaDiscovery.deviceIdFrom("Küche", "ab12"))
        assertEquals("shrugtv_ab12", HaDiscovery.deviceIdFrom("  ", "ab12"))
        assertEquals("shrugtv_ab12", HaDiscovery.deviceIdFrom(null, "ab12"))
    }

    @Test
    fun `broker address is normalised`() {
        assertEquals("tcp://192.168.0.10:1883", MqttSettings.normaliseUrl("192.168.0.10"))
        assertEquals("tcp://broker.lan:1884", MqttSettings.normaliseUrl("broker.lan:1884"))
        assertEquals("tcp://broker.lan:1883", MqttSettings.normaliseUrl("mqtt://broker.lan"))
        assertEquals("ssl://broker.lan:8883", MqttSettings.normaliseUrl("mqtts://broker.lan"))
        assertEquals("ssl://x:8883", MqttSettings.normaliseUrl("ssl://x:8883"))
        assertEquals("", MqttSettings.normaliseUrl(" "))
    }

    @Test
    fun `prefixes move every topic`() {
        val d = HaDiscovery("tv", "TV", "AFTKA", "1", true, "home/media", "ha")
        assertEquals("home/media/tv/state", d.stateTopic)
        assertEquals("home/media/tv/cmd/#", d.commandWildcard)
        for ((topic, payload) in d.configs(emptyList())) {
            assertTrue(topic, topic.startsWith("ha/"))
            assertEquals("home/media/tv/availability", JsonParser.parseString(payload).asJsonObject.get("availability_topic").asString)
        }
    }

    @Test
    fun `switched off groups are removed, counts cover all entities`() {
        val d = HaDiscovery("tv", "TV", "AFTKA", "1", true, groups = setOf(HaDiscovery.INFO, HaDiscovery.CONTROLS))
        val configs = d.configs(emptyList())
        assertTrue(configs.filter { "/image/" in it.first || "/event/" in it.first }.all { it.second.isEmpty() })
        assertTrue(configs.first { "/sensor/shrugtv_tv/status/" in it.first }.second.isNotEmpty())
        val counts = d.counts()
        assertEquals(6, counts[HaDiscovery.INFO])
        assertEquals(1, counts[HaDiscovery.AVATAR])
        assertEquals(1, counts[HaDiscovery.PREVIEW])
        assertEquals(1, counts[HaDiscovery.EVENTS])
        assertEquals(configs.size, counts.values.sum())
    }

    @Test
    fun `followed channels are two sensors on their own topic and go away with the group`() {
        val on = HaDiscovery("tv", "TV", "AFTKA", "1", true, groups = HaDiscovery.GROUPS.toSet())
        assertEquals("shrugtv/tv/followed", on.followedTopic)
        val live = on.configs(emptyList()).first { "/sensor/shrugtv_tv/live_channels/" in it.first }.second
        assertTrue(live.contains("shrugtv/tv/followed") && live.contains("live_count") && live.contains("value_json.live"))
        val all = on.configs(emptyList()).first { "/sensor/shrugtv_tv/followed_channels/" in it.first }.second
        assertTrue(all.contains("followed_count") && all.contains("value_json.followed"))
        assertEquals(2, on.counts()[HaDiscovery.FOLLOWED])
        val off = HaDiscovery("tv", "TV", "AFTKA", "1", true, groups = HaDiscovery.GROUPS.toSet() - HaDiscovery.FOLLOWED)
        assertTrue(off.configs(emptyList()).filter { "live_channels" in it.first || "followed_channels" in it.first }.all { it.second.isEmpty() })
    }

    @Test
    fun `topic prefixes are checked`() {
        assertEquals("home/tv", MqttSettings.topicPrefix(" home/tv "))
        assertEquals("shrugtv", MqttSettings.topicPrefix("#"))
        assertEquals("shrugtv", MqttSettings.topicPrefix("a/+/b"))
        assertEquals("shrugtv", MqttSettings.topicPrefix("/lead"))
        assertEquals("shrugtv", MqttSettings.topicPrefix(""))
        // Never inside the topics of other systems
        assertEquals("shrugtv", MqttSettings.topicPrefix("zigbee2mqtt"))
        assertEquals("shrugtv", MqttSettings.topicPrefix("Tasmota/tv"))
        assertEquals("shrugtv", MqttSettings.topicPrefix("homeassistant"))
        assertEquals("ha", MqttSettings.discoveryPrefix("ha"))
        assertEquals("homeassistant", MqttSettings.discoveryPrefix("ha/#"))
        assertEquals("homeassistant", MqttSettings.discoveryPrefix(null))
    }

    @Test
    fun `discovery ids never collide with other devices of the same name`() {
        val d = HaDiscovery("wohnzimmer", "Wohnzimmer", "AFTKA", "1", true)
        assertEquals("shrugtv_wohnzimmer", d.nodeId)
        assertEquals("shrugtv/wohnzimmer/state", d.stateTopic) // own topics unchanged
        // The fallback id from the hash already starts with shrugtv: not doubled
        assertEquals("shrugtv_ab12cd34", HaDiscovery("shrugtv_ab12cd34", "x", "m", "1", true).nodeId)
    }

    @Test
    fun `channel picture and stream preview switch separately`() {
        val d = HaDiscovery("tv", "TV", "AFTKA", "1", true, groups = HaDiscovery.GROUPS.toSet() - HaDiscovery.PREVIEW)
        val configs = d.configs(emptyList())
        assertTrue(configs.first { "/image/shrugtv_tv/avatar/" in it.first }.second.isNotEmpty())
        assertTrue(configs.first { "/image/shrugtv_tv/preview/" in it.first }.second.isEmpty())
    }
}
