package com.example.todo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.data.TodoEntity
import com.example.todo.databinding.ItemIvyTaskBinding

class IvyPlannerAdapter(
    private val tasks: List<TodoEntity>,
    private val onSelectionChanged: (List<TodoEntity>) -> Unit
) : RecyclerView.Adapter<IvyPlannerAdapter.ViewHolder>() {

    private val selectedTasks = mutableListOf<TodoEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIvyTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = tasks.size

    fun getSelectedTasks() = selectedTasks.toList()

    inner class ViewHolder(private val binding: ItemIvyTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: TodoEntity) {
            binding.cbIvyTask.text = task.title
            binding.cbIvyTask.isChecked = selectedTasks.contains(task)

            binding.cbIvyTask.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (selectedTasks.size < 6) {
                        selectedTasks.add(task)
                    } else {
                        binding.cbIvyTask.isChecked = false
                        // Show error message
                    }
                } else {
                    selectedTasks.remove(task)
                }
                onSelectionChanged(selectedTasks)
            }
        }
    }
}
