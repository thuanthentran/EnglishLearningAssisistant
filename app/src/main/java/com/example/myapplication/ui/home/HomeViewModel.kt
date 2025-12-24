package com.example.myapplication.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.DailyLearningRepository
import com.example.myapplication.data.repository.LearnedWordsRepository
import com.example.myapplication.data.repository.LearningProgressRepository
import com.example.myapplication.data.repository.LearningProgressRepository.DailyProgress
import com.example.myapplication.data.repository.LearningProgressRepository.LearningStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel cho màn hình chính - quản lý tiến trình học
 */
class HomeViewModel(
    private val learningProgressRepository: LearningProgressRepository,
    private val dailyLearningRepository: DailyLearningRepository,
    private val learnedWordsRepository: LearnedWordsRepository
) : ViewModel() {

    // Tiến trình hàng ngày (merged từ cả 2 nguồn)
    private val _dailyProgress = MutableStateFlow(DailyProgress())
    val dailyProgress: StateFlow<DailyProgress> = _dailyProgress.asStateFlow()

    // Thống kê tổng hợp (merged)
    private val _learningStats = MutableStateFlow(LearningStats())
    val learningStats: StateFlow<LearningStats> = _learningStats.asStateFlow()

    // Lịch sử tuần
    private val _weeklyProgress = MutableStateFlow<List<DailyProgress>>(emptyList())
    val weeklyProgress: StateFlow<List<DailyProgress>> = _weeklyProgress.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        syncFromCloud()
    }

    /**
     * Đồng bộ từ cloud khi mở app
     */
    fun syncFromCloud() {
        viewModelScope.launch {
            _isLoading.value = true

            // Sync tất cả repositories
            learnedWordsRepository.syncFromCloud()
            dailyLearningRepository.syncFromCloud()
            learningProgressRepository.syncFromCloud()

            // Merge dữ liệu
            mergeDailyProgress()
            mergeStats()

            loadWeeklyProgress()
            _isLoading.value = false
        }
    }

    /**
     * Merge dữ liệu từ DailyLearningRepository vào dailyProgress
     * DailyLearningRepository là nguồn chính xác cho wordsLearned
     * LearningProgressRepository là nguồn cho quiz và review stats
     */
    private fun mergeDailyProgress() {
        val dailyLearning = dailyLearningRepository.loadFromLocal()
        val progressFromRepo = learningProgressRepository.dailyProgress.value

        // Lấy số từ học hôm nay từ DailyLearningRepository (nguồn chính xác)
        val wordsLearnedToday = dailyLearning.wordsLearnedToday

        _dailyProgress.value = DailyProgress(
            date = progressFromRepo.date.ifEmpty { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) },
            wordsLearned = wordsLearnedToday,
            wordsReviewed = progressFromRepo.wordsReviewed,
            quizCompleted = progressFromRepo.quizCompleted,
            quizCorrect = progressFromRepo.quizCorrect,
            studyTimeMinutes = progressFromRepo.studyTimeMinutes
        )
    }

    /**
     * Merge stats với dữ liệu từ LearnedWordsRepository
     */
    private fun mergeStats() {
        val statsFromRepo = learningProgressRepository.learningStats.value
        val totalLearnedWords = learnedWordsRepository.getLearnedCount()

        _learningStats.value = statsFromRepo.copy(
            totalWordsLearned = totalLearnedWords
        )
    }

    /**
     * Load lịch sử học trong tuần
     */
    fun loadWeeklyProgress() {
        viewModelScope.launch {
            val result = learningProgressRepository.getWeeklyProgress()
            result.onSuccess { progressList ->
                _weeklyProgress.value = progressList
            }
        }
    }

    /**
     * Factory để tạo ViewModel
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                val learningProgressRepository = LearningProgressRepository.getInstance(context)
                val dailyLearningRepository = DailyLearningRepository.getInstance(context)
                val learnedWordsRepository = LearnedWordsRepository.getInstance(context)
                return HomeViewModel(learningProgressRepository, dailyLearningRepository, learnedWordsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

