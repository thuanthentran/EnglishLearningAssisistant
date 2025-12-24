package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository để quản lý từ đã học với Firebase sync
 *
 * Firestore Schema:
 * users/{uid}/learned_words/{wordId}
 * {
 *   learnedAt: Timestamp
 * }
 */
class LearnedWordsRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "LearnedWordsRepository"
        private const val PREFS_NAME = "learned_words_prefs"
        private const val KEY_LEARNED_WORDS = "learned_word_ids"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_LEARNED_WORDS = "learned_words"

        @Volatile
        private var INSTANCE: LearnedWordsRepository? = null

        fun getInstance(context: Context): LearnedWordsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearnedWordsRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    // State flow để UI có thể observe learned words
    private val _learnedWordIds = MutableStateFlow<Set<Int>>(emptySet())
    val learnedWordIds: StateFlow<Set<Int>> = _learnedWordIds.asStateFlow()

    init {
        // Load từ local cache khi khởi tạo
        loadFromLocalCache()
    }

    /**
     * Lấy UID của user hiện tại
     */
    private fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Load learned words từ local cache
     */
    private fun loadFromLocalCache() {
        val savedIds = prefs.getStringSet(KEY_LEARNED_WORDS, emptySet()) ?: emptySet()
        _learnedWordIds.value = savedIds.mapNotNull { it.toIntOrNull() }.toSet()
        Log.d(TAG, "Loaded ${_learnedWordIds.value.size} learned words from local cache")
    }

    /**
     * Lưu learned words vào local cache
     */
    private fun saveToLocalCache(wordIds: Set<Int>) {
        val stringSet = wordIds.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_LEARNED_WORDS, stringSet).apply()
        _learnedWordIds.value = wordIds
        Log.d(TAG, "Saved ${wordIds.size} learned words to local cache")
    }

    /**
     * Sync learned words từ Firestore về local
     * Gọi sau khi user đăng nhập
     */
    suspend fun syncFromCloud(): Result<Unit> {
        val uid = getCurrentUserId()
        Log.d(TAG, "syncFromCloud called, uid = $uid")

        if (uid == null) {
            Log.w(TAG, "Cannot sync: User not logged in")
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            Log.d(TAG, "Fetching from: users/$uid/learned_words")
            val snapshot = firestore
                .collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LEARNED_WORDS)
                .get()
                .await()

            Log.d(TAG, "Got ${snapshot.documents.size} documents from Firestore")

            val wordIds = snapshot.documents.mapNotNull { doc ->
                Log.d(TAG, "Document: ${doc.id}")
                doc.id.toIntOrNull()
            }.toSet()

            saveToLocalCache(wordIds)
            Log.d(TAG, "Synced ${wordIds.size} learned words from cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync from cloud", e)
            Result.failure(e)
        }
    }

    /**
     * Đánh dấu một từ là đã học
     * Lưu vào cả Firestore và local cache
     */
    suspend fun markLearned(wordId: Int): Result<Unit> {
        val uid = getCurrentUserId()
        Log.d(TAG, "markLearned called: wordId=$wordId, uid=$uid")

        // Cập nhật local cache ngay lập tức
        val currentIds = _learnedWordIds.value.toMutableSet()
        currentIds.add(wordId)
        saveToLocalCache(currentIds)

        // Nếu user đã đăng nhập, sync lên Firestore
        if (uid != null) {
            return try {
                val path = "users/$uid/learned_words/$wordId"
                Log.d(TAG, "Writing to Firestore: $path")

                val docRef = firestore
                    .collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_LEARNED_WORDS)
                    .document(wordId.toString())

                docRef.set(
                    mapOf("learnedAt" to FieldValue.serverTimestamp())
                ).await()

                Log.d(TAG, "Successfully marked word $wordId as learned in cloud")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark word $wordId as learned in cloud: ${e.message}", e)
                // Vẫn trả về success vì đã lưu local
                Result.success(Unit)
            }
        } else {
            Log.w(TAG, "User not logged in, only saved to local cache")
        }

        return Result.success(Unit)
    }

    /**
     * Bỏ đánh dấu một từ đã học
     */
    suspend fun unmarkLearned(wordId: Int): Result<Unit> {
        val uid = getCurrentUserId()

        // Cập nhật local cache ngay lập tức
        val currentIds = _learnedWordIds.value.toMutableSet()
        currentIds.remove(wordId)
        saveToLocalCache(currentIds)

        // Nếu user đã đăng nhập, xóa trên Firestore
        if (uid != null) {
            return try {
                firestore
                    .collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_LEARNED_WORDS)
                    .document(wordId.toString())
                    .delete()
                    .await()

                Log.d(TAG, "Unmarked word $wordId in cloud")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unmark word $wordId in cloud", e)
                Result.success(Unit)
            }
        }

        return Result.success(Unit)
    }

    /**
     * Kiểm tra từ đã được học chưa
     */
    fun isLearned(wordId: Int): Boolean {
        return _learnedWordIds.value.contains(wordId)
    }

    /**
     * Lấy tất cả word IDs đã học
     */
    fun getLearnedWordIds(): Set<Int> {
        return _learnedWordIds.value
    }

    /**
     * Xóa local cache khi user đăng xuất
     */
    fun clearLocalCache() {
        prefs.edit().remove(KEY_LEARNED_WORDS).apply()
        _learnedWordIds.value = emptySet()
        Log.d(TAG, "Cleared local learned words cache")
    }

    /**
     * Alias for clearLocalCache (for consistency)
     */
    fun clearLocal() {
        clearLocalCache()
    }

    /**
     * Lấy số lượng từ đã học
     */
    fun getLearnedCount(): Int {
        return _learnedWordIds.value.size
    }
}

