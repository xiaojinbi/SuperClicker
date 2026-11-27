package com.example.superclicker.model

/**
 * 一个执行步骤。参数含义按 type 区分：
 * - CLICK     : x, y, durationMs, repeat(连击次数), intervalMs(连击间隔)
 * - MULTI_CLICK: points(多点坐标), durationMs
 * - SWIPE     : x, y 起点，x2, y2 终点，durationMs
 * - WAIT      : waitMs
 * - BACK/HOME : 无参数
 */
data class Step(
    var type: String = TYPE_CLICK,
    var x: Int = 500,
    var y: Int = 500,
    var x2: Int = 500,
    var y2: Int = 900,
    var durationMs: Long = 100,
    var intervalMs: Long = 200,
    var repeat: Int = 1,
    var waitMs: Long = 1000,
    var points: MutableList<Point> = mutableListOf(),
    var enabled: Boolean = true,
    var note: String = ""
) {
    companion object {
        const val TYPE_CLICK = "CLICK"
        const val TYPE_MULTI_CLICK = "MULTI_CLICK"
        const val TYPE_SWIPE = "SWIPE"
        const val TYPE_WAIT = "WAIT"
        const val TYPE_BACK = "BACK"
        const val TYPE_HOME = "HOME"

        val ALL_TYPES = listOf(TYPE_CLICK, TYPE_MULTI_CLICK, TYPE_SWIPE, TYPE_WAIT, TYPE_BACK, TYPE_HOME)
    }
}

data class Point(var x: Int = 0, var y: Int = 0)

data class Rule(
    var name: String = "默认规则",
    var steps: MutableList<Step> = mutableListOf(),
    var loopCount: Int = 0  // 0 = 无限循环
)

fun String.stepLabel(): String = when (this) {
    Step.TYPE_CLICK -> "单击"
    Step.TYPE_MULTI_CLICK -> "多点点击"
    Step.TYPE_SWIPE -> "滑动"
    Step.TYPE_WAIT -> "等待"
    Step.TYPE_BACK -> "返回键"
    Step.TYPE_HOME -> "Home 键"
    else -> this
}
