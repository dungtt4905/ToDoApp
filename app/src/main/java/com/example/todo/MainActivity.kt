package com.example.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo.data.AppDatabase
import com.example.todo.data.EisenhowerTag
import com.example.todo.data.Priority
import com.example.todo.data.TodoEntity
import com.example.todo.data.TodoRepository
import com.example.todo.databinding.ActivityMainBinding
import com.example.todo.databinding.DialogPlanTomorrowBinding
import com.example.todo.databinding.DialogTodoEditorBinding
import com.example.todo.notification.NotificationScheduler
import com.example.todo.ui.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var ivyAdapter: TodoAdapter

    // UI State for Sort
    private var isSortAscending = false // Default Descending (Newest first)
    private var currentSortFieldStr = "" // Initialized in onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.get(this)
        val repo = TodoRepository(db.todoDao())
        val factory = TodoViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Handle edit intent from CalendarActivity
        handleEditIntent()
    }

    private fun handleEditIntent() {
        val editTodoId = intent.getLongExtra("editTodoId", -1L)
        if (editTodoId != -1L) {
            lifecycleScope.launch {
                val todo = viewModel.getTodoById(editTodoId)
                if (todo != null) {
                    showEditorDialog(todo)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { viewModel.toggleDone(it) },
            onEdit = { showEditorDialog(it) },
            onDelete = { viewModel.delete(it) },
            onPomodoro = { todo ->
                val intent = Intent(this, PomodoroActivity::class.java)
                intent.putExtra("taskId", todo.id)
                startActivity(intent)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        ivyAdapter = TodoAdapter(
            onToggle = { viewModel.toggleDone(it) },
            onEdit = { showEditorDialog(it) },
            onDelete = { viewModel.delete(it) },
            onPomodoro = { todo ->
                val intent = Intent(this, PomodoroActivity::class.java)
                intent.putExtra("taskId", todo.id)
                startActivity(intent)
            }
        )
        binding.ivyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.ivyRecyclerView.adapter = ivyAdapter
    }

    private fun setupListeners() {
        binding.btnPlanTomorrow.setOnClickListener {
            showIvyPlannerDialog()
        }

        binding.btnViewTomorrowPlan.setOnClickListener {
            showTomorrowPlanDialog()
        }

        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Search
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setQuery(text.toString())
        }

        // Filter & Sort Button
        binding.btnFilterSort.setOnClickListener {
            showFilterSortBottomSheet()
        }

        // Eisenhower Matrix Cards
        binding.cardDoNow.setOnClickListener { viewModel.setGroup(TodoGroup.DO_NOW) }
        binding.cardSchedule.setOnClickListener { viewModel.setGroup(TodoGroup.SCHEDULE) }
        binding.cardDelegate.setOnClickListener { viewModel.setGroup(TodoGroup.DELEGATE) }
        binding.cardEliminate.setOnClickListener { viewModel.setGroup(TodoGroup.ELIMINATE) }
        binding.btnAll.setOnClickListener { viewModel.setGroup(TodoGroup.ALL) }
        binding.btnUpcoming.setOnClickListener { viewModel.setGroup(TodoGroup.UPCOMING) }

        // FAB Add
        binding.fabAdd.setOnClickListener {
            showEditorDialog(null)
        }
    }
    
    private fun showFilterSortBottomSheet() {
        val currentFilter = when (viewModel.state.value.filter) {
            Filter.ALL -> getString(R.string.filter_all)
            Filter.ACTIVE -> getString(R.string.filter_active)
            Filter.DONE -> getString(R.string.filter_done)
        }
        
        val bottomSheet = FilterSortBottomSheet(
            currentFilter = currentFilter,
            currentSortField = currentSortFieldStr,
            isSortAscending = isSortAscending
        ) { filter, sortField, isAscending ->
            // Update filter
            val filterEnum = when (filter) {
                getString(R.string.filter_all) -> Filter.ALL
                getString(R.string.filter_active) -> Filter.ACTIVE
                getString(R.string.filter_done) -> Filter.DONE
                else -> Filter.ALL
            }
            viewModel.setFilter(filterEnum)
            
            // Update sort
            currentSortFieldStr = sortField
            isSortAscending = isAscending
            updateSort()
        }
        
        bottomSheet.show(supportFragmentManager, "FilterSortBottomSheet")
    }
    
    private fun updateSort() {
        val sortEnum = when (currentSortFieldStr) {
            getString(R.string.sort_field_created) -> {
                if (isSortAscending) Sort.CREATED_ASC else Sort.CREATED_DESC
            }
            getString(R.string.sort_field_due) -> {
                if (isSortAscending) Sort.DUE_ASC else Sort.DUE_DESC
            }
            getString(R.string.sort_field_priority) -> {
                // Descending = High to Low (High Priority first)
                if (isSortAscending) Sort.PRIORITY_ASC else Sort.PRIORITY_DESC
            }
            else -> Sort.CREATED_DESC
        }
        viewModel.setSort(sortEnum)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                // Animate list updates
                adapter.submitList(state.items) {
                    binding.recyclerView.scheduleLayoutAnimation()
                }
                
                if (state.ivyTasks.isNotEmpty()) {
                    binding.ivyCard.visibility = View.VISIBLE
                    ivyAdapter.submitList(state.ivyTasks)
                } else {
                    binding.ivyCard.visibility = View.GONE
                }
                
                // Show/hide empty state with fade animation
                val isEmpty = state.items.isEmpty() && state.ivyTasks.isEmpty()
                if (isEmpty) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.emptyState.alpha = 0f
                    binding.emptyState.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                } else {
                    binding.emptyState.visibility = View.GONE
                }
                
                // Update header title based on group
                val groupTitle = when (state.group) {
                    TodoGroup.ALL -> "All Tasks"
                    TodoGroup.UPCOMING -> "Upcoming & Overdue"
                    TodoGroup.DO_NOW -> "Do Now"
                    TodoGroup.SCHEDULE -> "Schedule"
                    TodoGroup.DELEGATE -> "Delegate"
                    TodoGroup.ELIMINATE -> "Eliminate"
                }
                binding.tvListHeader.text = groupTitle
                
                // Update task counts in matrix cards with animation
                val allItems = state.items
                val doNowCount = allItems.count { it.tag == EisenhowerTag.DO_NOW }
                val scheduleCount = allItems.count { it.tag == EisenhowerTag.SCHEDULE }
                val delegateCount = allItems.count { it.tag == EisenhowerTag.DELEGATE }
                val eliminateCount = allItems.count { it.tag == EisenhowerTag.ELIMINATE }
                
                animateCountChange(binding.tvCountDoNow, "$doNowCount tasks")
                animateCountChange(binding.tvCountSchedule, "$scheduleCount tasks")
                animateCountChange(binding.tvCountDelegate, "$delegateCount tasks")
                animateCountChange(binding.tvCountEliminate, "$eliminateCount tasks")

                // Schedule notifications
                state.items.forEach { todo ->
                    if (todo.dueAt != null && !todo.isDone) {
                        NotificationScheduler.schedule(this@MainActivity, todo)
                    } else {
                        NotificationScheduler.cancel(this@MainActivity, todo)
                    }
                }
            }
        }
    }
    
    private fun animateCountChange(textView: TextView, newText: String) {
        if (textView.text != newText) {
            textView.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction {
                    textView.text = newText
                    textView.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    private fun showIvyPlannerDialog() {
        val dialogBinding = DialogPlanTomorrowBinding.inflate(LayoutInflater.from(this))
        val tasks = viewModel.state.value.items.filter { !it.isDone }
        val plannerAdapter = IvyPlannerAdapter(tasks) { selectedTasks ->
            if (selectedTasks.size > 6) {
                Toast.makeText(this, R.string.ivy_max_6_tasks, Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogBinding.ivyPlanRecyclerView.layoutManager = LinearLayoutManager(this)
        dialogBinding.ivyPlanRecyclerView.adapter = plannerAdapter

        AlertDialog.Builder(this)
            .setTitle(R.string.ivy_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val selected = plannerAdapter.getSelectedTasks()
                if (selected.size <= 6) {
                    viewModel.saveIvyPlan(selected)
                } else {
                    Toast.makeText(this, R.string.ivy_max_6_tasks, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditorDialog(todo: TodoEntity?) {
        val dialogBinding = DialogTodoEditorBinding.inflate(LayoutInflater.from(this))
        val isCreatingNew = todo == null

        // Init values
        dialogBinding.etTitle.setText(todo?.title ?: "")
        dialogBinding.etNote.setText(todo?.note ?: "")
        
        // Spinners
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Priority.values())
        dialogBinding.spinnerPriority.adapter = priorityAdapter
        val initialPriority = todo?.priority ?: Priority.MEDIUM
        dialogBinding.spinnerPriority.setSelection(Priority.values().indexOf(initialPriority))

        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, EisenhowerTag.values())
        dialogBinding.spinnerTag.adapter = tagAdapter
        val initialTag = todo?.tag ?: EisenhowerTag.DO_NOW
        dialogBinding.spinnerTag.setSelection(EisenhowerTag.values().indexOf(initialTag))
        
        // Due Date Logic
        var currentDueAt: Long? = todo?.dueAt
        fun updateDateText() {
            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dialogBinding.tvDueDate.text = currentDueAt?.let { "Due: ${fmt.format(Date(it))}" } ?: "Due Date: None"
            dialogBinding.btnClearDate.visibility = if (currentDueAt != null) View.VISIBLE else View.GONE
        }
        updateDateText()

        dialogBinding.btnPickDate.setOnClickListener {
            showDateTimePicker(currentDueAt) { selectedTime ->
                currentDueAt = selectedTime
                updateDateText()
            }
        }

        dialogBinding.btnClearDate.setOnClickListener {
            currentDueAt = null
            updateDateText()
        }
        
        // Show Dialog
        val title = if (isCreatingNew) "Add Task" else "Edit Task"
        dialogBinding.tvDialogTitle.text = title

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val titleInput = dialogBinding.etTitle.text.toString().trim()
                val noteInput = dialogBinding.etNote.text.toString().trim()
                val priorityInput = dialogBinding.spinnerPriority.selectedItem as Priority
                val tagInput = dialogBinding.spinnerTag.selectedItem as EisenhowerTag
                
                if (titleInput.isNotBlank()) {
                    if (isCreatingNew) {
                        viewModel.add(
                            title = titleInput, 
                            note = noteInput, 
                            dueAt = currentDueAt, 
                            priority = priorityInput, 
                            tag = tagInput
                        )
                    } else {
                        viewModel.update(todo.copy(
                            title = titleInput,
                            note = noteInput,
                            dueAt = currentDueAt,
                            priority = priorityInput,
                            tag = tagInput
                        ))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePicker(initial: Long?, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.timeInMillis = initial

        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTomorrowPlanDialog() {
        val tomorrowPlan = viewModel.getTomorrowPlan()
        
        if (tomorrowPlan.isEmpty()) {
            Toast.makeText(this, R.string.ivy_no_tasks_planned, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogPlanTomorrowBinding.inflate(LayoutInflater.from(this))
        val planAdapter = TodoAdapter(
            onToggle = { todo ->
                viewModel.toggleDone(todo)
            },
            onEdit = { todo ->
                showEditorDialog(todo)
            },
            onDelete = { todo ->
                viewModel.removePlanTask(todo)
            },
            onPomodoro = { todo ->
                val intent = Intent(this, PomodoroActivity::class.java)
                intent.putExtra("taskId", todo.id)
                startActivity(intent)
            }
        )
        
        dialogBinding.ivyPlanRecyclerView.layoutManager = LinearLayoutManager(this)
        dialogBinding.ivyPlanRecyclerView.adapter = planAdapter
        planAdapter.submitList(tomorrowPlan)

        AlertDialog.Builder(this)
            .setTitle(R.string.ivy_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
