package com.example.myapplication.ui.homework

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.HomeworkItem
import com.example.myapplication.data.HomeworkRepository
import com.example.myapplication.utils.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeworkSolution(
    val id: String = "",
    val imageBitmap: Bitmap? = null,
    val recognizedText: String = "",
    val solution: String = ""
)

data class HomeworkUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentBitmap: Bitmap? = null,
    val solution: HomeworkSolution? = null,
    val previousSolutions: List<HomeworkSolution> = emptyList(),
    val recognizedText: String = ""
)

class HomeworkViewModel(
    private val repository: HomeworkRepository,
    private val currentUsername: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeworkUiState())
    val uiState: StateFlow<HomeworkUiState> = _uiState

    init {
        loadPreviousSolutions()
    }

    private fun loadPreviousSolutions() {
        viewModelScope.launch {
            try {
                val homeworkItems = repository.getAllHomeworkByUsername(currentUsername)
                val solutions = mutableListOf<HomeworkSolution>()

                for (item in homeworkItems) {
                    val bitmap = repository.loadHomeworkImage(item.imagePath)
                    solutions.add(
                        HomeworkSolution(
                            id = item.id.toString(),
                            imageBitmap = bitmap,
                            recognizedText = item.recognizedText,
                            solution = item.solution
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    previousSolutions = solutions
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeworkViewModel", "Error loading previous solutions: ${e.message}")
            }
        }
    }

    fun setCurrentBitmap(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            currentBitmap = bitmap,
            error = null
        )
    }

    fun clearCurrentBitmap() {
        _uiState.value = _uiState.value.copy(
            currentBitmap = null,
            recognizedText = "",
            solution = null
        )
    }

    fun solveProblem() {
        val bitmap = _uiState.value.currentBitmap ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Recognize text from image
                val recognizedText = GeminiService.recognizeText(bitmap)
                _uiState.value = _uiState.value.copy(recognizedText = recognizedText)

                // Get solution
                val solution = GeminiService.solveProblem(bitmap, recognizedText)

                val homeworkId = repository.saveHomework(
                    username = currentUsername,
                    bitmap = bitmap,
                    recognizedText = recognizedText,
                    solution = solution
                )

                val homeworkSolution = HomeworkSolution(
                    id = homeworkId.toString(),
                    imageBitmap = bitmap,
                    recognizedText = recognizedText,
                    solution = solution
                )

                _uiState.value = _uiState.value.copy(
                    solution = homeworkSolution,
                    previousSolutions = listOf(homeworkSolution) + _uiState.value.previousSolutions,
                    isLoading = false,
                    currentBitmap = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Lỗi: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun deleteHomework(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteHomework(id.toLong())
                // Reload solutions
                loadPreviousSolutions()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Lỗi khi xóa: ${e.message}"
                )
            }
        }
    }

    fun backToHome() {
        _uiState.value = _uiState.value.copy(
            currentBitmap = null,
            solution = null,
            recognizedText = "",
            error = null
        )
    }
}

// Factory để tạo ViewModel
class HomeworkViewModelFactory(
    private val context: Context,
    private val currentUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeworkViewModel::class.java)) {
            val repository = HomeworkRepository(context)
            @Suppress("UNCHECKED_CAST")
            return HomeworkViewModel(repository, currentUsername) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

