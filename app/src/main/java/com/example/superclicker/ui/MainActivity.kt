package com.example.superclicker.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.superclicker.R
import com.example.superclicker.model.Rule
import com.example.superclicker.model.Step
import com.example.superclicker.service.ClickerAccessibilityService
import com.example.superclicker.service.FloatingControlService
import com.example.superclicker.util.RuleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rule: Rule
    private lateinit var adapter: StepAdapter
    private var editor: StepEditorDialog? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rule = RuleStore.load(this) ?: Rule()
        setupRecycler()
        setupButtons()
        observeState()
        checkPermissions()

        FloatingControlService.onStartRequested = { startExecution() }
    }

    private fun setupRecycler() {
        adapter = StepAdapter(
            rule.steps,
            onEdit = { index -> openEditor(index) },
            onDelete = { index ->
                rule.steps.removeAt(index)
                adapter.refresh()
                Toast.makeText(this, "已删除步骤", Toast.LENGTH_SHORT).show()
            }
        )
        findViewById<RecyclerView>(R.id.recyclerSteps).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = this@MainActivity.adapter
        }
        findViewById<EditText>(R.id.etLoop).setText(rule.loopCount.toString())
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btnPerm).setOnClickListener { checkPermissions() }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { openEditor(-1) }

        findViewById<Button>(R.id.btnStart).setOnClickListener { startExecution() }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            ClickerAccessibilityService.instance?.stop()
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            rule.loopCount = findViewById<EditText>(R.id.etLoop).text.toString().toIntOrNull() ?: 0
            val ok = RuleStore.save(this, rule)
            Toast.makeText(this, if (ok) "规则已保存" else "保存失败", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnLoad).setOnClickListener {
            RuleStore.load(this)?.let {
                rule = it
                rule.steps.clear()
                rule.steps.addAll(it.steps)
                adapter.refresh()
                findViewById<EditText>(R.id.etLoop).setText(rule.loopCount.toString())
                Toast.makeText(this, "规则已加载", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "没有保存的规则", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnFloat).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
            } else {
                FloatingControlService.start(this)
                Toast.makeText(this, "悬浮控制台已启动", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeState() {
        scope.launch {
            ClickerAccessibilityService.running.collectLatest { running ->
                findViewById<TextView>(R.id.tvStatus).text = if (running) "执行中" else "空闲"
            }
        }
        scope.launch {
            ClickerAccessibilityService.progress.collectLatest { text ->
                title = text
            }
        }
    }

    private fun startExecution() {
        if (!ClickerAccessibilityService.isServiceConnected()) {
            Toast.makeText(this, "请先开启无障碍权限", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }
        if (rule.steps.isEmpty()) {
            Toast.makeText(this, "请先添加步骤", Toast.LENGTH_SHORT).show()
            return
        }
        rule.loopCount = findViewById<EditText>(R.id.etLoop).text.toString().toIntOrNull() ?: 0
        RuleStore.save(this, rule)
        ClickerAccessibilityService.instance?.start(rule)
        Toast.makeText(this, "开始执行", Toast.LENGTH_SHORT).show()
    }

    private fun openEditor(index: Int) {
        val existing = if (index in rule.steps.indices) rule.steps[index] else null
        editor = StepEditorDialog(this, existing) { step ->
            if (index in rule.steps.indices) rule.steps[index] = step else rule.steps.add(step)
            adapter.refresh()
        }
        editor?.show()
    }

    private fun checkPermissions() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "无障碍权限未开启，无法执行点击", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "无障碍权限已开启", Toast.LENGTH_SHORT).show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = "$packageName/${ClickerAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StepEditorDialog.REQUEST_PICK_COORD && resultCode == Activity.RESULT_OK && data != null) {
            val x = data.getIntExtra(CoordinatePickerActivity.EXTRA_X, 0)
            val y = data.getIntExtra(CoordinatePickerActivity.EXTRA_Y, 0)
            editor?.onCoordinatePicked(x, y)
            Toast.makeText(this, "已拾取坐标 ($x, $y)，请确认保存", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
