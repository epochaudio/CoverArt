package com.example.roonplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 简化版开机启动接收器
 * 直接启动MainActivity，无复杂后台服务
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting CoverArt application")
            
            try {
                // 直接启动MainActivity，不创建后台服务
                val startIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("started_from_boot", true)
                }
                
                context.startActivity(startIntent)
                Log.d(TAG, "MainActivity started successfully after boot")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MainActivity after boot: ${e.message}", e)
            }
        }
    }
}