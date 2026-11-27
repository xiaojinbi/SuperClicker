package com.example.superclicker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.superclicker.R
import com.example.superclicker.model.Step
import com.example.superclicker.model.stepLabel

class StepAdapter(
    private val steps: MutableList<Step>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<StepAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndex: TextView = view.findViewById(R.id.tvIndex)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDetail: TextView = view.findViewById(R.id.tvDetail)
        val cbEnabled: CheckBox = view.findViewById(R.id.cbEnabled)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val step = steps[position]
        holder.tvIndex.text = (position + 1).toString()
        holder.tvType.text = step.type.stepLabel()
        holder.tvDetail.text = describe(step)
        holder.cbEnabled.setOnCheckedChangeListener(null)
        holder.cbEnabled.isChecked = step.enabled
        holder.cbEnabled.setOnCheckedChangeListener { _, checked -> step.enabled = checked }
        holder.btnEdit.setOnClickListener { onEdit(position) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount(): Int = steps.size

    fun refresh() = notifyDataSetChanged()

    private fun describe(step: Step): String = when (step.type) {
        Step.TYPE_CLICK -> "x=${step.x} y=${step.y} · ${step.durationMs}ms · 连击${step.repeat}次"
        Step.TYPE_MULTI_CLICK -> "多点 ${step.points.size} 个：" +
                step.points.joinToString(" ") { "(${it.x},${it.y})" }
        Step.TYPE_SWIPE -> "(${step.x},${step.y}) → (${step.x2},${step.y2}) · ${step.durationMs}ms"
        Step.TYPE_WAIT -> "等待 ${step.waitMs}ms"
        Step.TYPE_BACK -> "返回键"
        Step.TYPE_HOME -> "Home 键"
        else -> ""
    }
}
