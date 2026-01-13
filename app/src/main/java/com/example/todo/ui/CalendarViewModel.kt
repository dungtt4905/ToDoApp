package com.example.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.TodoEntity
import com.example.todo.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarState(
    val selectedDate: Calendar? = null,
    val tasksForSelectedDate: List<TodoEntity> = emptyList(),
    val datesWithTasks: Set<String> = emptySet()
)

class CalendarViewModel(private val repo: TodoRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        loadDatesWithTasks()
    }

    private fun loadDatesWithTasks() {
        viewModelScope.launch {
            val dates = repo.getAllDatesWithTasks()
            _state.value = _state.value.copy(datesWithTasks = dates.toSet())
        }
    }

    fun selectDate(calendar: Calendar) {
        viewModelScope.launch {
            val startOfDay = getStartOfDay(calendar)
            val endOfDay = getEndOfDay(calendar)
            
            val tasks = repo.getTasksForDateRange(startOfDay, endOfDay)
            _state.value = _state.value.copy(
                selectedDate = calendar,
                tasksForSelectedDate = tasks
            )
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            val newDoneState = !todo.isDone
            repo.update(todo.copy(isDone = newDoneState))
            
            // Reload tasks for selected date
            _state.value.selectedDate?.let { selectDate(it) }
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repo.delete(todo)
            
            // Reload tasks and dates
            _state.value.selectedDate?.let { selectDate(it) }
            loadDatesWithTasks()
        }
    }

    private fun getStartOfDay(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

class CalendarViewModelFactory(private val repo: TodoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CalendarViewModel(repo) as T
}
