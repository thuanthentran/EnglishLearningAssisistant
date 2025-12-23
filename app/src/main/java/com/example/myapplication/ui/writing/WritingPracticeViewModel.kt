package com.example.myapplication.ui.writing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.*
import com.example.myapplication.utils.WritingPracticeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Writing Practice Screen
 */
data class WritingPracticeUiState(
    val selectedExamType: WritingExamType = WritingExamType.IELTS_TASK2,
    val prompt: String = "",
    val userEssay: String = "",
    val currentAIMode: WritingAIMode = WritingAIMode.SUGGESTION,
    val isLoading: Boolean = false,
    val error: String? = null,
    val feedbackResult: WritingFeedbackResult? = null,
    val feedbackHistory: List<WritingFeedbackResult> = emptyList(),
    val showFeedbackScreen: Boolean = false
)

/**
 * ViewModel for Writing Practice Feature
 */
class WritingPracticeViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WritingPracticeUiState())
    val uiState: StateFlow<WritingPracticeUiState> = _uiState.asStateFlow()

    /**
     * Update selected exam type
     */
    fun setExamType(examType: WritingExamType) {
        _uiState.value = _uiState.value.copy(
            selectedExamType = examType,
            error = null
        )
        updateAIMode()
    }

    /**
     * Update the writing prompt/question
     */
    fun setPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(
            prompt = prompt,
            error = null
        )
    }

    /**
     * Update user's essay
     * This also automatically updates AI mode to SCORING if essay is provided
     */
    fun setUserEssay(essay: String) {
        _uiState.value = _uiState.value.copy(
            userEssay = essay,
            error = null
        )
        updateAIMode()
    }

    /**
     * Manually set AI mode (only effective when user essay is empty)
     */
    fun setAIMode(mode: WritingAIMode) {
        if (_uiState.value.userEssay.isBlank()) {
            _uiState.value = _uiState.value.copy(
                currentAIMode = mode,
                error = null
            )
        }
    }

    /**
     * Update AI mode based on whether user has provided an essay
     */
    private fun updateAIMode() {
        val newMode = if (_uiState.value.userEssay.isNotBlank()) {
            WritingAIMode.SCORING
        } else {
            WritingAIMode.SUGGESTION
        }
        _uiState.value = _uiState.value.copy(currentAIMode = newMode)
    }

    /**
     * Get feedback from AI
     */
    fun getFeedback() {
        val currentState = _uiState.value

        // Validate input
        if (currentState.prompt.isBlank()) {
            _uiState.value = currentState.copy(
                error = "Please enter a writing prompt/question"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val input = WritingPracticeInput(
                examType = currentState.selectedExamType,
                prompt = currentState.prompt,
                userEssay = currentState.userEssay,
                aiMode = currentState.currentAIMode
            )

            val result = WritingPracticeService.getFeedback(input)

            result.fold(
                onSuccess = { feedbackResult ->
                    _uiState.value = _uiState.value.copy(
                        feedbackResult = feedbackResult,
                        feedbackHistory = listOf(feedbackResult) + _uiState.value.feedbackHistory,
                        isLoading = false,
                        showFeedbackScreen = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Error: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    /**
     * Navigate back from feedback screen
     */
    fun backFromFeedback() {
        _uiState.value = _uiState.value.copy(
            showFeedbackScreen = false,
            feedbackResult = null
        )
    }

    /**
     * Clear all inputs and start fresh
     */
    fun clearAll() {
        _uiState.value = _uiState.value.copy(
            prompt = "",
            userEssay = "",
            currentAIMode = WritingAIMode.SUGGESTION,
            error = null,
            feedbackResult = null,
            showFeedbackScreen = false
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * View a feedback from history
     */
    fun viewFeedbackFromHistory(feedbackResult: WritingFeedbackResult) {
        _uiState.value = _uiState.value.copy(
            feedbackResult = feedbackResult,
            showFeedbackScreen = true
        )
    }

    /**
     * Get essay type display name for current exam type
     */
    fun getEssayTypeDisplayName(): String {
        return _uiState.value.selectedExamType.essayType
    }

    /**
     * Get word count of user's essay
     */
    fun getWordCount(): Int {
        val essay = _uiState.value.userEssay
        return if (essay.isBlank()) 0 else essay.trim().split("\\s+".toRegex()).size
    }

    /**
     * Check if minimum word count is met
     */
    fun isMinimumWordCountMet(): Boolean {
        val wordCount = getWordCount()
        return when (_uiState.value.selectedExamType) {
            WritingExamType.IELTS_TASK2 -> wordCount >= 250
            WritingExamType.TOEIC_Q8 -> wordCount >= 300
        }
    }

    /**
     * Get minimum word count for current exam type
     */
    fun getMinimumWordCount(): Int {
        return when (_uiState.value.selectedExamType) {
            WritingExamType.IELTS_TASK2 -> 250
            WritingExamType.TOEIC_Q8 -> 300
        }
    }
}

/**
 * Factory for creating WritingPracticeViewModel
 */
class WritingPracticeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WritingPracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WritingPracticeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

