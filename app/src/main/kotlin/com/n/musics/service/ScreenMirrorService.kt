package com.n.musics.service

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenMirrorService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onCreate() {
        super.onCreate()
        // Screen ki dimensions nikalo
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = windowManager.currentWindowMetrics
            screenWidth = display.bounds.width()
            screenHeight = display.bounds.height()
            screenDensity = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Jab Activity se MediaProjection ka Intent aayega, tab yeh setup hoga
        // (Iska code hum MainActivity me add karenge)
        return START_STICKY
    }

    fun setupMediaProjection(projection: MediaProjection) {
        mediaProjection = projection
        
        // ImageReader setup karo jo screen ke frames capture karega
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        // VirtualDisplay banayein jo screen ko ImageReader me redirect karega
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "N_musics_Screen_Mirror",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        // Frames capture karna shuru karo
        startCapturing()
    }

    private fun startCapturing() {
        val handler = Handler(Looper.getMainLooper())
        
        // Har 100ms (10 FPS) me screen ka screenshot lo
        val captureRunnable = object : Runnable {
            override fun run() {
                captureScreen()
                handler.postDelayed(this, 100) // 100ms delay = 10 FPS
            }
        }
        handler.post(captureRunnable)
    }

    private fun captureScreen() {
        val image = imageReader?.acquireLatestImage()
        if (image != null) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            // Image ko Bitmap me convert karo
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // Yahan par hum is Bitmap ko compress karke parent ko bhejenge
            // (Network code baad me add karenge)
            // sendBitmapToParent(bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}