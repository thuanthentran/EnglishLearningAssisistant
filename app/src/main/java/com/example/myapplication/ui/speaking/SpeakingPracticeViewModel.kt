package com.example.myapplication.ui.speaking

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.SpeakingHistoryRepository
import com.example.myapplication.data.repository.SpeakingStatistics
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.utils.SpeakingPracticeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * UI State for Speaking Practice Screen
 */
data class SpeakingPracticeUiState(
    val selectedExamType: SpeakingExamType = SpeakingExamType.TOEIC_Q11,
    val prompt: String = "",
    val audioUri: Uri? = null,
    val audioFileName: String? = null,
    val transcribedText: String = "",
    val isTranscribing: Boolean = false,
    val isScoring: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val feedbackResult: SpeakingFeedbackResult? = null,
    val showFeedbackScreen: Boolean = false,
    val showHistoryScreen: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val speakingHistory: List<SpeakingFeedbackResult> = emptyList()
)

/**
 * ViewModel for Speaking Practice Feature
 */
class SpeakingPracticeViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakingPracticeUiState())
    val uiState: StateFlow<SpeakingPracticeUiState> = _uiState.asStateFlow()

    private val historyRepository = SpeakingHistoryRepository(context)
    private val userPreferences = UserPreferences(context)

    init {
        loadHistory()
    }

    /**
     * Load history from repository
     */
    private fun loadHistory() {
        val username = userPreferences.getUsername()
        val history = historyRepository.getHistory(username)
        _uiState.value = _uiState.value.copy(speakingHistory = history)
    }

    /**
     * Update the speaking prompt/question
     */
    fun setPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(
            prompt = prompt,
            error = null
        )
    }

    /**
     * Set audio file from Uri
     */
    fun setAudioUri(uri: Uri, fileName: String? = null) {
        _uiState.value = _uiState.value.copy(
            audioUri = uri,
            audioFileName = fileName ?: "audio_recording",
            transcribedText = "",
            feedbackResult = null,
            error = null
        )
    }

    /**
     * Clear audio file
     */
    fun clearAudio() {
        _uiState.value = _uiState.value.copy(
            audioUri = null,
            audioFileName = null,
            transcribedText = "",
            feedbackResult = null,
            error = null
        )
    }

    /**
     * Transcribe the audio file
     */
    fun transcribeAudio() {
        val audioUri = _uiState.value.audioUri
        if (audioUri == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please select or record an audio file first"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTranscribing = true,
                isLoading = true,
                error = null
            )

            try {
                // Copy audio to temp file
                val tempFile = copyUriToTempFile(audioUri)
                if (tempFile == null) {
                    _uiState.value = _uiState.value.copy(
                        isTranscribing = false,
                        isLoading = false,
                        error = "Failed to read audio file"
                    )
                    return@launch
                }

                val result = SpeakingPracticeService.transcribeAudio(tempFile)

                result.fold(
                    onSuccess = { transcription ->
                        _uiState.value = _uiState.value.copy(
                            transcribedText = transcription.text,
                            isTranscribing = false,
                            isLoading = false,
                            error = null
                        )
                        // Clean up temp file
                        tempFile.delete()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isTranscribing = false,
                            isLoading = false,
                            error = "Transcription failed: ${exception.message}"
                        )
                        // Clean up temp file
                        tempFile.delete()
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranscribing = false,
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Score the transcribed text
     */
    fun scoreSpeaking() {
        val transcribedText = _uiState.value.transcribedText
        if (transcribedText.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please transcribe audio first"
            )
            return
        }

        val prompt = _uiState.value.prompt
        if (prompt.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a speaking question/prompt"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScoring = true,
                isLoading = true,
                error = null
            )

            val input = SpeakingPracticeInput(
                examType = _uiState.value.selectedExamType,
                prompt = prompt,
                transcribedText = transcribedText
            )

            val result = SpeakingPracticeService.getSpeakingFeedback(input)

            result.fold(
                onSuccess = { feedback ->
                    // Save to history
                    val username = userPreferences.getUsername()
                    val saved = historyRepository.saveFeedbackResult(feedback, username)
                    if (saved) {
                        loadHistory() // Reload history
                    }

                    _uiState.value = _uiState.value.copy(
                        feedbackResult = feedback,
                        showFeedbackScreen = true,
                        isScoring = false,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isScoring = false,
                        isLoading = false,
                        error = "Scoring failed: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * Transcribe and score in one go
     */
    fun transcribeAndScore() {
        val audioUri = _uiState.value.audioUri
        val prompt = _uiState.value.prompt

        if (audioUri == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please select or record an audio file first"
            )
            return
        }

        if (prompt.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a speaking question/prompt"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTranscribing = true,
                isLoading = true,
                error = null
            )

            try {
                // Copy audio to temp file
                val tempFile = copyUriToTempFile(audioUri)
                if (tempFile == null) {
                    _uiState.value = _uiState.value.copy(
                        isTranscribing = false,
                        isLoading = false,
                        error = "Failed to read audio file"
                    )
                    return@launch
                }

                val result = SpeakingPracticeService.transcribeAndScore(
                    audioFile = tempFile,
                    prompt = prompt,
                    examType = _uiState.value.selectedExamType
                )

                result.fold(
                    onSuccess = { feedback ->
                        // Save to history
                        val username = userPreferences.getUsername()
                        val saved = historyRepository.saveFeedbackResult(feedback, username)
                        if (saved) {
                            loadHistory() // Reload history
                        }

                        _uiState.value = _uiState.value.copy(
                            transcribedText = feedback.transcribedText,
                            feedbackResult = feedback,
                            showFeedbackScreen = true,
                            isTranscribing = false,
                            isScoring = false,
                            isLoading = false,
                            error = null
                        )
                        tempFile.delete()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isTranscribing = false,
                            isScoring = false,
                            isLoading = false,
                            error = "Process failed: ${exception.message}"
                        )
                        tempFile.delete()
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranscribing = false,
                    isScoring = false,
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Copy Uri content to a temporary file
     */
    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            val fileName = _uiState.value.audioFileName ?: "audio"
            val extension = getExtensionFromUri(uri) ?: "mp3"
            val tempFile = File(context.cacheDir, "${fileName}_${System.currentTimeMillis()}.$extension")

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get file extension from Uri
     */
    private fun getExtensionFromUri(uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/m4a", "audio/mp4" -> "m4a"
            "audio/ogg" -> "ogg"
            "audio/webm" -> "webm"
            "audio/flac" -> "flac"
            "audio/3gpp" -> "3gp"
            "audio/amr" -> "amr"
            else -> uri.lastPathSegment?.substringAfterLast('.') ?: "mp3"
        }
    }

    /**
     * Back from feedback screen
     */
    fun backFromFeedback() {
        _uiState.value = _uiState.value.copy(
            showFeedbackScreen = false
        )
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset all state
     */
    fun reset() {
        _uiState.value = SpeakingPracticeUiState()
    }

    /**
     * Show history screen
     */
    fun showHistory() {
        _uiState.value = _uiState.value.copy(showHistoryScreen = true)
    }

    /**
     * Hide history screen
     */
    fun hideHistory() {
        _uiState.value = _uiState.value.copy(showHistoryScreen = false)
    }

    /**
     * View feedback from history
     */
    fun viewFeedbackFromHistory(feedbackResult: SpeakingFeedbackResult) {
        _uiState.value = _uiState.value.copy(
            feedbackResult = feedbackResult,
            showFeedbackScreen = true,
            showHistoryScreen = false
        )
    }

    /**
     * Delete a history item
     */
    fun deleteHistoryItem(timestamp: Long) {
        val username = userPreferences.getUsername()
        val deleted = historyRepository.deleteHistoryItem(timestamp, username)
        if (deleted) {
            loadHistory()
        }
    }

    /**
     * Clear all history
     */
    fun clearAllHistory() {
        val username = userPreferences.getUsername()
        val cleared = historyRepository.clearAllHistory(username)
        if (cleared) {
            loadHistory()
        }
    }

    /**
     * Get speaking statistics
     */
    fun getStatistics(): SpeakingStatistics {
        val username = userPreferences.getUsername()
        return historyRepository.getStatistics(username)
    }

    /**
     * Create demo history entries for testing (only in debug mode)
     */
    fun createDemoHistory() {
        val username = userPreferences.getUsername()
        val demoEntries = listOf(
            SpeakingFeedbackResult(
                examType = SpeakingExamType.TOEIC_Q11,
                prompt = "Do you agree or disagree with working from home?",
                transcribedText = "I strongly agree that working from home is more productive. First, you can focus better without office distractions. Second, you save commuting time which can be used for work.",
                feedback = "Good response with clear opinion and supporting reasons. Score breakdown: Content (4/5), Organization (3/5), Language (4/5), Grammar (4/5), Delivery (4/5).",
                overallScore = 4,
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            SpeakingFeedbackResult(
                examType = SpeakingExamType.TOEIC_Q11,
                prompt = "Which is better: small company or large company?",
                transcribedText = "I prefer working for small companies because they offer more flexibility and personal attention. You can learn multiple skills and have direct contact with management.",
                feedback = "Well-structured response with good examples. Score breakdown: Content (3/5), Organization (4/5), Language (3/5), Grammar (3/5), Delivery (3/5).",
                overallScore = 3,
                timestamp = System.currentTimeMillis() - 172800000 // 2 days ago
            )
        )

        demoEntries.forEach { entry ->
            historyRepository.saveFeedbackResult(entry, username)
        }
        loadHistory()
    }

    /**
     * Load sample prompt for demo
     */
    fun loadSamplePrompt() {
        val samplePrompts = listOf(
            "Do you agree or disagree with the following statement? Working from home is more productive than working in an office. Use specific reasons and examples to support your opinion.",
            "Some people prefer to work for a large company, while others prefer to work for a small company. Which do you prefer? Give reasons and examples to support your answer.",
            "Do you agree or disagree that technology has made our lives easier? Use specific reasons and examples to support your opinion.",
            "Some people think that it is important to have a job that pays well. Others think it is more important to have a job you enjoy. Which view do you agree with? Explain why.",
            "Do you agree or disagree with the following statement? People should take time off from work to travel. Give specific reasons and examples to support your answer."
        )
        val randomPrompt = samplePrompts.random()
        _uiState.value = _uiState.value.copy(prompt = randomPrompt)
    }
}

/**
 * Factory for creating SpeakingPracticeViewModel with context
 */
class SpeakingPracticeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeakingPracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpeakingPracticeViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

