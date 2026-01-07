package com.example.todo.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.todo.data.AppDatabase
import com.example.todo.data.TodoRepository
import com.example.todo.databinding.ActivityPomodoroBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private lateinit var viewModel: PomodoroViewModel
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getLongExtra("taskId", -1L)
        if (taskId == -1L) {
            finish()
            return
        }

        // Setup ViewModel manual injection
        val dao = AppDatabase.get(applicationContext).todoDao()
        val repo = TodoRepository(dao)
        val factory = PomodoroViewModelFactory(repo, taskId)
        viewModel = ViewModelProvider(this, factory)[PomodoroViewModel::class.java]

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnStart.setOnClickListener {
            val focus = binding.etFocusMin.text.toString().toIntOrNull() ?: 25
            val breakMin = binding.etBreakMin.text.toString().toIntOrNull() ?: 5
            val sets = binding.etSets.text.toString().toIntOrNull() ?: 4
            viewModel.start(focus, breakMin, sets)
        }

        binding.btnStop.setOnClickListener {
            viewModel.stop()
            finish()
        }

        binding.btnPauseResume.setOnClickListener {
            if (viewModel.uiState.value.isPaused) {
                viewModel.resume()
            } else {
                viewModel.pause()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.tvTaskTitle.text = state.taskTitle
                
                // Toggle Views
                if (state.phase == PomodoroPhase.IDLE) {
                    binding.layoutSetup.visibility = View.VISIBLE
                    binding.layoutRunning.visibility = View.GONE
                } else {
                    binding.layoutSetup.visibility = View.GONE
                    binding.layoutRunning.visibility = View.VISIBLE
                }

                // Update Running UI
                binding.tvPhase.text = if (state.phase == PomodoroPhase.FOCUS) "FOCUS" else "BREAK"
                binding.tvProgress.text = "Set ${state.currentSet} / ${state.totalSets}"
                
                val minutes = (state.remainingMillis / 1000) / 60
                val seconds = (state.remainingMillis / 1000) % 60
                binding.tvCountdown.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                binding.btnPauseResume.text = if (state.isPaused) "Resume" else "Pause"
            }
        }
        
        // Listen specifically for beep trigger changes
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.beepTrigger > 0) { 
                    // Simple logic: if timestamp changed recently, beep. 
                     if (System.currentTimeMillis() - state.beepTrigger < 500) {
                         toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                     }
                }
            }
        }
    }
}
