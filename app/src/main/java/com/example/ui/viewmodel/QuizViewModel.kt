package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LevelEntity
import com.example.data.local.QuizQuestionEntity
import com.example.data.repository.ConcoursRepository
import com.example.data.repository.QuizCompletionReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizSessionUiState(
    val isLoading: Boolean = true,
    val level: LevelEntity? = null,
    val questions: List<QuizQuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isChecked: Boolean = false,
    val isCurrentCorrect: Boolean = false,
    val heartsRemaining: Int = 5,
    val correctCount: Int = 0,
    val currentCombo: Int = 0,
    val maxCombo: Int = 0,
    val isFinished: Boolean = false,
    val completionReport: QuizCompletionReport? = null,
    val errorMessage: String? = null
) {
    val currentQuestion: QuizQuestionEntity?
        get() = questions.getOrNull(currentIndex)

    val completedSteps: Int
        get() {
            if (questions.isEmpty()) return 0
            val raw = if (isChecked) currentIndex + 1 else currentIndex
            return raw.coerceIn(0, questions.size)
        }

    val progressFraction: Float
        get() {
            if (questions.isEmpty()) return 0f
            return (completedSteps.toFloat() / questions.size.toFloat()).coerceIn(0f, 1f)
        }
}

class QuizViewModel(
    private val repository: ConcoursRepository,
    private val levelId: String,
    private val currentUserId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizSessionUiState())
    val uiState: StateFlow<QuizSessionUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val (level, questions) = repository.getLevelWithQuestions(levelId)
            val stats = repository.userStatsFlow.first()
            if (level == null || questions.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Impossible de charger ce niveau de QCM."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        level = level,
                        questions = questions,
                        heartsRemaining = stats.hearts.coerceAtLeast(1)
                    )
                }
            }
        }
    }

    fun selectOption(index: Int) {
        _uiState.update { state ->
            if (state.isChecked || state.isFinished) state
            else state.copy(selectedOptionIndex = index)
        }
    }

    fun checkAnswer() {
        _uiState.update { state ->
            val question = state.currentQuestion ?: return@update state
            val selected = state.selectedOptionIndex ?: return@update state
            if (state.isChecked) return@update state

            val isCorrect = selected == question.correctIndex
            val nextCombo = if (isCorrect) state.currentCombo + 1 else 0
            val nextMaxCombo = maxOf(state.maxCombo, nextCombo)
            val nextHearts = if (isCorrect) state.heartsRemaining else (state.heartsRemaining - 1).coerceAtLeast(0)
            val nextCorrectCount = if (isCorrect) state.correctCount + 1 else state.correctCount

            state.copy(
                isChecked = true,
                isCurrentCorrect = isCorrect,
                currentCombo = nextCombo,
                maxCombo = nextMaxCombo,
                heartsRemaining = nextHearts,
                correctCount = nextCorrectCount
            )
        }
    }

    fun continueToNext() {
        val state = _uiState.value
        if (!state.isChecked || state.isFinished) return

        val hasNextQuestion = state.currentIndex + 1 < state.questions.size
        if (hasNextQuestion && state.heartsRemaining > 0) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isChecked = false,
                    isCurrentCorrect = false
                )
            }
        } else {
            finishSession(state)
        }
    }

    private fun finishSession(state: QuizSessionUiState) {
        val level = state.level ?: return
        viewModelScope.launch {
            val report = repository.recordQuizCompletion(
                level = level,
                correctCount = state.correctCount,
                totalCount = state.questions.size,
                maxCombo = state.maxCombo,
                remainingHearts = state.heartsRemaining,
                currentUserId = currentUserId
            )
            _uiState.update {
                it.copy(
                    isFinished = true,
                    completionReport = report
                )
            }
        }
    }
}
