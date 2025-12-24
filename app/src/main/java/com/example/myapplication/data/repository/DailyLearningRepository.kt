package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository để quản lý tiến độ học hàng ngày với Firebase sync
 *
 * Firestore Schema:
 * users/{uid}/daily_learning/{date}
 * {
 *   date: String (yyyy-MM-dd),
 *   wordsLearnedToday: Int,
 *   todayLearnedWordIds: List<Int>,
 *   lastUpdated: Timestamp
 * }
 */
class DailyLearningRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "DailyLearningRepository"
        private const val PREFS_NAME = "daily_learning_prefs"
        private const val KEY_LAST_LEARN_DATE = "last_learn_date"
        private const val KEY_WORDS_LEARNED_TODAY = "words_learned_today"
        private const val KEY_TODAY_LEARNED_WORD_IDS = "today_learned_word_ids"

        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_DAILY_LEARNING = "daily_learning"

        @Volatile
        private var INSTANCE: DailyLearningRepository? = null

        fun getInstance(context: Context): DailyLearningRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DailyLearningRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Data class cho daily learning progress
     */
    data class DailyProgress(
        val date: String,
        val wordsLearnedToday: Int,
        val todayLearnedWordIds: Set<Int>
    )

    /**
     * Load tiến độ học hàng ngày từ local
     */
    fun loadFromLocal(): DailyProgress {
        val today = getTodayDateString()
        val lastLearnDate = prefs.getString(KEY_LAST_LEARN_DATE, "") ?: ""

        return if (lastLearnDate == today) {
            val wordsLearnedToday = prefs.getInt(KEY_WORDS_LEARNED_TODAY, 0)
            val savedIds = prefs.getStringSet(KEY_TODAY_LEARNED_WORD_IDS, emptySet()) ?: emptySet()
            val todayLearnedWordIds = savedIds.mapNotNull { it.toIntOrNull() }.toSet()

            // Fix inconsistent state
            val fixedWordsCount = if (wordsLearnedToday > 0 && todayLearnedWordIds.isEmpty()) {
                0
            } else {
                todayLearnedWordIds.size
            }

            DailyProgress(today, fixedWordsCount, todayLearnedWordIds)
        } else {
            // Ngày mới, reset
            saveToLocal(today, 0, emptySet())
            DailyProgress(today, 0, emptySet())
        }
    }

    /**
     * Lưu tiến độ học vào local
     */
    fun saveToLocal(date: String, wordsLearnedToday: Int, todayLearnedWordIds: Set<Int>) {
        prefs.edit().apply {
            putString(KEY_LAST_LEARN_DATE, date)
            putInt(KEY_WORDS_LEARNED_TODAY, wordsLearnedToday)
            putStringSet(KEY_TODAY_LEARNED_WORD_IDS, todayLearnedWordIds.map { it.toString() }.toSet())
            apply()
        }
        Log.d(TAG, "Saved to local: date=$date, words=$wordsLearnedToday, ids=${todayLearnedWordIds.size}")
    }

    /**
     * Thêm từ đã học hôm nay
     */
    fun addLearnedWord(wordId: Int): DailyProgress {
        val today = getTodayDateString()
        val current = loadFromLocal()

        val newIds = current.todayLearnedWordIds + wordId
        val newCount = newIds.size

        saveToLocal(today, newCount, newIds)

        // Sync lên cloud nếu đăng nhập
        syncToCloud(today, newCount, newIds)

        return DailyProgress(today, newCount, newIds)
    }

    /**
     * Sync tiến độ lên Firestore
     */
    private fun syncToCloud(date: String, wordsLearnedToday: Int, todayLearnedWordIds: Set<Int>) {
        val uid = getCurrentUserId() ?: return

        val data = hashMapOf(
            "date" to date,
            "wordsLearnedToday" to wordsLearnedToday,
            "todayLearnedWordIds" to todayLearnedWordIds.toList(),
            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_DAILY_LEARNING)
            .document(date)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Synced daily progress to cloud: $date")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync daily progress", e)
            }
    }

    /**
     * Sync tiến độ từ Firestore về local (gọi khi đăng nhập)
     */
    suspend fun syncFromCloud(): Result<DailyProgress> {
        val uid = getCurrentUserId()
        if (uid == null) {
            Log.w(TAG, "Cannot sync: User not logged in")
            return Result.failure(Exception("User not logged in"))
        }

        val today = getTodayDateString()

        return try {
            Log.d(TAG, "Syncing daily progress from cloud for date: $today")

            val document = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_DAILY_LEARNING)
                .document(today)
                .get()
                .await()

            if (document.exists()) {
                val wordsLearnedToday = document.getLong("wordsLearnedToday")?.toInt() ?: 0
                val idsList = document.get("todayLearnedWordIds") as? List<*> ?: emptyList<Any>()
                val todayLearnedWordIds = idsList.mapNotNull {
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        is String -> it.toIntOrNull()
                        else -> null
                    }
                }.toSet()

                // Merge với local (lấy union của cả 2)
                val localProgress = loadFromLocal()
                val mergedIds = localProgress.todayLearnedWordIds + todayLearnedWordIds
                val mergedCount = mergedIds.size

                saveToLocal(today, mergedCount, mergedIds)

                // Sync lại nếu có merge
                if (mergedIds != todayLearnedWordIds) {
                    syncToCloud(today, mergedCount, mergedIds)
                }

                Log.d(TAG, "Synced from cloud: $mergedCount words learned today")
                Result.success(DailyProgress(today, mergedCount, mergedIds))
            } else {
                // Không có dữ liệu trên cloud, sync local lên
                val localProgress = loadFromLocal()
                if (localProgress.todayLearnedWordIds.isNotEmpty()) {
                    syncToCloud(today, localProgress.wordsLearnedToday, localProgress.todayLearnedWordIds)
                }
                Log.d(TAG, "No cloud data for today, using local")
                Result.success(localProgress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync from cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Clear local data (khi logout)
     */
    fun clearLocal() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared local daily learning data")
    }

    /**
     * Lấy danh sách từ đã học trong tuần này (từ Thứ 2 đến Chủ nhật)
     */
    suspend fun getWeekLearnedWords(): Result<Set<Int>> {
        val uid = getCurrentUserId()
        if (uid == null) {
            Log.w(TAG, "Cannot get week words: User not logged in")
            return Result.success(emptySet())
        }

        return try {
            val calendar = Calendar.getInstance()
            // Lấy ngày Thứ 2 đầu tuần
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val startOfWeek = dateFormat.format(calendar.time)

            // Lấy ngày Chủ nhật cuối tuần
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val endOfWeek = dateFormat.format(calendar.time)

            Log.d(TAG, "Fetching week words from $startOfWeek to $endOfWeek")

            val snapshot = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_DAILY_LEARNING)
                .whereGreaterThanOrEqualTo("date", startOfWeek)
                .whereLessThanOrEqualTo("date", endOfWeek)
                .get()
                .await()

            val allWordIds = mutableSetOf<Int>()
            snapshot.documents.forEach { doc ->
                val idsList = doc.get("todayLearnedWordIds") as? List<*> ?: emptyList<Any>()
                val wordIds = idsList.mapNotNull {
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        is String -> it.toIntOrNull()
                        else -> null
                    }
                }
                allWordIds.addAll(wordIds)
            }

            Log.d(TAG, "Found ${allWordIds.size} words learned this week")
            Result.success(allWordIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get week learned words", e)
            Result.failure(e)
        }
    }
}

