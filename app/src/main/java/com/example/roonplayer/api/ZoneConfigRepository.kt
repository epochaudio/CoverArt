package com.example.roonplayer.api

import android.content.SharedPreferences
import org.json.JSONObject

class ZoneConfigRepository(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val OUTPUT_ID_KEY = "roon_output_id"
        const val ZONE_CONFIG_KEY = "configured_zone"
    }

    fun saveZoneConfiguration(zoneId: String) {
        sharedPreferences.edit()
            .putString(ZONE_CONFIG_KEY, zoneId)
            .apply()
    }

    fun saveOutputId(outputId: String) {
        sharedPreferences.edit()
            .putString(OUTPUT_ID_KEY, outputId)
            .apply()
    }

    fun getStoredOutputId(hostInput: String): String? {
        return sharedPreferences.getString(OUTPUT_ID_KEY, null)
            ?: if (hostInput.isNotBlank()) {
                sharedPreferences.getString("roon_zone_id_$hostInput", null)
            } else {
                null
            }
    }

    fun loadZoneConfiguration(
        hostInput: String,
        findZoneIdByOutputId: (String) -> String?
    ): String? {
        val existingZone = sharedPreferences.getString(ZONE_CONFIG_KEY, null)
        if (existingZone != null) {
            return existingZone
        }

        val legacyCoreId = if (hostInput.isBlank()) {
            null
        } else {
            sharedPreferences.getString("roon_core_id_$hostInput", null)
        }
        val legacyCoreKey = legacyCoreId?.let { "configured_zone_$it" }
        val legacyZone = legacyCoreKey?.let { sharedPreferences.getString(it, null) }
        if (legacyZone != null) {
            sharedPreferences.edit()
                .putString(ZONE_CONFIG_KEY, legacyZone)
                .remove(legacyCoreKey)
                .apply()
            return legacyZone
        }

        val legacyOutput = getStoredOutputId(hostInput)
        if (legacyOutput != null) {
            val mappedZoneId = findZoneIdByOutputId(legacyOutput)
            if (mappedZoneId != null) {
                saveZoneConfiguration(mappedZoneId)
                return mappedZoneId
            }
        }

        return null
    }

    fun findZoneIdByOutputId(
        outputId: String,
        zones: Map<String, JSONObject>
    ): String? {
        for ((zoneId, zone) in zones) {
            val outputs = zone.optJSONArray("outputs")
            if (outputs != null) {
                for (i in 0 until outputs.length()) {
                    val output = outputs.getJSONObject(i)
                    if (output.optString("output_id") == outputId) {
                        return zoneId
                    }
                }
            }
        }
        return null
    }
}
