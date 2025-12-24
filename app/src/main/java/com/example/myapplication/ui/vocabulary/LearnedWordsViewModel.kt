package com.example.myapplication.ui.vocabulary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.LearnedWordsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LearnedWordsViewModel(
    private val repository: LearnedWordsRepository
) : ViewModel() {

    val learnedWordIds: StateFlow<Set<Int>> = repository.learnedWordIds

    /**
     * Đánh dấu từ là đã học
     */
    fun markAsLearned(wordId: Int) {
        viewModelScope.launch {
            repository.markLearned(wordId)
        }
    }

    /**
     * Bỏ đánh dấu từ đã học
     */
    fun unmarkAsLearned(wordId: Int) {
        viewModelScope.launch {
            repository.unmarkLearned(wordId)
        }
    }

    /**
     * Toggle trạng thái học của từ
     */
    fun toggleLearned(wordId: Int) {
        viewModelScope.launch {
            if (repository.isLearned(wordId)) {
                repository.unmarkLearned(wordId)
            } else {
                repository.markLearned(wordId)
            }
        }
    }

    /**
     * Kiểm tra từ đã được học chưa
     */
    fun isLearned(wordId: Int): Boolean {
        return repository.isLearned(wordId)
    }

    /**
     * Sync từ cloud
     */
    fun syncFromCloud() {
        viewModelScope.launch {
            repository.syncFromCloud()
        }
    }

    /**
     * Xóa cache khi logout
     */
    fun clearCache() {
        repository.clearLocalCache()
    }

    /**
     * Lấy số từ đã học
     */
    fun getLearnedCount(): Int {
        return repository.getLearnedCount()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LearnedWordsViewModel::class.java)) {
                return LearnedWordsViewModel(LearnedWordsRepository.getInstance(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

