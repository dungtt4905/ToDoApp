package com.example.todo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo.data.AppDatabase
import com.example.todo.data.TodoRepository
import com.example.todo.databinding.ActivityCalendarBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var viewModel: CalendarViewModel
    private lateinit var adapter: TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.get(this)
        val repo = TodoRepository(db.todoDao())
        val factory = CalendarViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupCalendar()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { viewModel.toggleDone(it) },
            onEdit = { todo ->
                // Return to MainActivity with edit intent
                val intent = Intent(this, com.example.todo.MainActivity::class.java)
                intent.putExtra("editTodoId", todo.id)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            },
            onDelete = { viewModel.deleteTodo(it) },
            onPomodoro = { todo ->
                val intent = Intent(this, PomodoroActivity::class.java)
                intent.putExtra("taskId", todo.id)
                startActivity(intent)
            }
        )
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTasks.adapter = adapter
    }

    private fun setupCalendar() {
        // Load today's tasks by default
        val today = Calendar.getInstance()
        viewModel.selectDate(today)
        
        // Set CalendarView to today
        binding.calendarView.date = today.timeInMillis

        // Handle date selection
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            viewModel.selectDate(selectedCalendar)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Update tasks list
                adapter.submitList(state.tasksForSelectedDate)
                
                // Update selected date text
                if (state.selectedDate != null) {
                    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                    binding.tvSelectedDate.text = dateFormat.format(state.selectedDate.time)
                    
                    if (state.tasksForSelectedDate.isEmpty()) {
                        binding.tvNoTasks.visibility = View.VISIBLE
                        binding.recyclerViewTasks.visibility = View.GONE
                    } else {
                        binding.tvNoTasks.visibility = View.GONE
                        binding.recyclerViewTasks.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvSelectedDate.text = getString(com.example.todo.R.string.select_date_prompt)
                    binding.tvNoTasks.visibility = View.GONE
                    binding.recyclerViewTasks.visibility = View.GONE
                }
            }
        }
    }
}
