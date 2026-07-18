package com.n.musics.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AppBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockerService"
        var isBlockingEnabled = false
        val blockedApps = mutableListOf<String>()
    }

    override fun onServiceConnected() {
        Log.d(TAG, "✅ App Blocker Service Connected!")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        serviceInfo = info
        isBlockingEnabled = true

        Toast.makeText(this, "App Blocker Active", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            if (packageName != null && isBlockingEnabled && packageName in blockedApps) {
                Log.d(TAG, "🚫 Blocking app: $packageName")
                blockCurrentApp(packageName)
            }
        }
    }

    private fun blockCurrentApp(packageName: String) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        Toast.makeText(this, "This app is blocked by parent", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {
        Log.d(TAG, "️ App Blocker Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 App Blocker Service Destroyed")
        isBlockingEnabled = false
    }
}