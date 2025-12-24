package com.example.myapplication.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.DictionaryWord
import com.example.myapplication.data.repository.DictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(
    private val repository: DictionaryRepository
) : ViewModel() {

    // Multiple Choice State
    private val _multipleChoiceState = MutableStateFlow(MultipleChoiceUiState())
    val multipleChoiceState: StateFlow<MultipleChoiceUiState> = _multipleChoiceState.asStateFlow()

    // Match Word State
    private val _matchWordState = MutableStateFlow(MatchWordUiState())
    val matchWordState: StateFlow<MatchWordUiState> = _matchWordState.asStateFlow()

    // Store all questions for multiple choice game
    private var allQuestions: List<DictionaryWord> = emptyList()

    // ==================== MULTIPLE CHOICE GAME ====================

    fun initMultipleChoiceGame() {
        viewModelScope.launch {
            _multipleChoiceState.value = MultipleChoiceUiState(isLoading = true)

            try {
                val words = withContext(Dispatchers.IO) {
                    repository.getRandomWords(10)
                }

                if (words.isEmpty()) {
                    _multipleChoiceState.value = MultipleChoiceUiState(
                        isLoading = false,
                        errorMessage = "Không thể tải từ vựng. Vui lòng thử lại."
                    )
                    return@launch
                }

                allQuestions = words
                loadQuestion(0)

            } catch (e: Exception) {
                _multipleChoiceState.value = MultipleChoiceUiState(
                    isLoading = false,
                    errorMessage = "Đã xảy ra lỗi: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadQuestion(questionIndex: Int) {
        if (questionIndex >= allQuestions.size) {
            _multipleChoiceState.value = _multipleChoiceState.value.copy(
                isGameFinished = true,
                isLoading = false
            )
            return
        }

        val currentWord = allQuestions[questionIndex]
        val correctAnswer = getFirstMeaning(currentWord.definition)

        // Get 3 wrong answers
        val wrongDefinitions = withContext(Dispatchers.IO) {
            repository.getRandomDefinitions(currentWord.id, 3)
                .map { getFirstMeaning(it) }
        }

        // Create options with correct answer at random position
        val allOptions = (wrongDefinitions + correctAnswer).shuffled()
        val correctIndex = allOptions.indexOf(correctAnswer)

        _multipleChoiceState.value = MultipleChoiceUiState(
            isLoading = false,
            currentQuestion = questionIndex,
            totalQuestions = allQuestions.size,
            score = _multipleChoiceState.value.score,
            currentWord = currentWord,
            options = allOptions,
            correctAnswerIndex = correctIndex,
            selectedAnswerIndex = null,
            isAnswered = false,
            isGameFinished = false
        )
    }

    fun onMultipleChoiceEvent(event: GameEvent) {
        when (event) {
            is GameEvent.SelectAnswer -> selectAnswer(event.index)
            is GameEvent.NextQuestion -> nextQuestion()
            is GameEvent.RestartGame -> restartMultipleChoiceGame()
            else -> { /* Ignore other events */ }
        }
    }

    private fun selectAnswer(index: Int) {
        val currentState = _multipleChoiceState.value
        if (currentState.isAnswered) return

        val isCorrect = index == currentState.correctAnswerIndex
        val newScore = if (isCorrect) currentState.score + 1 else currentState.score

        _multipleChoiceState.value = currentState.copy(
            selectedAnswerIndex = index,
            isAnswered = true,
            score = newScore
        )
    }

    private fun nextQuestion() {
        viewModelScope.launch {
            val nextIndex = _multipleChoiceState.value.currentQuestion + 1
            loadQuestion(nextIndex)
        }
    }

    private fun restartMultipleChoiceGame() {
        _multipleChoiceState.value = MultipleChoiceUiState(isLoading = true)
        initMultipleChoiceGame()
    }

    // ==================== MATCH WORD GAME ====================

    fun initMatchWordGame() {
        viewModelScope.launch {
            _matchWordState.value = MatchWordUiState(isLoading = true)
            loadMatchRound()
        }
    }

    private suspend fun loadMatchRound() {
        try {
            val words = withContext(Dispatchers.IO) {
                repository.getRandomWords(5)
            }

            if (words.size < 5) {
                _matchWordState.value = _matchWordState.value.copy(
                    isLoading = false,
                    errorMessage = "Không đủ từ vựng để chơi. Vui lòng thử lại."
                )
                return
            }

            val leftItems = words.mapIndexed { index, word ->
                MatchItem(
                    index = index,
                    text = word.word,
                    originalWordId = word.id
                )
            }

            val rightItems = words.mapIndexed { index, word ->
                MatchItem(
                    index = index,
                    text = getFirstMeaning(word.definition),
                    originalWordId = word.id
                )
            }.shuffled()

            _matchWordState.value = _matchWordState.value.copy(
                isLoading = false,
                words = words,
                leftItems = leftItems,
                rightItems = rightItems,
                selectedLeftIndex = null,
                selectedRightIndex = null,
                matchedPairs = emptySet(),
                wrongPairIndices = null,
                isRoundComplete = false,
                errorMessage = null
            )

        } catch (e: Exception) {
            _matchWordState.value = _matchWordState.value.copy(
                isLoading = false,
                errorMessage = "Đã xảy ra lỗi: ${e.message}"
            )
        }
    }

    fun onMatchWordEvent(event: GameEvent) {
        when (event) {
            is GameEvent.SelectLeftItem -> selectLeftItem(event.index)
            is GameEvent.SelectRightItem -> selectRightItem(event.index)
            is GameEvent.NextRound -> nextRound()
            is GameEvent.RestartMatchGame -> restartMatchGame()
            else -> { /* Ignore other events */ }
        }
    }

    private fun selectLeftItem(index: Int) {
        val currentState = _matchWordState.value
        if (currentState.matchedPairs.contains(index)) return

        _matchWordState.value = currentState.copy(
            selectedLeftIndex = index,
            wrongPairIndices = null
        )

        // Check if both sides selected
        checkMatch()
    }

    private fun selectRightItem(index: Int) {
        val currentState = _matchWordState.value
        val rightItem = currentState.rightItems.getOrNull(index) ?: return

        // Check if this right item is already matched
        if (currentState.matchedPairs.any { leftIdx ->
            val leftItem = currentState.leftItems.getOrNull(leftIdx)
            leftItem?.originalWordId == rightItem.originalWordId
        }) return

        _matchWordState.value = currentState.copy(
            selectedRightIndex = index,
            wrongPairIndices = null
        )

        // Check if both sides selected
        checkMatch()
    }

    private fun checkMatch() {
        val currentState = _matchWordState.value
        val leftIdx = currentState.selectedLeftIndex ?: return
        val rightIdx = currentState.selectedRightIndex ?: return

        val leftItem = currentState.leftItems.getOrNull(leftIdx) ?: return
        val rightItem = currentState.rightItems.getOrNull(rightIdx) ?: return

        if (leftItem.originalWordId == rightItem.originalWordId) {
            // Correct match
            val newMatchedPairs = currentState.matchedPairs + leftIdx
            val isRoundComplete = newMatchedPairs.size == currentState.leftItems.size

            _matchWordState.value = currentState.copy(
                matchedPairs = newMatchedPairs,
                selectedLeftIndex = null,
                selectedRightIndex = null,
                score = currentState.score + 1,
                isRoundComplete = isRoundComplete,
                isGameFinished = isRoundComplete && currentState.currentRound >= currentState.totalRounds
            )
        } else {
            // Wrong match - show red briefly
            _matchWordState.value = currentState.copy(
                wrongPairIndices = Pair(leftIdx, rightIdx),
                selectedLeftIndex = null,
                selectedRightIndex = null
            )
        }
    }

    private fun nextRound() {
        viewModelScope.launch {
            val currentState = _matchWordState.value
            _matchWordState.value = currentState.copy(
                currentRound = currentState.currentRound + 1,
                isLoading = true
            )
            loadMatchRound()
        }
    }

    private fun restartMatchGame() {
        _matchWordState.value = MatchWordUiState(isLoading = true)
        initMatchWordGame()
    }

    // ==================== FACTORY ====================

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = DictionaryRepository(context)
            return GameViewModel(repository) as T
        }
    }
}

