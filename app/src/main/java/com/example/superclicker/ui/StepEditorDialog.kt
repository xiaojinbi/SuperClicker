package com.example.superclicker.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.example.superclicker.R
import com.example.superclicker.model.Point
import com.example.superclicker.model.Step

/**
 * 步骤编辑对话框。支持新建与编辑。
 * 坐标拾取通过 startActivityForResult 调用 CoordinatePickerActivity。
 */
class StepEditorDialog(
    private val context: Context,
    private val existing: Step?,
    private val onSave: (Step) -> Unit
) {
    private var currentType = Step.TYPE_CLICK
    private var pickedX: Int? = null
    private var pickedY: Int? = null
    private var etXRef: EditText? = null
    private var etYRef: EditText? = null

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_step, null)
        etXRef = view.findViewById(R.id.etX)
        etYRef = view.findViewById(R.id.etY)
        bindFields(view)
        setupSpinner(view)
        setupPickButton(view)

        val title = if (existing == null) "新增步骤" else "编辑步骤"
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val step = collectStep(view)
                        if (step != null) {
                            onSave(step)
                            dialog.dismiss()
                        }
                    }
                }
            }
            .show()
    }

    private fun bindFields(view: View) {
        val s = existing ?: return
        currentType = s.type
        view.findViewById<EditText>(R.id.etX).setText(s.x.toString())
        view.findViewById<EditText>(R.id.etY).setText(s.y.toString())
        view.findViewById<EditText>(R.id.etX2).setText(s.x2.toString())
        view.findViewById<EditText>(R.id.etY2).setText(s.y2.toString())
        view.findViewById<EditText>(R.id.etDuration).setText(s.durationMs.toString())
        view.findViewById<EditText>(R.id.etRepeat).setText(s.repeat.toString())
        view.findViewById<EditText>(R.id.etWait).setText(s.waitMs.toString())
        view.findViewById<EditText>(R.id.etNote).setText(s.note)
        view.findViewById<EditText>(R.id.etPoints).setText(
            s.points.joinToString(";") { "${it.x},${it.y}" }
        )
    }

    private fun setupSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerType)
        val adapter = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item, Step.ALL_TYPES.map { it.stepLabel() }
        )
        spinner.adapter = adapter
        val index = Step.ALL_TYPES.indexOf(currentType).coerceAtLeast(0)
        spinner.setSelection(index)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentType = Step.ALL_TYPES[pos]
                updateVisibility(view)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateVisibility(view)
    }

    private fun updateVisibility(view: View) {
        fun vis(id: Int, show: Boolean) {
            view.findViewById<View>(id).visibility = if (show) View.VISIBLE else View.GONE
        }
        vis(R.id.rowCoords, currentType in listOf(Step.TYPE_CLICK, Step.TYPE_SWIPE))
        vis(R.id.rowEndCoords, currentType == Step.TYPE_SWIPE)
        vis(R.id.rowDuration, currentType in listOf(Step.TYPE_CLICK, Step.TYPE_MULTI_CLICK, Step.TYPE_SWIPE))
        vis(R.id.etWait, currentType == Step.TYPE_WAIT)
        vis(R.id.etPoints, currentType == Step.TYPE_MULTI_CLICK)
    }

    private fun setupPickButton(view: View) {
        view.findViewById<Button>(R.id.btnPickCoord).setOnClickListener {
            val activity = context as? Activity ?: return@setOnClickListener
            val intent = Intent(context, CoordinatePickerActivity::class.java)
            activity.startActivityForResult(intent, REQUEST_PICK_COORD)
        }
    }

    /** 由 Activity 在 onActivityResult 中调用，把拾取结果回填到输入框 */
    fun onCoordinatePicked(x: Int, y: Int) {
        pickedX = x
        pickedY = y
        etXRef?.setText(x.toString())
        etYRef?.setText(y.toString())
    }

    fun collectStep(view: View): Step? {
        val step = existing ?: Step()
        step.type = currentType
        when (currentType) {
            Step.TYPE_CLICK, Step.TYPE_SWIPE -> {
                val x = view.findViewById<EditText>(R.id.etX).text.toString().toIntOrNull()
                val y = view.findViewById<EditText>(R.id.etY).text.toString().toIntOrNull()
                if (x == null || y == null) return null
                step.x = pickedX ?: x
                step.y = pickedY ?: y
                if (currentType == Step.TYPE_SWIPE) {
                    step.x2 = view.findViewById<EditText>(R.id.etX2).text.toString().toIntOrNull() ?: step.x
                    step.y2 = view.findViewById<EditText>(R.id.etY2).text.toString().toIntOrNull() ?: step.y
                }
                step.durationMs = view.findViewById<EditText>(R.id.etDuration).text.toString().toLongOrNull() ?: 100
                step.repeat = view.findViewById<EditText>(R.id.etRepeat).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
            }
            Step.TYPE_MULTI_CLICK -> {
                val text = view.findViewById<EditText>(R.id.etPoints).text.toString()
                val pts = parsePoints(text)
                if (pts.isEmpty()) return null
                step.points = pts
                step.durationMs = view.findViewById<EditText>(R.id.etDuration).text.toString().toLongOrNull() ?: 100
            }
            Step.TYPE_WAIT -> {
                step.waitMs = view.findViewById<EditText>(R.id.etWait).text.toString().toLongOrNull() ?: 1000
            }
        }
        step.note = view.findViewById<EditText>(R.id.etNote).text.toString()
        return step
    }

    private fun parsePoints(text: String): MutableList<Point> {
        val result = mutableListOf<Point>()
        text.split(";").forEach { seg ->
            val parts = seg.trim().split(",")
            if (parts.size == 2) {
                val x = parts[0].trim().toIntOrNull()
                val y = parts[1].trim().toIntOrNull()
                if (x != null && y != null) result.add(Point(x, y))
            }
        }
        return result
    }

    companion object {
        const val REQUEST_PICK_COORD = 1001
    }
}
