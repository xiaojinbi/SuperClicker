package com.example.superclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.superclicker.model.Point
import com.example.superclicker.model.Rule
import com.example.superclicker.model.Step
import com.example.superclicker.model.stepLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 自动化执行引擎。
 * 通过 AccessibilityService.dispatchGesture 注入点击/滑动手势，
 * 逐条执行 Rule 中的 Step，支持循环与暂停。
 */
class ClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClickerService"

        @Volatile
        var instance: ClickerAccessibilityService? = null
            private set

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        private val _progress = MutableStateFlow("空闲")
        val progress: StateFlow<String> = _progress

        fun isServiceConnected(): Boolean = instance != null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var paused = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* 不需要监听事件 */ }

    override fun onInterrupt() {
        stop()
    }

    override fun onDestroy() {
        stop()
        instance = null
        super.onDestroy()
    }

    fun start(rule: Rule) {
        if (_running.value) return
        paused = false
        _running.value = true
        job = scope.launch {
            try {
                var loop = 0
                while (_running.value) {
                    loop++
                    _progress.value = "第 $loop 轮"
                    for ((index, step) in rule.steps.withIndex()) {
                        if (!_running.value) break
                        while (paused && _running.value) delay(100)
                        if (!step.enabled) continue
                        _progress.value = "第 $loop 轮 · 步骤 ${index + 1}/${rule.steps.size} · ${step.type.stepLabel()}"
                        executeStep(step)
                    }
                    if (rule.loopCount in 1..Int.MAX_VALUE && loop >= rule.loopCount) break
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e(TAG, "execute error", e)
            } finally {
                _running.value = false
                _progress.value = "已停止"
                job = null
            }
        }
    }

    fun stop() {
        _running.value = false
        job?.cancel()
        job = null
        paused = false
    }

    fun togglePause() {
        paused = !paused
        _progress.value = if (paused) "已暂停" else "继续执行"
    }

    private suspend fun executeStep(step: Step) {
        when (step.type) {
            Step.TYPE_CLICK -> {
                repeat(step.repeat.coerceAtLeast(1)) {
                    if (!_running.value) return@repeat
                    tap(step.x.toFloat(), step.y.toFloat(), step.durationMs)
                    if (step.intervalMs > 0) delay(step.intervalMs)
                }
            }
            Step.TYPE_MULTI_CLICK -> {
                if (step.points.isNotEmpty()) multiTap(step.points, step.durationMs)
            }
            Step.TYPE_SWIPE -> {
                swipe(
                    step.x.toFloat(), step.y.toFloat(),
                    step.x2.toFloat(), step.y2.toFloat(),
                    step.durationMs
                )
            }
            Step.TYPE_WAIT -> delay(step.waitMs.coerceAtLeast(0))
            Step.TYPE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Step.TYPE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /** 注入一次点击手势，并等待系统完成 */
    private suspend fun tap(x: Float, y: Float, duration: Long) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchAndWait(gesture)
    }

    /** 注入多点同时点击 */
    private suspend fun multiTap(points: List<Point>, duration: Long) {
        val builder = GestureDescription.Builder()
        points.forEach { p ->
            val path = Path().apply { moveTo(p.x.toFloat(), p.y.toFloat()) }
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        }
        dispatchAndWait(builder.build())
    }

    /** 注入滑动手势 */
    private suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchAndWait(gesture)
    }

    private suspend fun dispatchAndWait(gesture: GestureDescription) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        suspendCoroutine<Unit> { cont ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(Unit)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(Unit)
                }
            }
            val ok = dispatchGesture(gesture, callback, null)
            if (!ok) cont.resume(Unit)
        }
    }
}
