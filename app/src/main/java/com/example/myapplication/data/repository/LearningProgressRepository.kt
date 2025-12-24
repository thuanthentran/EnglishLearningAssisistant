package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository để quản lý tiến trình học tập với Firebase sync
 *
 * Firestore Schema:
 * users/{uid}/learning_progress/{date}
 * {
 *   date: String (yyyy-MM-dd),
 *   wordsLearned: Int,           // Số từ đã học hôm nay
 *   wordsReviewed: Int,          // Số từ đã ôn tập hôm nay
 *   quizCompleted: Int,          // Số bài quiz đã hoàn thành
 *   quizCorrect: Int,            // Số câu trả lời đúng
 *   totalStudyTimeMinutes: Int,  // Thời gian học (phút)
 *   streakDays: Int,             // Số ngày học liên tiếp
 *   lastUpdated: Timestamp
 * }
 *
 * users/{uid}/learning_stats
 * {
 *   totalWordsLearned: Int,      // Tổng số từ đã học
 *   totalWordsReviewed: Int,     // Tổng số từ đã ôn
 *   totalQuizCompleted: Int,     // Tổng số quiz
 *   totalQuizCorrect: Int,       // Tổng số câu đúng
 *   totalStudyTimeMinutes: Int,  // Tổng thời gian học
 *   currentStreak: Int,          // Streak hiện tại
 *   longestStreak: Int,          // Streak dài nhất
 *   lastStudyDate: String,       // Ngày học cuối
 *   lastUpdated: Timestamp
 * }
 */
class LearningProgressRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "LearningProgressRepo"
        private const val PREFS_NAME = "learning_progress_prefs"

        // Keys for daily progress
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_WORDS_LEARNED = "words_learned"
        private const val KEY_WORDS_REVIEWED = "words_reviewed"
        private const val KEY_QUIZ_COMPLETED = "quiz_completed"
        private const val KEY_QUIZ_CORRECT = "quiz_correct"
        private const val KEY_STUDY_TIME = "study_time_minutes"

        // Keys for stats
        private const val KEY_TOTAL_WORDS_LEARNED = "total_words_learned"
        private const val KEY_TOTAL_WORDS_REVIEWED = "total_words_reviewed"
        private const val KEY_TOTAL_QUIZ_COMPLETED = "total_quiz_completed"
        private const val KEY_TOTAL_QUIZ_CORRECT = "total_quiz_correct"
        private const val KEY_TOTAL_STUDY_TIME = "total_study_time"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_STUDY_DATE = "last_study_date"

        // Firestore collections
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_LEARNING_PROGRESS = "learning_progress"
        private const val DOC_LEARNING_STATS = "learning_stats"

        // Daily goals
        const val DAILY_WORDS_GOAL = 10
        const val DAILY_REVIEW_GOAL = 10
        const val DAILY_QUIZ_GOAL = 5

        @Volatile
        private var INSTANCE: LearningProgressRepository? = null

        fun getInstance(context: Context): LearningProgressRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearningProgressRepository(context.applicationContext).also {
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

    // State flows for UI observation
    private val _dailyProgress = MutableStateFlow(DailyProgress())
    val dailyProgress: StateFlow<DailyProgress> = _dailyProgress.asStateFlow()

    private val _learningStats = MutableStateFlow(LearningStats())
    val learningStats: StateFlow<LearningStats> = _learningStats.asStateFlow()

    init {
        loadFromLocal()
    }

    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Data class cho tiến trình hàng ngày
     */
    data class DailyProgress(
        val date: String = "",
        val wordsLearned: Int = 0,
        val wordsReviewed: Int = 0,
        val quizCompleted: Int = 0,
        val quizCorrect: Int = 0,
        val studyTimeMinutes: Int = 0
    ) {
        val wordsProgress: Float get() = (wordsLearned.toFloat() / DAILY_WORDS_GOAL).coerceIn(0f, 1f)
        val reviewProgress: Float get() = (wordsReviewed.toFloat() / DAILY_REVIEW_GOAL).coerceIn(0f, 1f)
        val quizProgress: Float get() = (quizCompleted.toFloat() / DAILY_QUIZ_GOAL).coerceIn(0f, 1f)
        val overallProgress: Float get() = ((wordsProgress + reviewProgress + quizProgress) / 3f).coerceIn(0f, 1f)
        val quizAccuracy: Float get() = if (quizCompleted > 0) quizCorrect.toFloat() / quizCompleted else 0f
    }

    /**
     * Data class cho thống kê tổng hợp
     */
    data class LearningStats(
        val totalWordsLearned: Int = 0,
        val totalWordsReviewed: Int = 0,
        val totalQuizCompleted: Int = 0,
        val totalQuizCorrect: Int = 0,
        val totalStudyTimeMinutes: Int = 0,
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val lastStudyDate: String = ""
    ) {
        val overallAccuracy: Float get() = if (totalQuizCompleted > 0) totalQuizCorrect.toFloat() / totalQuizCompleted else 0f
        val totalStudyHours: Float get() = totalStudyTimeMinutes / 60f
    }

    /**
     * Load dữ liệu từ local cache
     */
    private fun loadFromLocal() {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""

        // Reset nếu là ngày mới
        val dailyProgress = if (lastDate == today) {
            DailyProgress(
                date = today,
                wordsLearned = prefs.getInt(KEY_WORDS_LEARNED, 0),
                wordsReviewed = prefs.getInt(KEY_WORDS_REVIEWED, 0),
                quizCompleted = prefs.getInt(KEY_QUIZ_COMPLETED, 0),
                quizCorrect = prefs.getInt(KEY_QUIZ_CORRECT, 0),
                studyTimeMinutes = prefs.getInt(KEY_STUDY_TIME, 0)
            )
        } else {
            // Ngày mới, reset daily progress
            DailyProgress(date = today)
        }

        val stats = LearningStats(
            totalWordsLearned = prefs.getInt(KEY_TOTAL_WORDS_LEARNED, 0),
            totalWordsReviewed = prefs.getInt(KEY_TOTAL_WORDS_REVIEWED, 0),
            totalQuizCompleted = prefs.getInt(KEY_TOTAL_QUIZ_COMPLETED, 0),
            totalQuizCorrect = prefs.getInt(KEY_TOTAL_QUIZ_CORRECT, 0),
            totalStudyTimeMinutes = prefs.getInt(KEY_TOTAL_STUDY_TIME, 0),
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0),
            lastStudyDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        )

        _dailyProgress.value = dailyProgress
        _learningStats.value = stats
    }

    /**
     * Lưu daily progress vào local
     */
    private fun saveDailyProgressToLocal(progress: DailyProgress) {
        prefs.edit().apply {
            putString(KEY_LAST_DATE, progress.date)
            putInt(KEY_WORDS_LEARNED, progress.wordsLearned)
            putInt(KEY_WORDS_REVIEWED, progress.wordsReviewed)
            putInt(KEY_QUIZ_COMPLETED, progress.quizCompleted)
            putInt(KEY_QUIZ_CORRECT, progress.quizCorrect)
            putInt(KEY_STUDY_TIME, progress.studyTimeMinutes)
            apply()
        }
        _dailyProgress.value = progress
    }

    /**
     * Lưu stats vào local
     */
    private fun saveStatsToLocal(stats: LearningStats) {
        prefs.edit().apply {
            putInt(KEY_TOTAL_WORDS_LEARNED, stats.totalWordsLearned)
            putInt(KEY_TOTAL_WORDS_REVIEWED, stats.totalWordsReviewed)
            putInt(KEY_TOTAL_QUIZ_COMPLETED, stats.totalQuizCompleted)
            putInt(KEY_TOTAL_QUIZ_CORRECT, stats.totalQuizCorrect)
            putInt(KEY_TOTAL_STUDY_TIME, stats.totalStudyTimeMinutes)
            putInt(KEY_CURRENT_STREAK, stats.currentStreak)
            putInt(KEY_LONGEST_STREAK, stats.longestStreak)
            putString(KEY_LAST_STUDY_DATE, stats.lastStudyDate)
            apply()
        }
        _learningStats.value = stats
    }

    /**
     * Sync daily progress lên Firestore
     */
    private fun syncDailyProgressToCloud(progress: DailyProgress) {
        val uid = getCurrentUserId() ?: return

        val data = hashMapOf(
            "date" to progress.date,
            "wordsLearned" to progress.wordsLearned,
            "wordsReviewed" to progress.wordsReviewed,
            "quizCompleted" to progress.quizCompleted,
            "quizCorrect" to progress.quizCorrect,
            "studyTimeMinutes" to progress.studyTimeMinutes,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_LEARNING_PROGRESS)
            .document(progress.date)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Synced daily progress to cloud: ${progress.date}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync daily progress", e)
            }
    }

    /**
     * Sync stats lên Firestore
     */
    private fun syncStatsToCloud(stats: LearningStats) {
        val uid = getCurrentUserId() ?: return

        val data = hashMapOf(
            "totalWordsLearned" to stats.totalWordsLearned,
            "totalWordsReviewed" to stats.totalWordsReviewed,
            "totalQuizCompleted" to stats.totalQuizCompleted,
            "totalQuizCorrect" to stats.totalQuizCorrect,
            "totalStudyTimeMinutes" to stats.totalStudyTimeMinutes,
            "currentStreak" to stats.currentStreak,
            "longestStreak" to stats.longestStreak,
            "lastStudyDate" to stats.lastStudyDate,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_LEARNING_PROGRESS)
            .document(DOC_LEARNING_STATS)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Synced learning stats to cloud")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync learning stats", e)
            }
    }

    /**
     * Sync từ Firestore về local (gọi khi đăng nhập)
     */
    suspend fun syncFromCloud(): Result<Unit> {
        val uid = getCurrentUserId()
        if (uid == null) {
            Log.w(TAG, "Cannot sync: User not logged in")
            return Result.failure(Exception("User not logged in"))
        }

        val today = getTodayDateString()

        return try {
            // Sync daily progress
            val dailyDoc = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LEARNING_PROGRESS)
                .document(today)
                .get()
                .await()

            val cloudDailyProgress = if (dailyDoc.exists()) {
                DailyProgress(
                    date = today,
                    wordsLearned = dailyDoc.getLong("wordsLearned")?.toInt() ?: 0,
                    wordsReviewed = dailyDoc.getLong("wordsReviewed")?.toInt() ?: 0,
                    quizCompleted = dailyDoc.getLong("quizCompleted")?.toInt() ?: 0,
                    quizCorrect = dailyDoc.getLong("quizCorrect")?.toInt() ?: 0,
                    studyTimeMinutes = dailyDoc.getLong("studyTimeMinutes")?.toInt() ?: 0
                )
            } else {
                DailyProgress(date = today)
            }

            // Merge với local (lấy max)
            val localProgress = _dailyProgress.value
            val mergedProgress = DailyProgress(
                date = today,
                wordsLearned = maxOf(localProgress.wordsLearned, cloudDailyProgress.wordsLearned),
                wordsReviewed = maxOf(localProgress.wordsReviewed, cloudDailyProgress.wordsReviewed),
                quizCompleted = maxOf(localProgress.quizCompleted, cloudDailyProgress.quizCompleted),
                quizCorrect = maxOf(localProgress.quizCorrect, cloudDailyProgress.quizCorrect),
                studyTimeMinutes = maxOf(localProgress.studyTimeMinutes, cloudDailyProgress.studyTimeMinutes)
            )

            saveDailyProgressToLocal(mergedProgress)

            // Sync lại nếu có merge
            if (mergedProgress != cloudDailyProgress) {
                syncDailyProgressToCloud(mergedProgress)
            }

            // Sync stats
            val statsDoc = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LEARNING_PROGRESS)
                .document(DOC_LEARNING_STATS)
                .get()
                .await()

            val cloudStats = if (statsDoc.exists()) {
                LearningStats(
                    totalWordsLearned = statsDoc.getLong("totalWordsLearned")?.toInt() ?: 0,
                    totalWordsReviewed = statsDoc.getLong("totalWordsReviewed")?.toInt() ?: 0,
                    totalQuizCompleted = statsDoc.getLong("totalQuizCompleted")?.toInt() ?: 0,
                    totalQuizCorrect = statsDoc.getLong("totalQuizCorrect")?.toInt() ?: 0,
                    totalStudyTimeMinutes = statsDoc.getLong("totalStudyTimeMinutes")?.toInt() ?: 0,
                    currentStreak = statsDoc.getLong("currentStreak")?.toInt() ?: 0,
                    longestStreak = statsDoc.getLong("longestStreak")?.toInt() ?: 0,
                    lastStudyDate = statsDoc.getString("lastStudyDate") ?: ""
                )
            } else {
                LearningStats()
            }

            // Merge stats (lấy max)
            val localStats = _learningStats.value
            val mergedStats = LearningStats(
                totalWordsLearned = maxOf(localStats.totalWordsLearned, cloudStats.totalWordsLearned),
                totalWordsReviewed = maxOf(localStats.totalWordsReviewed, cloudStats.totalWordsReviewed),
                totalQuizCompleted = maxOf(localStats.totalQuizCompleted, cloudStats.totalQuizCompleted),
                totalQuizCorrect = maxOf(localStats.totalQuizCorrect, cloudStats.totalQuizCorrect),
                totalStudyTimeMinutes = maxOf(localStats.totalStudyTimeMinutes, cloudStats.totalStudyTimeMinutes),
                currentStreak = maxOf(localStats.currentStreak, cloudStats.currentStreak),
                longestStreak = maxOf(localStats.longestStreak, cloudStats.longestStreak),
                lastStudyDate = if (localStats.lastStudyDate > cloudStats.lastStudyDate) localStats.lastStudyDate else cloudStats.lastStudyDate
            )

            saveStatsToLocal(mergedStats)

            // Sync lại nếu có merge
            if (mergedStats != cloudStats) {
                syncStatsToCloud(mergedStats)
            }

            // Cập nhật streak
            updateStreak()

            Log.d(TAG, "Synced from cloud successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync from cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật streak
     */
    private fun updateStreak() {
        val today = getTodayDateString()
        val lastStudyDate = _learningStats.value.lastStudyDate
        val currentStreak = _learningStats.value.currentStreak

        if (lastStudyDate.isEmpty()) return

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { dateFormat.format(it.time) }

        val newStreak = when {
            lastStudyDate == today -> currentStreak // Đã học hôm nay
            lastStudyDate == yesterday -> currentStreak // Học hôm qua, chờ học hôm nay
            else -> 0 // Đã nghỉ > 1 ngày, reset streak
        }

        if (newStreak != currentStreak) {
            val newStats = _learningStats.value.copy(currentStreak = newStreak)
            saveStatsToLocal(newStats)
            syncStatsToCloud(newStats)
        }
    }

    /**
     * Ghi nhận học từ mới
     */
    fun recordWordLearned(count: Int = 1) {
        val today = getTodayDateString()
        val current = _dailyProgress.value.let {
            if (it.date == today) it else DailyProgress(date = today)
        }

        val newProgress = current.copy(
            wordsLearned = current.wordsLearned + count
        )
        saveDailyProgressToLocal(newProgress)
        syncDailyProgressToCloud(newProgress)

        // Cập nhật stats
        val currentStats = _learningStats.value
        val newStats = currentStats.copy(
            totalWordsLearned = currentStats.totalWordsLearned + count,
            lastStudyDate = today,
            currentStreak = if (currentStats.lastStudyDate == today) currentStats.currentStreak
                           else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                           else 1,
            longestStreak = maxOf(currentStats.longestStreak,
                                  if (currentStats.lastStudyDate == today) currentStats.currentStreak
                                  else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                                  else 1)
        )
        saveStatsToLocal(newStats)
        syncStatsToCloud(newStats)
    }

    /**
     * Ghi nhận ôn từ
     */
    fun recordWordReviewed(count: Int = 1) {
        val today = getTodayDateString()
        val current = _dailyProgress.value.let {
            if (it.date == today) it else DailyProgress(date = today)
        }

        val newProgress = current.copy(
            wordsReviewed = current.wordsReviewed + count
        )
        saveDailyProgressToLocal(newProgress)
        syncDailyProgressToCloud(newProgress)

        // Cập nhật stats
        val currentStats = _learningStats.value
        val newStats = currentStats.copy(
            totalWordsReviewed = currentStats.totalWordsReviewed + count,
            lastStudyDate = today,
            currentStreak = if (currentStats.lastStudyDate == today) currentStats.currentStreak
                           else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                           else 1,
            longestStreak = maxOf(currentStats.longestStreak,
                                  if (currentStats.lastStudyDate == today) currentStats.currentStreak
                                  else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                                  else 1)
        )
        saveStatsToLocal(newStats)
        syncStatsToCloud(newStats)
    }

    /**
     * Ghi nhận quiz
     */
    fun recordQuizResult(totalQuestions: Int, correctAnswers: Int) {
        val today = getTodayDateString()
        val current = _dailyProgress.value.let {
            if (it.date == today) it else DailyProgress(date = today)
        }

        val newProgress = current.copy(
            quizCompleted = current.quizCompleted + totalQuestions,
            quizCorrect = current.quizCorrect + correctAnswers
        )
        saveDailyProgressToLocal(newProgress)
        syncDailyProgressToCloud(newProgress)

        // Cập nhật stats
        val currentStats = _learningStats.value
        val newStats = currentStats.copy(
            totalQuizCompleted = currentStats.totalQuizCompleted + totalQuestions,
            totalQuizCorrect = currentStats.totalQuizCorrect + correctAnswers,
            lastStudyDate = today,
            currentStreak = if (currentStats.lastStudyDate == today) currentStats.currentStreak
                           else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                           else 1,
            longestStreak = maxOf(currentStats.longestStreak,
                                  if (currentStats.lastStudyDate == today) currentStats.currentStreak
                                  else if (isYesterday(currentStats.lastStudyDate)) currentStats.currentStreak + 1
                                  else 1)
        )
        saveStatsToLocal(newStats)
        syncStatsToCloud(newStats)
    }

    /**
     * Ghi nhận thời gian học
     */
    fun recordStudyTime(minutes: Int) {
        val today = getTodayDateString()
        val current = _dailyProgress.value.let {
            if (it.date == today) it else DailyProgress(date = today)
        }

        val newProgress = current.copy(
            studyTimeMinutes = current.studyTimeMinutes + minutes
        )
        saveDailyProgressToLocal(newProgress)
        syncDailyProgressToCloud(newProgress)

        // Cập nhật stats
        val currentStats = _learningStats.value
        val newStats = currentStats.copy(
            totalStudyTimeMinutes = currentStats.totalStudyTimeMinutes + minutes
        )
        saveStatsToLocal(newStats)
        syncStatsToCloud(newStats)
    }

    /**
     * Lấy lịch sử học trong 7 ngày qua
     */
    suspend fun getWeeklyProgress(): Result<List<DailyProgress>> {
        val uid = getCurrentUserId()
        if (uid == null) {
            return Result.success(emptyList())
        }

        return try {
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)

            val snapshot = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LEARNING_PROGRESS)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            val progressList = snapshot.documents.mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                DailyProgress(
                    date = date,
                    wordsLearned = doc.getLong("wordsLearned")?.toInt() ?: 0,
                    wordsReviewed = doc.getLong("wordsReviewed")?.toInt() ?: 0,
                    quizCompleted = doc.getLong("quizCompleted")?.toInt() ?: 0,
                    quizCorrect = doc.getLong("quizCorrect")?.toInt() ?: 0,
                    studyTimeMinutes = doc.getLong("studyTimeMinutes")?.toInt() ?: 0
                )
            }.sortedBy { it.date }

            Result.success(progressList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get weekly progress", e)
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra ngày hôm qua
     */
    private fun isYesterday(dateString: String): Boolean {
        if (dateString.isEmpty()) return false
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { dateFormat.format(it.time) }
        return dateString == yesterday
    }

    /**
     * Clear local data (khi logout)
     */
    fun clearLocal() {
        prefs.edit().clear().apply()
        _dailyProgress.value = DailyProgress()
        _learningStats.value = LearningStats()
        Log.d(TAG, "Cleared local learning progress data")
    }
}

