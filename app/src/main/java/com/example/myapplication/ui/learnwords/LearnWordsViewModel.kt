package com.example.myapplication.ui.learnwords

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.DailyLearningRepository
import com.example.myapplication.data.repository.DictionaryRepository
import com.example.myapplication.data.repository.LearnedWordsRepository
import com.example.myapplication.data.repository.LearningProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LearnWordsViewModel(
    private val dictionaryRepository: DictionaryRepository,
    private val learnedWordsRepository: LearnedWordsRepository,
    private val dailyLearningRepository: DailyLearningRepository,
    private val learningProgressRepository: LearningProgressRepository
) : ViewModel() {

    companion object {
        private const val DAILY_GOAL = 10
        private const val ANSWER_OPTIONS_COUNT = 4
    }

    private val _uiState = MutableStateFlow(LearnWordsUiState(dailyGoal = DAILY_GOAL))
    val uiState: StateFlow<LearnWordsUiState> = _uiState.asStateFlow()

    init {
        loadDailyStatus()
    }

    /**
     * Load trạng thái học hàng ngày (từ local và sync từ cloud)
     */
    fun loadDailyStatus() {
        viewModelScope.launch {
            // Sync từ cloud trước
            dailyLearningRepository.syncFromCloud()

            // Load từ local (đã được merge với cloud)
            val dailyProgress = dailyLearningRepository.loadFromLocal()
            val totalLearnedWords = learnedWordsRepository.getLearnedCount()

            // Load từ đã học trong tuần
            val weekWordsResult = dailyLearningRepository.getWeekLearnedWords()
            val weekLearnedWordIds = weekWordsResult.getOrNull() ?: emptySet()

            _uiState.update {
                it.copy(
                    wordsLearnedToday = dailyProgress.wordsLearnedToday,
                    totalLearnedWords = totalLearnedWords,
                    todayLearnedWordIds = dailyProgress.todayLearnedWordIds,
                    weekLearnedWordIds = weekLearnedWordIds
                )
            }
        }
    }

    /**
     * Load từ mới để học hoặc xem lại từ đã học hôm nay
     */
    fun loadNewWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNewWords = true, errorMessage = null) }

            try {
                val learnedIds = learnedWordsRepository.getLearnedWordIds()
                val todayLearnedIds = _uiState.value.todayLearnedWordIds
                val remainingToday = DAILY_GOAL - _uiState.value.wordsLearnedToday

                // Nếu đã học đủ, hiển thị từ đã học hôm nay để xem lại
                if (remainingToday <= 0 && todayLearnedIds.isNotEmpty()) {
                    // Lấy từ theo ID trực tiếp
                    val todayWords = dictionaryRepository.getWordsByIds(todayLearnedIds)
                        .filter { it.definition.isNotBlank() }
                        .map { word ->
                            WordToLearn(
                                id = word.id,
                                word = word.word,
                                phonetic = word.phonetic,
                                type = word.type,
                                definition = word.definition,
                                shortDefinition = word.definition.split(",").first().trim()
                            )
                        }

                    if (todayWords.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoadingNewWords = false,
                                errorMessage = "Không tìm thấy từ đã học hôm nay"
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoadingNewWords = false,
                            newWordsToLearn = todayWords,
                            currentNewWordIndex = 0,
                            isNewWordsSessionComplete = false,
                            newWordsScore = todayWords.size // Đã học hết rồi
                        )
                    }
                    return@launch
                }

                // Lấy từ ngẫu nhiên chưa học
                val allRandomWords = dictionaryRepository.getRandomWords(remainingToday + learnedIds.size + 50)
                val newWords = allRandomWords
                    .filter { it.id !in learnedIds && it.definition.isNotBlank() }
                    .take(remainingToday)
                    .map { word ->
                        WordToLearn(
                            id = word.id,
                            word = word.word,
                            phonetic = word.phonetic,
                            type = word.type,
                            definition = word.definition,
                            shortDefinition = word.definition.split(",").first().trim()
                        )
                    }

                if (newWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingNewWords = false,
                            errorMessage = "Không còn từ mới để học"
                        )
                    }
                    return@launch
                }

                // Tạo đáp án cho từ đầu tiên
                val answerOptions = generateAnswerOptions(newWords.first(), learnedIds)

                _uiState.update {
                    it.copy(
                        isLoadingNewWords = false,
                        newWordsToLearn = newWords,
                        currentNewWordIndex = 0,
                        newWordAnswerOptions = answerOptions,
                        selectedNewWordAnswer = null,
                        isNewWordAnswerCorrect = null,
                        isNewWordsSessionComplete = false,
                        newWordsScore = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingNewWords = false,
                        errorMessage = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load từ đã học hôm nay để ôn
     */
    fun loadReviewWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReview = true, errorMessage = null) }

            try {
                // Lấy danh sách từ đã học hôm nay
                val todayLearnedIds = _uiState.value.todayLearnedWordIds

                if (todayLearnedIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingReview = false,
                            errorMessage = "Hôm nay bạn chưa học từ nào. Hãy học từ mới trước nhé!"
                        )
                    }
                    return@launch
                }

                // Lấy từ đã học hôm nay theo ID trực tiếp
                val reviewWords = dictionaryRepository.getWordsByIds(todayLearnedIds)
                    .filter { it.definition.isNotBlank() }
                    .shuffled()
                    .map { word ->
                        WordToLearn(
                            id = word.id,
                            word = word.word,
                            phonetic = word.phonetic,
                            type = word.type,
                            definition = word.definition,
                            shortDefinition = word.definition.split(",").first().trim()
                        )
                    }

                if (reviewWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingReview = false,
                            errorMessage = "Không tìm thấy từ để ôn tập"
                        )
                    }
                    return@launch
                }

                // Tạo đáp án cho từ đầu tiên
                val allLearnedIds = learnedWordsRepository.getLearnedWordIds()
                val answerOptions = generateAnswerOptions(reviewWords.first(), allLearnedIds)

                _uiState.update {
                    it.copy(
                        isLoadingReview = false,
                        reviewWords = reviewWords,
                        currentReviewIndex = 0,
                        reviewAnswerOptions = answerOptions,
                        selectedReviewAnswer = null,
                        isReviewAnswerCorrect = null,
                        isReviewSessionComplete = false,
                        reviewScore = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingReview = false,
                        errorMessage = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Tạo đáp án cho câu hỏi
     */
    private fun generateAnswerOptions(correctWord: WordToLearn, excludeIds: Set<Int>): List<String> {
        val correctAnswer = correctWord.shortDefinition
        val wrongAnswers = dictionaryRepository.getRandomDefinitions(correctWord.id, ANSWER_OPTIONS_COUNT * 2)
            .map { it.split(",").first().trim() }
            .filter { it != correctAnswer && it.isNotBlank() }
            .distinct()
            .take(ANSWER_OPTIONS_COUNT - 1)

        val allOptions = (wrongAnswers + correctAnswer).shuffled()
        return allOptions
    }

    /**
     * Đánh dấu từ hiện tại là đã học (dùng cho UI học từ mới)
     */
    fun markCurrentWordAsLearned() {
        val currentState = _uiState.value
        val currentWord = currentState.newWordsToLearn.getOrNull(currentState.currentNewWordIndex) ?: return

        viewModelScope.launch {
            // Đánh dấu từ đã học (sync lên cloud)
            learnedWordsRepository.markLearned(currentWord.id)

            // Thêm vào danh sách học hôm nay (sync lên cloud)
            val newProgress = dailyLearningRepository.addLearnedWord(currentWord.id)

            // Ghi nhận vào learning progress (cho thanh tiến trình)
            learningProgressRepository.recordWordLearned(1)

            _uiState.update {
                it.copy(
                    newWordsScore = it.newWordsScore + 1,
                    wordsLearnedToday = newProgress.wordsLearnedToday,
                    totalLearnedWords = it.totalLearnedWords + 1,
                    todayLearnedWordIds = newProgress.todayLearnedWordIds
                )
            }
        }
    }

    /**
     * Chọn đáp án cho từ mới
     */
    fun selectNewWordAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState.selectedNewWordAnswer != null) return // Đã chọn rồi

        val currentWord = currentState.newWordsToLearn.getOrNull(currentState.currentNewWordIndex) ?: return
        val selectedAnswer = currentState.newWordAnswerOptions.getOrNull(answerIndex) ?: return
        val isCorrect = selectedAnswer == currentWord.shortDefinition

        viewModelScope.launch {
            // Nếu đúng, đánh dấu từ đã học
            if (isCorrect) {
                learnedWordsRepository.markLearned(currentWord.id)
                val newProgress = dailyLearningRepository.addLearnedWord(currentWord.id)

                _uiState.update {
                    it.copy(
                        selectedNewWordAnswer = answerIndex,
                        isNewWordAnswerCorrect = true,
                        newWordsScore = it.newWordsScore + 1,
                        wordsLearnedToday = newProgress.wordsLearnedToday,
                        totalLearnedWords = it.totalLearnedWords + 1,
                        todayLearnedWordIds = newProgress.todayLearnedWordIds
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        selectedNewWordAnswer = answerIndex,
                        isNewWordAnswerCorrect = false
                    )
                }
            }
        }
    }

    /**
     * Chọn đáp án cho ôn tập
     */
    fun selectReviewAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState.selectedReviewAnswer != null) return // Đã chọn rồi

        val currentWord = currentState.reviewWords.getOrNull(currentState.currentReviewIndex) ?: return
        val selectedAnswer = currentState.reviewAnswerOptions.getOrNull(answerIndex) ?: return
        val isCorrect = selectedAnswer == currentWord.shortDefinition

        // Ghi nhận quiz result
        learningProgressRepository.recordQuizResult(1, if (isCorrect) 1 else 0)
        learningProgressRepository.recordWordReviewed(1)

        _uiState.update {
            it.copy(
                selectedReviewAnswer = answerIndex,
                isReviewAnswerCorrect = isCorrect,
                reviewScore = if (isCorrect) it.reviewScore + 1 else it.reviewScore
            )
        }
    }

    /**
     * Chọn đáp án cho ôn tập tuần
     */
    fun selectWeekReviewAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState.selectedWeekReviewAnswer != null) return // Đã chọn rồi

        val currentWord = currentState.weekReviewWords.getOrNull(currentState.currentWeekReviewIndex) ?: return
        val selectedAnswer = currentState.weekReviewAnswerOptions.getOrNull(answerIndex) ?: return
        val isCorrect = selectedAnswer == currentWord.shortDefinition

        // Ghi nhận quiz result
        learningProgressRepository.recordQuizResult(1, if (isCorrect) 1 else 0)
        learningProgressRepository.recordWordReviewed(1)

        _uiState.update {
            it.copy(
                selectedWeekReviewAnswer = answerIndex,
                isWeekReviewAnswerCorrect = isCorrect,
                weekReviewScore = if (isCorrect) it.weekReviewScore + 1 else it.weekReviewScore
            )
        }
    }

    /**
     * Chuyển sang từ mới tiếp theo
     */
    fun nextNewWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentNewWordIndex + 1

        if (nextIndex >= currentState.newWordsToLearn.size) {
            // Hoàn thành session
            _uiState.update { it.copy(isNewWordsSessionComplete = true) }
            return
        }

        val nextWord = currentState.newWordsToLearn[nextIndex]
        val learnedIds = learnedWordsRepository.getLearnedWordIds()
        val answerOptions = generateAnswerOptions(nextWord, learnedIds)

        _uiState.update {
            it.copy(
                currentNewWordIndex = nextIndex,
                newWordAnswerOptions = answerOptions,
                selectedNewWordAnswer = null,
                isNewWordAnswerCorrect = null,
                isCardFlipped = false // Reset flip state
            )
        }
    }

    /**
     * Chuyển sang từ ôn tập tiếp theo
     */
    fun nextReviewWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentReviewIndex + 1

        if (nextIndex >= currentState.reviewWords.size) {
            // Hoàn thành session
            _uiState.update { it.copy(isReviewSessionComplete = true) }
            return
        }

        val nextWord = currentState.reviewWords[nextIndex]
        val learnedIds = learnedWordsRepository.getLearnedWordIds()
        val answerOptions = generateAnswerOptions(nextWord, learnedIds)

        _uiState.update {
            it.copy(
                currentReviewIndex = nextIndex,
                reviewAnswerOptions = answerOptions,
                selectedReviewAnswer = null,
                isReviewAnswerCorrect = null
            )
        }
    }

    /**
     * Reset session
     */
    fun resetNewWordsSession() {
        _uiState.update {
            it.copy(
                newWordsToLearn = emptyList(),
                currentNewWordIndex = 0,
                newWordAnswerOptions = emptyList(),
                selectedNewWordAnswer = null,
                isNewWordAnswerCorrect = null,
                isNewWordsSessionComplete = false,
                newWordsScore = 0,
                isCardFlipped = false
            )
        }
    }

    fun resetReviewSession() {
        _uiState.update {
            it.copy(
                reviewWords = emptyList(),
                currentReviewIndex = 0,
                reviewAnswerOptions = emptyList(),
                selectedReviewAnswer = null,
                isReviewAnswerCorrect = null,
                isReviewSessionComplete = false,
                reviewScore = 0
            )
        }
    }

    fun resetWeekReviewSession() {
        _uiState.update {
            it.copy(
                weekReviewWords = emptyList(),
                currentWeekReviewIndex = 0,
                weekReviewAnswerOptions = emptyList(),
                selectedWeekReviewAnswer = null,
                isWeekReviewAnswerCorrect = null,
                isWeekReviewSessionComplete = false,
                weekReviewScore = 0
            )
        }
    }

    /**
     * Load từ đã học trong tuần để ôn
     */
    fun loadWeekReviewWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeekReview = true, errorMessage = null) }

            try {
                // Lấy danh sách từ đã học trong tuần
                val weekWordsResult = dailyLearningRepository.getWeekLearnedWords()
                val weekLearnedIds = weekWordsResult.getOrNull() ?: emptySet()

                if (weekLearnedIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingWeekReview = false,
                            errorMessage = "Tuần này bạn chưa học từ nào. Hãy học từ mới trước nhé!"
                        )
                    }
                    return@launch
                }

                // Lấy TẤT CẢ từ đã học trong tuần theo ID (không giới hạn)
                val reviewWords = dictionaryRepository.getWordsByIds(weekLearnedIds)
                    .filter { it.definition.isNotBlank() }
                    .shuffled() // Trộn ngẫu nhiên để ôn đa dạng
                    .map { word ->
                        WordToLearn(
                            id = word.id,
                            word = word.word,
                            phonetic = word.phonetic,
                            type = word.type,
                            definition = word.definition,
                            shortDefinition = word.definition.split(",").first().trim()
                        )
                    }
                // Không có .take() nữa - lấy tất cả các từ

                if (reviewWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingWeekReview = false,
                            errorMessage = "Không tìm thấy từ để ôn tập"
                        )
                    }
                    return@launch
                }

                // Tạo đáp án cho từ đầu tiên
                val allLearnedIds = learnedWordsRepository.getLearnedWordIds()
                val answerOptions = generateAnswerOptions(reviewWords.first(), allLearnedIds)

                _uiState.update {
                    it.copy(
                        isLoadingWeekReview = false,
                        weekReviewWords = reviewWords,
                        currentWeekReviewIndex = 0,
                        weekReviewAnswerOptions = answerOptions,
                        selectedWeekReviewAnswer = null,
                        isWeekReviewAnswerCorrect = null,
                        isWeekReviewSessionComplete = false,
                        weekReviewScore = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingWeekReview = false,
                        errorMessage = "Lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Lật card học từ mới
     */
    fun flipCard() {
        _uiState.update {
            it.copy(isCardFlipped = !it.isCardFlipped)
        }
    }


    /**
     * Chuyển sang từ ôn tập tuần tiếp theo
     */
    fun nextWeekReviewWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentWeekReviewIndex + 1

        if (nextIndex >= currentState.weekReviewWords.size) {
            // Hoàn thành session
            _uiState.update { it.copy(isWeekReviewSessionComplete = true) }
            return
        }

        val nextWord = currentState.weekReviewWords[nextIndex]
        val learnedIds = learnedWordsRepository.getLearnedWordIds()
        val answerOptions = generateAnswerOptions(nextWord, learnedIds)

        _uiState.update {
            it.copy(
                currentWeekReviewIndex = nextIndex,
                weekReviewAnswerOptions = answerOptions,
                selectedWeekReviewAnswer = null,
                isWeekReviewAnswerCorrect = null
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LearnWordsViewModel::class.java)) {
                val dictionaryRepository = DictionaryRepository(context)
                val learnedWordsRepository = LearnedWordsRepository.getInstance(context)
                val dailyLearningRepository = DailyLearningRepository.getInstance(context)
                val learningProgressRepository = LearningProgressRepository.getInstance(context)
                return LearnWordsViewModel(
                    dictionaryRepository,
                    learnedWordsRepository,
                    dailyLearningRepository,
                    learningProgressRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

