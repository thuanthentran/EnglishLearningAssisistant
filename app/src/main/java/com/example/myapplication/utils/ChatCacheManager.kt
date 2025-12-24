package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Cache Manager for Chat AI features (Translation & Analysis)
 * Uses SharedPreferences for persistent storage
 */
class ChatCacheManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "chat_ai_cache",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: ChatCacheManager? = null

        fun getInstance(context: Context): ChatCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatCacheManager(context).also { INSTANCE = it }
            }
        }

        private const val KEY_TRANSLATIONS = "translations"
        private const val KEY_ANALYSES = "analyses"
        private const val MAX_CACHE_SIZE = 100 // Maximum number of cached items
    }

    /**
     * Save translation to cache
     */
    fun saveTranslation(content: String, result: TranslationResult) {
        val cache = getTranslationCache().toMutableMap()

        // Add new translation
        cache[content] = result

        // Limit cache size - remove oldest entries if exceeds limit
        if (cache.size > MAX_CACHE_SIZE) {
            val keysToRemove = cache.keys.take(cache.size - MAX_CACHE_SIZE)
            keysToRemove.forEach { cache.remove(it) }
        }

        // Save to SharedPreferences
        val json = gson.toJson(cache)
        prefs.edit().putString(KEY_TRANSLATIONS, json).apply()
    }

    /**
     * Get translation from cache
     */
    fun getTranslation(content: String): TranslationResult? {
        return getTranslationCache()[content]
    }

    /**
     * Get all translations cache
     */
    private fun getTranslationCache(): Map<String, TranslationResult> {
        val json = prefs.getString(KEY_TRANSLATIONS, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, TranslationResult>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Save message analysis to cache
     */
    fun saveAnalysis(message: String, result: MessageAnalysis) {
        val cache = getAnalysisCache().toMutableMap()

        // Add new analysis
        cache[message] = result

        // Limit cache size
        if (cache.size > MAX_CACHE_SIZE) {
            val keysToRemove = cache.keys.take(cache.size - MAX_CACHE_SIZE)
            keysToRemove.forEach { cache.remove(it) }
        }

        // Save to SharedPreferences
        val json = gson.toJson(cache)
        prefs.edit().putString(KEY_ANALYSES, json).apply()
    }

    /**
     * Get message analysis from cache
     */
    fun getAnalysis(message: String): MessageAnalysis? {
        return getAnalysisCache()[message]
    }

    /**
     * Get all analyses cache
     */
    private fun getAnalysisCache(): Map<String, MessageAnalysis> {
        val json = prefs.getString(KEY_ANALYSES, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, MessageAnalysis>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Clear all cache
     */
    fun clearAllCache() {
        prefs.edit()
            .remove(KEY_TRANSLATIONS)
            .remove(KEY_ANALYSES)
            .apply()
    }

    /**
     * Clear translation cache only
     */
    fun clearTranslationCache() {
        prefs.edit().remove(KEY_TRANSLATIONS).apply()
    }

    /**
     * Clear analysis cache only
     */
    fun clearAnalysisCache() {
        prefs.edit().remove(KEY_ANALYSES).apply()
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            translationCount = getTranslationCache().size,
            analysisCount = getAnalysisCache().size
        )
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val translationCount: Int,
    val analysisCount: Int
)

