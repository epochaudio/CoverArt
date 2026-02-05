package com.example.roonplayer.domain

import org.json.JSONObject

data class ZoneSelectionDecision(
    val zoneId: String?,
    val reason: String,
    val persist: Boolean,
    val statusMessage: String? = null
)

class ZoneSelectionUseCase {

    fun selectZone(
        availableZones: Map<String, JSONObject>,
        storedZoneId: String?,
        currentZoneId: String?
    ): ZoneSelectionDecision {
        if (availableZones.isEmpty()) {
            return ZoneSelectionDecision(
                zoneId = null,
                reason = "无可用区域",
                persist = false
            )
        }

        if (storedZoneId != null && availableZones.containsKey(storedZoneId)) {
            return ZoneSelectionDecision(
                zoneId = storedZoneId,
                reason = "存储配置",
                persist = false
            )
        }

        if (storedZoneId != null && !availableZones.containsKey(storedZoneId)) {
            val fallbackZoneId = autoSelectZoneId(availableZones)
            return ZoneSelectionDecision(
                zoneId = fallbackZoneId,
                reason = "配置失效回退",
                persist = false,
                statusMessage = "⚠️ 配置的Zone不可用，正在回退到可用区域"
            )
        }

        if (currentZoneId != null && availableZones.containsKey(currentZoneId)) {
            return ZoneSelectionDecision(
                zoneId = currentZoneId,
                reason = "当前选择",
                persist = false
            )
        }

        val autoZoneId = autoSelectZoneId(availableZones)
        return ZoneSelectionDecision(
            zoneId = autoZoneId,
            reason = "自动选择",
            persist = true
        )
    }

    private fun autoSelectZoneId(availableZones: Map<String, JSONObject>): String? {
        for ((zoneId, zone) in availableZones) {
            val state = zone.optString("state", "")
            val nowPlaying = zone.optJSONObject("now_playing")
            if (state == "playing" && nowPlaying != null) {
                return zoneId
            }
        }

        for ((zoneId, zone) in availableZones) {
            val nowPlaying = zone.optJSONObject("now_playing")
            if (nowPlaying != null) {
                return zoneId
            }
        }

        return availableZones.keys.firstOrNull()
    }
}
