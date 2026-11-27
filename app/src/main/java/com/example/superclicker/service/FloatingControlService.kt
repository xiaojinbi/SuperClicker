package com.example.superclicker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.superclicker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 悬浮控制台：可拖动的小面板，提供开始/停止/暂停按钮。
 * 需要 SYSTEM_ALERT_WINDOW 权限。
 */
class FloatingControlService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (canDrawOverlay()) addFloatingView()
        observeState()
    }

    private fun startForegroundCompat() {
        val channelId = "floating_control"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "悬浮控制台", android.app.NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(android.app.NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("超级点击器 Core")
                .setContentText("悬浮控制台运行中")
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("超级点击器 Core")
                .setContentText("悬浮控制台运行中")
                .build()
        }
        startForeground(1001, notification)
    }

    private fun canDrawOverlay(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)

    private fun addFloatingView() {
        if (floatingView != null) return
        val view = LayoutInflater.from(this).inflate(R.layout.view_floating_control, null)
        floatingView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(v, params)
                    true
                }
                else -> false
            }
        }

        view.findViewById<Button>(R.id.btnFloatStart).setOnClickListener {
            if (!ClickerAccessibilityService.isServiceConnected()) {
                view.findViewById<TextView>(R.id.tvFloatStatus).text = "请开启无障碍权限"
            } else {
                onStartRequested?.invoke()
            }
        }
        view.findViewById<Button>(R.id.btnFloatPause).setOnClickListener {
            ClickerAccessibilityService.instance?.togglePause()
        }
        view.findViewById<Button>(R.id.btnFloatStop).setOnClickListener {
            ClickerAccessibilityService.instance?.stop()
        }

        windowManager.addView(view, params)
    }

    private fun observeState() {
        scope.launch {
            ClickerAccessibilityService.progress.collectLatest { text ->
                floatingView?.findViewById<TextView>(R.id.tvFloatStatus)?.text = text
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        floatingView?.let { runCatching { windowManager.removeView(it) } }
        floatingView = null
        super.onDestroy()
    }

    companion object {
        var onStartRequested: (() -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, FloatingControlService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingControlService::class.java))
        }
    }
}
