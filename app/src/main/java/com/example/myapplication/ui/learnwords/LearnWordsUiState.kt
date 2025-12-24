package com.example.myapplication.ui.learnwords

/**
 * UI State cho màn hình Học từ
 */
data class LearnWordsUiState(
    // Trạng thái hàng ngày
    val wordsLearnedToday: Int = 0,
    val dailyGoal: Int = 10,
    val totalLearnedWords: Int = 0,
    val todayLearnedWordIds: Set<Int> = emptySet(), // Các từ đã học hôm nay
    val weekLearnedWordIds: Set<Int> = emptySet(), // Các từ đã học trong tuần

    // Trạng thái học từ mới
    val isLoadingNewWords: Boolean = false,
    val newWordsToLearn: List<WordToLearn> = emptyList(),
    val currentNewWordIndex: Int = 0,
    val newWordAnswerOptions: List<String> = emptyList(),
    val selectedNewWordAnswer: Int? = null,
    val isNewWordAnswerCorrect: Boolean? = null,
    val isCardFlipped: Boolean = false, // Trạng thái lật card

    // Trạng thái ôn từ
    val isLoadingReview: Boolean = false,
    val reviewWords: List<WordToLearn> = emptyList(),
    val currentReviewIndex: Int = 0,
    val reviewAnswerOptions: List<String> = emptyList(),
    val selectedReviewAnswer: Int? = null,
    val isReviewAnswerCorrect: Boolean? = null,

    // Trạng thái ôn từ trong tuần
    val isLoadingWeekReview: Boolean = false,
    val weekReviewWords: List<WordToLearn> = emptyList(),
    val currentWeekReviewIndex: Int = 0,
    val weekReviewAnswerOptions: List<String> = emptyList(),
    val selectedWeekReviewAnswer: Int? = null,
    val isWeekReviewAnswerCorrect: Boolean? = null,

    // Game finished states
    val isNewWordsSessionComplete: Boolean = false,
    val isReviewSessionComplete: Boolean = false,
    val isWeekReviewSessionComplete: Boolean = false,
    val newWordsScore: Int = 0,
    val reviewScore: Int = 0,
    val weekReviewScore: Int = 0,

    // Error
    val errorMessage: String? = null
)

/**
 * Từ cần học/ôn
 */
data class WordToLearn(
    val id: Int,
    val word: String,
    val phonetic: String,
    val type: String,
    val definition: String,
    val shortDefinition: String // Định nghĩa ngắn gọn cho game
)

/**
 * Các sự kiện trong màn hình Học từ
 */
sealed class LearnWordsEvent {
    // Học từ mới
    data object LoadNewWords : LearnWordsEvent()
    data class SelectNewWordAnswer(val answerIndex: Int) : LearnWordsEvent()
    data object NextNewWord : LearnWordsEvent()
    data object FinishNewWordsSession : LearnWordsEvent()
    data object FlipCard : LearnWordsEvent()

    // Ôn từ đã học
    data object LoadReviewWords : LearnWordsEvent()
    data class SelectReviewAnswer(val answerIndex: Int) : LearnWordsEvent()
    data object NextReviewWord : LearnWordsEvent()
    data object FinishReviewSession : LearnWordsEvent()

    // Ôn từ đã học trong tuần
    data object LoadWeekReviewWords : LearnWordsEvent()
    data class SelectWeekReviewAnswer(val answerIndex: Int) : LearnWordsEvent()
    data object NextWeekReviewWord : LearnWordsEvent()
    data object FinishWeekReviewSession : LearnWordsEvent()

    // Chung
    data object ResetSession : LearnWordsEvent()
}



