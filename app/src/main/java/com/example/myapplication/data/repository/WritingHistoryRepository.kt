package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.data.model.WritingAIMode
import com.example.myapplication.data.model.WritingExamType
import com.example.myapplication.data.model.WritingFeedbackResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository for storing and retrieving Writing Practice history
 * Uses SharedPreferences for persistence
 */
class WritingHistoryRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "writing_practice_history"
        private const val KEY_HISTORY = "feedback_history"
        private const val MAX_HISTORY_ITEMS = 50 // Limit history size
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save a feedback result to history
     */
    fun saveFeedback(result: WritingFeedbackResult) {
        val history = getHistory().toMutableList()

        // Add new item at the beginning
        history.add(0, result)

        // Limit history size
        if (history.size > MAX_HISTORY_ITEMS) {
            history.removeAt(history.size - 1)
        }

        saveHistory(history)
    }

    /**
     * Get all feedback history
     */
    fun getHistory(): List<WritingFeedbackResult> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<WritingHistoryItem>>() {}.type
            val items: List<WritingHistoryItem> = gson.fromJson(json, type)
            items.map { it.toFeedbackResult() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a specific history item by timestamp
     */
    fun deleteHistoryItem(timestamp: Long) {
        val history = getHistory().toMutableList()
        history.removeAll { it.timestamp == timestamp }
        saveHistory(history)
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    /**
     * Save history list to SharedPreferences
     */
    private fun saveHistory(history: List<WritingFeedbackResult>) {
        val items = history.map { WritingHistoryItem.fromFeedbackResult(it) }
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}

/**
 * Data class for serializing WritingFeedbackResult to JSON
 * We need this because enums need special handling
 */
data class WritingHistoryItem(
    val examType: String,
    val aiMode: String,
    val prompt: String,
    val userEssay: String?,
    val feedback: String,
    val timestamp: Long
) {
    fun toFeedbackResult(): WritingFeedbackResult {
        return WritingFeedbackResult(
            examType = WritingExamType.valueOf(examType),
            aiMode = WritingAIMode.valueOf(aiMode),
            prompt = prompt,
            userEssay = userEssay,
            feedback = feedback,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromFeedbackResult(result: WritingFeedbackResult): WritingHistoryItem {
            return WritingHistoryItem(
                examType = result.examType.name,
                aiMode = result.aiMode.name,
                prompt = result.prompt,
                userEssay = result.userEssay,
                feedback = result.feedback,
                timestamp = result.timestamp
            )
        }
    }
}

