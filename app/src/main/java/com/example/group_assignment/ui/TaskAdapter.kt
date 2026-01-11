package com.example.group_assignment.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.group_assignment.R
import com.example.group_assignment.model.Task

/**
 * Enhanced Task Adapter with Category Support
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Show category badge for task organization
 * - Enhanced visual feedback for completed tasks
 * - Better UI/UX with proper spacing
 * - Strike-through effect for completed tasks
 */
class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskChecked: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTaskDate)
        val tvCategory: TextView = itemView.findViewById(R.id.tvTaskCategory)
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbTaskCompleted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Set task title
        holder.tvTitle.text = task.title

        // Set due date with better formatting
        holder.tvDate.text = "Due: ${task.dueDate}"

        // Set category
        holder.tvCategory.text = task.category

        // Handle completion checkbox
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = task.isCompleted

        // Apply strike-through effect for completed tasks
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.6f
            holder.tvDate.alpha = 0.6f
            holder.tvCategory.alpha = 0.6f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1.0f
            holder.tvDate.alpha = 1.0f
            holder.tvCategory.alpha = 1.0f
        }

        // Set checkbox listener
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            onTaskChecked(task.copy(isCompleted = isChecked))
        }

        // Set long click listener for delete
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(task)
            true
        }
    }

    override fun getItemCount() = tasks.size

    /**
     * Update the task list and refresh the RecyclerView
     */
    fun updateData(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    /**
     * Filter tasks based on search query
     * This is used in conjunction with the search functionality
     */
    fun filter(query: String, allTasks: List<Task>) {
        tasks = if (query.isEmpty()) {
            allTasks
        } else {
            allTasks.filter { task ->
                task.title.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}