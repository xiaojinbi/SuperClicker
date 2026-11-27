package com.example.superclicker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.superclicker.R

/** 全屏透明页：点击任意位置返回坐标（屏幕绝对坐标） */
class CoordinatePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinate_picker)

        findViewById<android.view.View>(R.id.pickerRoot).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val intent = Intent().apply {
                    putExtra(EXTRA_X, event.rawX.toInt())
                    putExtra(EXTRA_Y, event.rawY.toInt())
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
                true
            } else false
        }
    }

    companion object {
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
    }
}
