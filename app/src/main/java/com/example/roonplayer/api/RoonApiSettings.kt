package com.example.roonplayer.api

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * RoonApiSettings - 基于官方RoonCoverArt实现的Settings管理类
 * 参考：https://github.com/epochaudio/RoonCoverArt/blob/main/app.js
 */
class RoonApiSettings(
    private val getHostInput: () -> String,
    private val zoneConfigRepository: ZoneConfigRepository,
    private val onZoneConfigChanged: (String?) -> Unit,
    private val getAvailableZones: () -> Map<String, JSONObject>
) {
    companion object {
        private const val TAG = "RoonApiSettings"
    }
    
    private var currentSettings = mutableMapOf<String, Any>()
    
    init {
        // 加载保存的设置
        loadSavedSettings()
    }
    
    /**
     * 创建Settings布局 - 按照官方格式
     */
    private fun makeLayout(settings: Map<String, Any>): JSONObject {
        val hasValidZoneData = hasValidZoneData()
        
        return JSONObject().apply {
            put("values", JSONObject().apply {
                // 将设置值转换为JSON
                for ((key, value) in settings) {
                    put(key, value)
                }
            })
            put("layout", JSONArray().apply {
                // 只有在Zone数据可用时才显示zone选择器
                if (hasValidZoneData) {
                    put(JSONObject().apply {
                        put("type", "zone")
                        put("title", "选择播放区域")
                        put("setting", "output")
                    })
                } else {
                    // Zone数据不可用时显示提示信息
                    put(JSONObject().apply {
                        put("type", "label")
                        put("title", "正在加载Zone数据...")
                    })
                }
            })
            put("has_error", !hasValidZoneData)
        }
    }
    
    /**
     * 检查Zone数据是否有效
     */
    private fun hasValidZoneData(): Boolean {
        return try {
            val zones = getAvailableZones()
            val hasZones = zones.isNotEmpty()
            Log.d(TAG, "Zone data validation: hasZones=$hasZones, count=${zones.size}")
            hasZones
        } catch (e: Exception) {
            Log.e(TAG, "Error checking zone data: ${e.message}")
            false
        }
    }
    
    /**
     * 处理get_settings请求 - 官方API回调格式
     */
    fun getSettings(): JSONObject {
        val layout = makeLayout(currentSettings)
        Log.d(TAG, "Get settings: $layout")
        return layout
    }
    
    /**
     * 处理save_settings请求 - 官方API回调格式
     */
    fun saveSettings(newSettings: JSONObject, isDryRun: Boolean): JSONObject {
        Log.d(TAG, "Save settings - isDryRun: $isDryRun, values: $newSettings")
        
        val layout = makeLayout(convertJsonToMap(newSettings))
        
        if (!isDryRun && !layout.optBoolean("has_error", false)) {
            // 保存设置
            currentSettings = convertJsonToMap(newSettings)
            persistSettings()
            
            // 通知Zone配置变化
            val outputValue = newSettings.optJSONObject("output")
            val outputId = outputValue?.optString("output_id")
            if (outputId != null) {
                // 找到对应的zone_id并通知
                val zoneId = findZoneIdByOutputId(outputId)
                if (zoneId != null) {
                    Log.d(TAG, "Zone selected: $zoneId (from output: $outputId)")
                    onZoneConfigChanged(zoneId)
                } else {
                    Log.w(TAG, "Output selected but zone not available yet: $outputId")
                }
            }
        }
        
        // 修复：返回正确的Settings响应格式
        return JSONObject().apply {
            put("status", if (layout.optBoolean("has_error", false)) "Error" else "Success")
            put("settings", layout)
        }
    }
    
    /**
     * Settings服务注册信息
     */
    fun getServiceInfo(): JSONObject {
        return JSONObject().apply {
            put("get_settings", true)
            put("save_settings", true)
        }
    }
    
    /**
     * 处理Settings服务消息
     */
    fun handleSettingsMessage(message: JSONObject): JSONObject? {
        return try {
            Log.d(TAG, "=== Settings Message Received ===")
            Log.d(TAG, "Full message: $message")
            
            val request = message.optString("request")
            Log.d(TAG, "Request type: $request")
            
            when (request) {
                "get_settings" -> {
                    Log.d(TAG, "Processing get_settings request")
                    
                    // 检查Zone数据是否可用
                    if (!hasValidZoneData()) {
                        Log.w(TAG, "Zone data not available for get_settings")
                        // 仍然返回布局，但标记为错误状态
                    }
                    
                    val response = getSettings()
                    Log.d(TAG, "get_settings response: $response")
                    response
                }
                "save_settings" -> {
                    Log.d(TAG, "Processing save_settings request")
                    val body = message.optJSONObject("body")
                    val isDryRun = body?.optBoolean("dry_run", false) ?: false
                    val newSettings = body?.optJSONObject("settings") ?: JSONObject()
                    Log.d(TAG, "save_settings - dry_run: $isDryRun, settings: $newSettings")
                    
                    // 验证设置数据
                    if (!isDryRun && !hasValidZoneData()) {
                        Log.e(TAG, "Cannot save settings: Zone data not available")
                        return createErrorResponse("Zone data not available")
                    }
                    
                    val response = saveSettings(newSettings, isDryRun)
                    Log.d(TAG, "save_settings response: $response")
                    response
                }
                else -> {
                    Log.w(TAG, "Unknown settings request: $request")
                    Log.d(TAG, "Available keys in message: ${message.keys().asSequence().toList()}")
                    createErrorResponse("Unknown request type: $request")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling settings message: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTrace.joinToString("\n")}")
            createErrorResponse("Settings processing error: ${e.message}")
        }
    }
    
    /**
     * 创建错误响应
     */
    private fun createErrorResponse(errorMessage: String): JSONObject {
        return JSONObject().apply {
            put("status", "Error")
            put("error", errorMessage)
            put("settings", JSONObject().apply {
                put("values", JSONObject())
                put("layout", JSONArray())
                put("has_error", true)
            })
        }
    }
    
    /**
     * 加载保存的设置
     */
    private fun loadSavedSettings() {
        val hostInput = getHostInput().trim()
        val savedOutput = zoneConfigRepository.getStoredOutputId(hostInput)
        
        if (savedOutput != null) {
            // 创建output对象
            val outputObj = JSONObject().apply {
                put("output_id", savedOutput)
                put("display_name", "Saved Zone")
            }
            currentSettings["output"] = outputObj
            Log.d(TAG, "Loaded saved output: $savedOutput")

            // 统一回写主键，完成 legacy key 向 OUTPUT_ID_KEY 的收敛
            zoneConfigRepository.saveOutputId(savedOutput)
        }
    }
    
    /**
     * 保存设置到SharedPreferences
     */
    private fun persistSettings() {
        // 保存output设置
        val outputObj = currentSettings["output"] as? JSONObject
        val outputId = outputObj?.optString("output_id")
        if (outputId != null) {
            // 找到对应的zone_id
            val zoneId = findZoneIdByOutputId(outputId)
            if (zoneId != null) {
                zoneConfigRepository.saveZoneConfiguration(zoneId)
                Log.d(TAG, "Saved zone configuration: $zoneId")
            }

            // 保存output_id用于Settings界面回填
            zoneConfigRepository.saveOutputId(outputId)
            Log.d(TAG, "Saved output setting: $outputId")
        }
    }
    
    /**
     * 通过output_id查找zone_id
     */
    private fun findZoneIdByOutputId(outputId: String): String? {
        return zoneConfigRepository.findZoneIdByOutputId(outputId, getAvailableZones())
    }
    
    /**
     * 将JSONObject转换为Map
     */
    private fun convertJsonToMap(json: JSONObject): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            map[key] = value
        }
        return map
    }
    
    /**
     * 加载Zone配置
     */
    fun loadZoneConfiguration(): String? {
        val hostInput = getHostInput().trim()
        val zoneId = zoneConfigRepository.loadZoneConfiguration(
            hostInput = hostInput,
            findZoneIdByOutputId = ::findZoneIdByOutputId
        )

        Log.d(TAG, "Loaded zone configuration: zoneId=$zoneId")
        return zoneId
    }
    
    /**
     * 获取当前设置
     */
    fun getCurrentSettings(): Map<String, Any> {
        return currentSettings.toMap()
    }
}
