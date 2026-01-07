package com.example.todo.ui

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.R
import com.example.todo.data.TodoEntity
import com.example.todo.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val onToggle: (TodoEntity) -> Unit,
    private val onEdit: (TodoEntity) -> Unit,
    private val onDelete: (TodoEntity) -> Unit,
    private val onPomodoro: (TodoEntity) -> Unit
) : ListAdapter<TodoEntity, TodoAdapter.TodoViewHolder>(DiffCallback) {

    inner class TodoViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TodoEntity) {
            val context = binding.root.context
            
            // --- Title Styling ---
            binding.tvTitle?.text = item.title
            binding.tvTitle?.let { tv ->
                if (item.isDone) {
                    tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tv.alpha = 0.5f // Fade out done tasks
                    tv.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                } else {
                    tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tv.alpha = 1.0f
                    tv.setTextColor(Color.BLACK)
                }
            }

            // --- Note Styling ---
            if (item.note.isNotBlank()) {
                binding.tvNote?.visibility = View.VISIBLE
                binding.tvNote?.text = item.note
                // Fade out note if done
                binding.tvNote?.alpha = if (item.isDone) 0.5f else 1.0f
            } else {
                binding.tvNote?.visibility = View.GONE
            }

            // --- Meta Information ---
            val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val dueText = item.dueAt?.let { fmt.format(Date(it)) } ?: "No due date"
            
            // Priority & Tag formatting
            val priorityLabel = item.priority.name.lowercase().replaceFirstChar { it.uppercase() }
            val tagLabel = item.tag.name.replace("_", " ").lowercase().split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            
            val repeatIcon = if (item.isRepeat) "🔁 " else ""
            
            binding.tvMeta?.text = "$repeatIcon$dueText • $priorityLabel • $tagLabel"

            // --- Overdue Logic & Visuals ---
            val now = System.currentTimeMillis()
            val isOverdue = item.dueAt != null && item.dueAt < now && !item.isDone
            
            // Show/Hide Overdue Label
            binding.tvOverdueLabel?.visibility = if (isOverdue) View.VISIBLE else View.GONE
            
            // Card Styling based on state
            if (binding.root is com.google.android.material.card.MaterialCardView) {
                val card = binding.root as com.google.android.material.card.MaterialCardView
                
                if (isOverdue) {
                    // Overdue: Red stroke, light red background
                    card.strokeWidth = 3
                    card.strokeColor = ContextCompat.getColor(context, R.color.color_overdue)
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_overdue_bg))
                } else if (item.isDone) {
                    // Done: No stroke, slightly gray background (optional, or just white with alpha content)
                    card.strokeWidth = 0
                    card.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white)) 
                    // Consider a separate color for done state if desired, keeping white for now
                } else {
                    // Normal: No stroke, white background
                    card.strokeWidth = 0
                    card.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }

            // Text Color for Meta (Due Date)
            if (isOverdue) {
                binding.tvMeta?.setTextColor(ContextCompat.getColor(context, R.color.color_overdue))
            } else {
                binding.tvMeta?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            
            // Content Alpha (Dim entire card content if done)
            binding.contentLayout?.alpha = if (item.isDone) 0.5f else 1.0f

            // --- Listeners ---
            // Unset listener first to prevent recycling issues
            binding.cbDone?.setOnCheckedChangeListener(null)
            binding.cbDone?.isChecked = item.isDone
            binding.cbDone?.setOnCheckedChangeListener { _, _ -> onToggle(item) }

            // Using localized strings for buttons if possible, but buttons are currently hardcoded in XML or unused
            // binding.btnPomodoro?.text = context.getString(R.string.btn_pomodoro) // If binding button text dynamically
            
            binding.btnEdit?.setOnClickListener { onEdit(item) }
            binding.btnDelete?.setOnClickListener { onDelete(item) }
            binding.btnPomodoro?.setOnClickListener { onPomodoro(item) }
            binding.root.setOnClickListener { onEdit(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TodoEntity>() {
        override fun areItemsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoEntity, newItem: TodoEntity): Boolean {
            return oldItem == newItem
        }
    }
}
