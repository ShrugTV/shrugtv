package com.shrug.shrugtv.ha

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.shrug.shrugtv.hub.HubEvent

/**
 * Home Assistant MQTT discovery for one TV (docs/mqtt_ha_plan.md section 3). Pure, so the JSON is
 * unit-tested. Every entity shares the device block, so HA shows one device per TV.
 */
class HaDiscovery(
    /** Device id, part of every topic: <prefix>/<id>/… */
    val deviceId: String,
    private val deviceName: String,
    private val model: String,
    private val swVersion: String,
    private val commandsEnabled: Boolean,
    topicPrefix: String = MqttSettings.DEFAULT_TOPIC_PREFIX,
    private val discoveryPrefix: String = MqttSettings.DEFAULT_DISCOVERY_PREFIX,
    /** Entity groups (settings "Home Assistant entities"); a switched-off group is removed from HA. */
    private val groups: Set<String> = GROUPS.toSet()
) {
    val base = "$topicPrefix/$deviceId"

    /**
     * Id in discovery topics, unique_id and the HA device: always starts with "shrugtv", so it can
     * never collide with another MQTT device of the same name (e.g. an ESPHome node "wohnzimmer").
     */
    val nodeId = if (deviceId.startsWith("shrugtv")) deviceId else "shrugtv_$deviceId"

    /** Holds the device hash of the TV that uses this device id (two TVs with the same name). */
    val ownerTopic = "$base/owner"
    val discoveryRoot = "$discoveryPrefix/+/$nodeId"
    val availabilityTopic = "$base/availability"
    val stateTopic = "$base/state"
    val eventTopic = "$base/event"
    fun commandTopic(name: String) = "$base/cmd/$name"
    val commandWildcard = "$base/cmd/#"

    /** (config topic, payload) for every entity; an empty payload removes an entity in HA. */
    fun configs(qualities: List<String>): List<Pair<String, String>> = build(qualities).map { (_, topic, cfg) -> topic to cfg }

    /** Number of entities per group (shown next to the group switches in the browser). */
    fun counts(): Map<String, Int> = build(emptyList(), all = true).groupingBy { it.first }.eachCount()

    private fun build(qualities: List<String>, all: Boolean = false): List<Triple<String, String, String>> {
        val list = ArrayList<Triple<String, String, String>>()
        var group = INFO
        fun add(component: String, objectId: String, cfg: JsonObject?) {
            val on = all || (group in groups && (group != CONTROLS || commandsEnabled))
            list.add(Triple(group, "$discoveryPrefix/$component/$nodeId/$objectId/config", if (on) cfg?.toString() ?: "" else ""))
        }

        add("sensor", "status", entity("status", "Status").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.status }}")
            addProperty("device_class", "enum")
            add("options", arr("idle", "buffering", "playing", "paused", "ended", "error"))
            addProperty("icon", "mdi:television-play")
        })
        add("sensor", "channel", entity("channel", "Channel").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.channel if value_json.channel else 'none' }}")
            addProperty("json_attributes_topic", stateTopic)
            addProperty("json_attributes_template",
                "{{ {'login': value_json.login, 'title': value_json.title, 'category': value_json.category, " +
                    "'language': value_json.language, 'kind': value_json.kind, 'vod_id': value_json.vod_id, " +
                    "'vod_position_s': value_json.vod_position_s} | tojson }}")
            addProperty("icon", "mdi:account-voice")
        })
        add("sensor", "title", entity("title", "Title").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ (value_json.title or '')[:250] }}")
            addProperty("icon", "mdi:format-title")
        })
        add("sensor", "category", entity("category", "Category").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.category or '' }}")
            addProperty("icon", "mdi:gamepad-variant")
        })
        add("sensor", "viewers", entity("viewers", "Viewers").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.viewers }}")
            addProperty("state_class", "measurement")
            addProperty("icon", "mdi:eye")
        })
        add("sensor", "uptime", entity("uptime", "Uptime").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.uptime_min if value_json.uptime_min is not none else 0 }}")
            addProperty("unit_of_measurement", "min")
            addProperty("device_class", "duration")
            addProperty("icon", "mdi:timer-outline")
        })
        group = AVATAR
        add("image", "avatar", entity("avatar", "Channel picture").apply {
            addProperty("url_topic", stateTopic)
            addProperty("url_template", "{{ value_json.avatar_url or '' }}")
        })
        group = PREVIEW
        add("image", "preview", entity("preview", "Stream preview").apply {
            addProperty("url_topic", stateTopic)
            addProperty("url_template", "{{ value_json.thumbnail_url or '' }}")
        })
        group = EVENTS
        add("event", "events", entity("events", "Events").apply {
            addProperty("state_topic", eventTopic)
            add("event_types", arr(*HubEvent.TYPES.toTypedArray()))
            addProperty("icon", "mdi:bell-ring-outline")
        })

        // Controls; removed again (empty config) when commands are switched off
        group = CONTROLS
        val buttons = listOf(
            "play_pause" to "Play/Pause", "live" to "Back to live", "next" to "Next channel",
            "previous" to "Previous channel", "close" to "Close player"
        )
        for ((id, name) in buttons) {
            add("button", id, entity(id, name).apply {
                addProperty("command_topic", commandTopic(id))
                addProperty("payload_press", "PRESS")
            })
        }
        add("switch", "chat", entity("chat", "Chat").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ 'ON' if value_json.chat_visible else 'OFF' }}")
            addProperty("command_topic", commandTopic("chat"))
            addProperty("icon", "mdi:chat")
        })
        add("select", "chat_layout", entity("chat_layout", "Chat layout").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.chat_layout or 'side_by_side' }}")
            addProperty("command_topic", commandTopic("chat_layout"))
            add("options", arr("side_by_side", "overlay", "transparent"))
        })
        add("select", "quality", entity("quality", "Quality").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.quality or 'Auto' }}")
            addProperty("command_topic", commandTopic("quality"))
            add("options", arr(*(qualities.ifEmpty { listOf("Auto") }).toTypedArray()))
        })
        add("number", "volume", entity("volume", "Stream volume").apply {
            addProperty("state_topic", stateTopic)
            addProperty("value_template", "{{ value_json.volume }}")
            addProperty("command_topic", commandTopic("volume"))
            addProperty("min", 0)
            addProperty("max", 100)
            addProperty("step", 5)
            addProperty("mode", "slider")
            addProperty("unit_of_measurement", "%")
        })
        add("text", "watch", entity("watch", "Watch channel").apply {
            addProperty("command_topic", commandTopic("watch"))
            addProperty("max", 25)
            addProperty("pattern", "^[A-Za-z0-9_]{1,25}$")
            addProperty("icon", "mdi:play-box-outline")
        })
        add("text", "chat_message", entity("chat_message", "Send chat message").apply {
            addProperty("command_topic", commandTopic("chat_message"))
            addProperty("max", 255)
            addProperty("icon", "mdi:send")
        })
        return list
    }

    private fun entity(objectId: String, name: String): JsonObject = JsonObject().apply {
        addProperty("name", name)
        addProperty("unique_id", "${nodeId}_$objectId")
        addProperty("availability_topic", availabilityTopic)
        add("device", JsonObject().apply {
            add("identifiers", arr(nodeId))
            addProperty("name", deviceName)
            addProperty("manufacturer", "ShrugTV")
            addProperty("model", model)
            addProperty("sw_version", swVersion)
        })
    }

    private fun arr(vararg values: String) = JsonArray().apply { values.forEach { add(it) } }

    companion object {
        const val INFO = "info"
        /** Channel picture (profile icon). */
        const val AVATAR = "avatar"
        /** Stream preview (snapshot of the stream, refreshed by Twitch every few minutes). */
        const val PREVIEW = "preview"
        const val EVENTS = "events"
        const val CONTROLS = "controls"
        val GROUPS = listOf(INFO, AVATAR, PREVIEW, EVENTS, CONTROLS)

        /** Topic-safe device id from the name in the settings ("Wohnzimmer TV" -> "wohnzimmer_tv"). */
        @JvmStatic
        fun deviceIdFrom(name: String?, fallbackHash: String): String {
            val slug = (name ?: "").lowercase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace(Regex("[^a-z0-9]+"), "_").trim('_').take(32)
            return slug.ifEmpty { "shrugtv_$fallbackHash" }
        }
    }
}
