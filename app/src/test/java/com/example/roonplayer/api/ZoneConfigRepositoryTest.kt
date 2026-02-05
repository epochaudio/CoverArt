package com.example.roonplayer.api

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneConfigRepositoryTest {

    @Test
    fun `returns existing configured zone first`() {
        val prefs = FakeSharedPreferences(
            mutableMapOf(
                ZoneConfigRepository.ZONE_CONFIG_KEY to "zone_existing"
            )
        )
        val repo = ZoneConfigRepository(prefs)

        val zoneId = repo.loadZoneConfiguration(
            hostInput = "192.168.1.10:9330",
            findZoneIdByOutputId = { "should_not_be_called" }
        )

        assertEquals("zone_existing", zoneId)
    }

    @Test
    fun `migrates legacy core scoped zone config`() {
        val hostInput = "192.168.1.11:9330"
        val coreId = "core_123"
        val legacyKey = "configured_zone_$coreId"
        val prefs = FakeSharedPreferences(
            mutableMapOf(
                "roon_core_id_$hostInput" to coreId,
                legacyKey to "zone_legacy"
            )
        )
        val repo = ZoneConfigRepository(prefs)

        val zoneId = repo.loadZoneConfiguration(
            hostInput = hostInput,
            findZoneIdByOutputId = { null }
        )

        assertEquals("zone_legacy", zoneId)
        assertEquals("zone_legacy", prefs.getString(ZoneConfigRepository.ZONE_CONFIG_KEY, null))
        assertFalse(prefs.contains(legacyKey))
    }

    @Test
    fun `maps legacy output id to zone id and persists mapped zone`() {
        val hostInput = "192.168.1.12:9330"
        val prefs = FakeSharedPreferences(
            mutableMapOf(
                "roon_zone_id_$hostInput" to "output_abc"
            )
        )
        val repo = ZoneConfigRepository(prefs)

        val zoneId = repo.loadZoneConfiguration(
            hostInput = hostInput,
            findZoneIdByOutputId = { outputId ->
                if (outputId == "output_abc") "zone_from_output" else null
            }
        )

        assertEquals("zone_from_output", zoneId)
        assertEquals("zone_from_output", prefs.getString(ZoneConfigRepository.ZONE_CONFIG_KEY, null))
    }

    @Test
    fun `getStoredOutputId prefers canonical key over host scoped key`() {
        val hostInput = "192.168.1.13:9330"
        val prefs = FakeSharedPreferences(
            mutableMapOf(
                ZoneConfigRepository.OUTPUT_ID_KEY to "output_main",
                "roon_zone_id_$hostInput" to "output_legacy"
            )
        )
        val repo = ZoneConfigRepository(prefs)

        val outputId = repo.getStoredOutputId(hostInput)

        assertEquals("output_main", outputId)
    }

    @Test
    fun `findZoneIdByOutputId resolves zone from outputs array`() {
        val repo = ZoneConfigRepository(FakeSharedPreferences())

        val zones = mapOf(
            "zone_a" to zoneWithOutputs("output_a", "output_b"),
            "zone_b" to zoneWithOutputs("output_c")
        )

        assertEquals("zone_a", repo.findZoneIdByOutputId("output_b", zones))
        assertEquals("zone_b", repo.findZoneIdByOutputId("output_c", zones))
        assertNull(repo.findZoneIdByOutputId("missing_output", zones))
    }

    private fun zoneWithOutputs(vararg outputIds: String): JSONObject {
        val outputs = JSONArray()
        outputIds.forEach { outputId ->
            outputs.put(JSONObject().put("output_id", outputId))
        }
        return JSONObject().put("outputs", outputs)
    }

    private class FakeSharedPreferences(
        private val data: MutableMap<String, Any?> = mutableMapOf()
    ) : SharedPreferences {

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            if (key == null) return defValue
            return data[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            if (key == null) return defValues
            @Suppress("UNCHECKED_CAST")
            return (data[key] as? MutableSet<String>) ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            if (key == null) return defValue
            return data[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            if (key == null) return defValue
            return data[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            if (key == null) return defValue
            return data[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            if (key == null) return defValue
            return data[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean {
            if (key == null) return false
            return data.containsKey(key)
        }

        override fun edit(): SharedPreferences.Editor = EditorImpl(data)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // No-op for unit tests.
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // No-op for unit tests.
        }

        private class EditorImpl(
            private val data: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) pending[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = null
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    data.clear()
                    clearRequested = false
                }

                pending.forEach { (key, value) ->
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                }
                pending.clear()
            }
        }
    }
}
