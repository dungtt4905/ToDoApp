package com.example.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.EisenhowerTag
import com.example.todo.data.Priority
import com.example.todo.data.TodoEntity
import com.example.todo.data.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TodoState(
    val query: String = "",
    val filter: Filter = Filter.ALL,
    val sort: Sort = Sort.CREATED_DESC,
    val group: TodoGroup = TodoGroup.ALL,
    val items: List<TodoEntity> = emptyList(),
    val ivyTasks: List<TodoEntity> = emptyList()
)

// Helper class to group filter parameters and avoid 'combine' 5-argument limit
private data class FilterParams(
    val query: String,
    val filter: Filter,
    val sort: Sort,
    val group: TodoGroup
)

class TodoViewModel(private val repo: TodoRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(Filter.ALL)
    private val sort = MutableStateFlow(Sort.CREATED_DESC)
    private val group = MutableStateFlow(TodoGroup.ALL)

    private val _ivyTasks = MutableStateFlow<List<TodoEntity>>(emptyList())
    val ivyTasks = _ivyTasks.asStateFlow()

    // Combine filter flows first
    private val paramsFlow = combine(query, filter, sort, group) { q, f, s, g ->
        FilterParams(q, f, s, g)
    }

    // Now combine repository data and params
    val state: StateFlow<TodoState> =
        combine(repo.observeAll(), paramsFlow, _ivyTasks) { list, params, ivyList ->
            val (q, f, s, g) = params
            val now = System.currentTimeMillis()
            
            val filtered = list
                .asSequence()
                .filter { item ->
                    when (f) {
                        Filter.ALL -> true
                        Filter.ACTIVE -> !item.isDone
                        Filter.DONE -> item.isDone
                    }
                }
                .filter { item ->
                    if (q.isBlank()) true
                    else item.title.contains(q, ignoreCase = true) || item.note.contains(q, ignoreCase = true)
                }
                .filter { item ->
                    when (g) {
                        TodoGroup.ALL -> true
                        TodoGroup.UPCOMING -> {
                            val due = item.dueAt
                            if (due == null) false
                            else {
                                val limit = now + (3L * 24 * 60 * 60 * 1000)
                                due <= limit
                            }
                        }
                        TodoGroup.DO_NOW -> item.tag == EisenhowerTag.DO_NOW
                        TodoGroup.SCHEDULE -> item.tag == EisenhowerTag.SCHEDULE
                        TodoGroup.DELEGATE -> item.tag == EisenhowerTag.DELEGATE
                        TodoGroup.ELIMINATE -> item.tag == EisenhowerTag.ELIMINATE
                    }
                }
                .toList()

            val sorted = when (s) {
                // Sắp xếp
                Sort.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                Sort.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                
                Sort.DUE_ASC -> filtered.sortedWith(
                    compareBy<TodoEntity> { it.isDone }
                        .thenBy { it.dueAt == null }
                        .thenBy { it.dueAt ?: Long.MAX_VALUE }
                        .thenByDescending { it.createdAt }
                )
                Sort.DUE_DESC -> filtered.sortedWith(
                    compareBy<TodoEntity> { it.isDone }
                        .thenBy { it.dueAt == null }
                        .thenByDescending { it.dueAt ?: Long.MIN_VALUE }
                        .thenByDescending { it.createdAt }
                )
                
                Sort.PRIORITY_DESC -> filtered.sortedWith(
                    compareByDescending<TodoEntity> { it.priority.ordinal }
                        .thenByDescending { it.createdAt }
                )
                Sort.PRIORITY_ASC -> filtered.sortedWith(
                    compareBy<TodoEntity> { it.priority.ordinal }
                        .thenByDescending { it.createdAt }
                )
            }

            TodoState(query = q, filter = f, sort = s, group = g, items = sorted, ivyTasks = ivyList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoState())

    init {
        loadIvyTasksForToday()
    }

    fun loadIvyTasksForToday() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            _ivyTasks.value = repo.getIvyTasksForDate(today)
        }
    }

    fun saveIvyPlan(tasks: List<TodoEntity>) {
        viewModelScope.launch {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrow.time)
            
            // Get existing plan for tomorrow
            val existingPlan = repo.getIvyTasksForDate(tomorrowDate).toMutableList()
            
            // Add new tasks to the plan (no duplicates)
            tasks.forEach { newTask ->
                if (!existingPlan.any { it.id == newTask.id }) {
                    existingPlan.add(newTask)
                }
            }
            
            // Remove tasks that are no longer in the selection
            existingPlan.forEach { existingTask ->
                if (!tasks.any { it.id == existingTask.id }) {
                    repo.update(existingTask.copy(ivyDate = null, ivyRank = null))
                }
            }
            
            // Update all selected tasks with ivy plan info
            existingPlan.forEachIndexed { index, task ->
                repo.update(task.copy(ivyDate = tomorrowDate, ivyRank = index + 1))
            }
            
            // Reload ivy tasks
            loadIvyTasksForToday()
        }
    }

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: Filter) { filter.value = f }
    fun setSort(s: Sort) { sort.value = s }
    fun setGroup(g: TodoGroup) { group.value = g }

    fun add(
        title: String, 
        note: String, 
        dueAt: Long?, 
        priority: Priority, 
        tag: EisenhowerTag
    ) {
        if (title.isBlank()) return
        viewModelScope.launch { 
            repo.add(title.trim(), note.trim(), dueAt, priority, tag) 
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch { 
            val newDoneState = !todo.isDone
            repo.update(todo.copy(isDone = newDoneState))
        }
    }

    fun update(todo: TodoEntity) {
        if (todo.title.isBlank()) return
        viewModelScope.launch { 
            repo.update(todo.copy(title = todo.title.trim(), note = todo.note.trim())) 
        }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch {
            repo.delete(todo)
        }
    }

    fun getTomorrowPlan(): List<TodoEntity> {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrow.time)
        return runBlocking { 
            repo.getIvyTasksForDate(tomorrowDate).sortedBy { it.ivyRank ?: 999 }
        }
    }

    fun removePlanTask(todo: TodoEntity) {
        viewModelScope.launch {
            repo.update(todo.copy(ivyDate = null, ivyRank = null))
        }
    }
}

class TodoViewModelFactory(private val repo: TodoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TodoViewModel(repo) as T
}
