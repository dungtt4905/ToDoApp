package com.example.todo.ui

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroPhase { FOCUS, BREAK, IDLE }

data class PomodoroUiState(
    val taskTitle: String = "",
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val remainingMillis: Long = 0,
    val currentSet: Int = 1,
    val totalSets: Int = 1,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val beepTrigger: Long = 0
)

class PomodoroViewModel(
    private val repository: TodoRepository,
    private val taskId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState = _uiState.asStateFlow()

    private var timer: CountDownTimer? = null
    private var focusDurationMillis: Long = 0
    private var breakDurationMillis: Long = 0

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val todo = repository.getById(taskId)
            _uiState.update { it.copy(taskTitle = todo?.title ?: "Unknown Task") }
        }
    }

    fun start(focusMin: Int, breakMin: Int, sets: Int) {
        focusDurationMillis = focusMin * 60 * 1000L
        breakDurationMillis = breakMin * 60 * 1000L
        
        _uiState.update {
            it.copy(
                phase = PomodoroPhase.FOCUS,
                remainingMillis = focusDurationMillis,
                currentSet = 1,
                totalSets = sets,
                isRunning = true,
                isPaused = false
            )
        }
        startTimer(focusDurationMillis)
    }

    private fun startTimer(millisInFuture: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _uiState.update { it.copy(remainingMillis = millisUntilFinished) }
            }

            override fun onFinish() {
                handlePhaseFinish()
            }
        }.start()
    }

    private fun handlePhaseFinish() {
        // Trigger beep
        _uiState.update { it.copy(beepTrigger = System.currentTimeMillis()) }

        val current = _uiState.value
        if (current.phase == PomodoroPhase.FOCUS) {
            if (current.currentSet >= current.totalSets) {
                stop() // Hoàn thành tất cả
            } else {
                // Chuyển sang Break
                _uiState.update {
                    it.copy(phase = PomodoroPhase.BREAK, remainingMillis = breakDurationMillis)
                }
                startTimer(breakDurationMillis)
            }
        } else if (current.phase == PomodoroPhase.BREAK) {
            // Chuyển sang Focus tiếp theo
            _uiState.update {
                it.copy(
                    phase = PomodoroPhase.FOCUS,
                    currentSet = current.currentSet + 1,
                    remainingMillis = focusDurationMillis
                )
            }
            startTimer(focusDurationMillis)
        }
    }

    fun pause() {
        timer?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resume() {
        val remaining = _uiState.value.remainingMillis
        _uiState.update { it.copy(isPaused = false) }
        startTimer(remaining)
    }

    fun stop() {
        timer?.cancel()
        _uiState.update { it.copy(phase = PomodoroPhase.IDLE, isRunning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}

class PomodoroViewModelFactory(
    private val repo: TodoRepository,
    private val taskId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PomodoroViewModel(repo, taskId) as T
    }
}
