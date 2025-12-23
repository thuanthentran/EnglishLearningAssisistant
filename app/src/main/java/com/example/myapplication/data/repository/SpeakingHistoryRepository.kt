package com.example.myapplication.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.myapplication.data.model.SpeakingFeedbackResult
import com.example.myapplication.data.model.SpeakingExamType
import com.google.gson.Gson

/**
 * Repository for managing Speaking Practice history storage
 */
class SpeakingHistoryRepository(context: Context) {

    private val dbHelper = SpeakingHistoryDatabaseHelper(context)
    private val gson = Gson()

    companion object {
        private const val TAG = "SpeakingHistoryRepo"
    }

    /**
     * Save speaking feedback result to database
     */
    fun saveFeedbackResult(result: SpeakingFeedbackResult, username: String? = null): Boolean {
        return try {
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(SpeakingHistoryDatabaseHelper.COLUMN_USERNAME, username ?: "guest")
                put(SpeakingHistoryDatabaseHelper.COLUMN_EXAM_TYPE, result.examType.name)
                put(SpeakingHistoryDatabaseHelper.COLUMN_PROMPT, result.prompt)
                put(SpeakingHistoryDatabaseHelper.COLUMN_TRANSCRIBED_TEXT, result.transcribedText)
                put(SpeakingHistoryDatabaseHelper.COLUMN_FEEDBACK, result.feedback)
                put(SpeakingHistoryDatabaseHelper.COLUMN_OVERALL_SCORE, result.overallScore)
                put(SpeakingHistoryDatabaseHelper.COLUMN_TIMESTAMP, result.timestamp)
            }

            val id = db.insert(SpeakingHistoryDatabaseHelper.TABLE_SPEAKING_HISTORY, null, values)
            db.close()

            if (id != -1L) {
                Log.d(TAG, "Speaking history saved successfully with ID: $id")
                true
            } else {
                Log.e(TAG, "Failed to save speaking history")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving speaking history", e)
            false
        }
    }

    /**
     * Get all speaking history for a user
     */
    fun getHistory(username: String? = null): List<SpeakingFeedbackResult> {
        return try {
            val db = dbHelper.readableDatabase
            val results = mutableListOf<SpeakingFeedbackResult>()

            val whereClause = "${SpeakingHistoryDatabaseHelper.COLUMN_USERNAME} = ?"
            val whereArgs = arrayOf(username ?: "guest")

            val cursor = db.query(
                SpeakingHistoryDatabaseHelper.TABLE_SPEAKING_HISTORY,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                "${SpeakingHistoryDatabaseHelper.COLUMN_TIMESTAMP} DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    try {
                        val examTypeName = it.getString(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_EXAM_TYPE))
                        val examType = SpeakingExamType.valueOf(examTypeName)

                        val result = SpeakingFeedbackResult(
                            examType = examType,
                            prompt = it.getString(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_PROMPT)),
                            transcribedText = it.getString(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_TRANSCRIBED_TEXT)),
                            feedback = it.getString(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_FEEDBACK)),
                            overallScore = it.getInt(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_OVERALL_SCORE)).let { score ->
                                if (score == 0 && it.isNull(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_OVERALL_SCORE))) null else score
                            },
                            timestamp = it.getLong(it.getColumnIndexOrThrow(SpeakingHistoryDatabaseHelper.COLUMN_TIMESTAMP))
                        )
                        results.add(result)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing speaking history item", e)
                    }
                }
            }

            db.close()
            Log.d(TAG, "Retrieved ${results.size} speaking history items")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error getting speaking history", e)
            emptyList()
        }
    }

    /**
     * Delete a specific speaking history item by timestamp
     */
    fun deleteHistoryItem(timestamp: Long, username: String? = null): Boolean {
        return try {
            val db = dbHelper.writableDatabase

            val whereClause = "${SpeakingHistoryDatabaseHelper.COLUMN_TIMESTAMP} = ? AND ${SpeakingHistoryDatabaseHelper.COLUMN_USERNAME} = ?"
            val whereArgs = arrayOf(timestamp.toString(), username ?: "guest")

            val deletedRows = db.delete(
                SpeakingHistoryDatabaseHelper.TABLE_SPEAKING_HISTORY,
                whereClause,
                whereArgs
            )

            db.close()

            if (deletedRows > 0) {
                Log.d(TAG, "Speaking history item deleted successfully")
                true
            } else {
                Log.w(TAG, "No speaking history item found to delete")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting speaking history item", e)
            false
        }
    }

    /**
     * Clear all speaking history for a user
     */
    fun clearAllHistory(username: String? = null): Boolean {
        return try {
            val db = dbHelper.writableDatabase

            val whereClause = "${SpeakingHistoryDatabaseHelper.COLUMN_USERNAME} = ?"
            val whereArgs = arrayOf(username ?: "guest")

            val deletedRows = db.delete(
                SpeakingHistoryDatabaseHelper.TABLE_SPEAKING_HISTORY,
                whereClause,
                whereArgs
            )

            db.close()

            Log.d(TAG, "Cleared $deletedRows speaking history items")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing speaking history", e)
            false
        }
    }

    /**
     * Get speaking statistics for a user
     */
    fun getStatistics(username: String? = null): SpeakingStatistics {
        return try {
            val db = dbHelper.readableDatabase

            val whereClause = "${SpeakingHistoryDatabaseHelper.COLUMN_USERNAME} = ?"
            val whereArgs = arrayOf(username ?: "guest")

            val cursor = db.query(
                SpeakingHistoryDatabaseHelper.TABLE_SPEAKING_HISTORY,
                arrayOf(
                    "COUNT(*) as total_attempts",
                    "AVG(${SpeakingHistoryDatabaseHelper.COLUMN_OVERALL_SCORE}) as avg_score",
                    "MAX(${SpeakingHistoryDatabaseHelper.COLUMN_OVERALL_SCORE}) as max_score"
                ),
                whereClause,
                whereArgs,
                null,
                null,
                null
            )

            var stats = SpeakingStatistics()

            cursor.use {
                if (it.moveToFirst()) {
                    stats = SpeakingStatistics(
                        totalAttempts = it.getInt(0),
                        averageScore = it.getDouble(1),
                        maxScore = it.getInt(2)
                    )
                }
            }

            db.close()
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error getting speaking statistics", e)
            SpeakingStatistics()
        }
    }
}

/**
 * Data class for speaking statistics
 */
data class SpeakingStatistics(
    val totalAttempts: Int = 0,
    val averageScore: Double = 0.0,
    val maxScore: Int = 0
)

/**
 * Database helper for Speaking Practice history
 */
private class SpeakingHistoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "SpeakingHistory.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_SPEAKING_HISTORY = "speaking_history"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_EXAM_TYPE = "exam_type"
        const val COLUMN_PROMPT = "prompt"
        const val COLUMN_TRANSCRIBED_TEXT = "transcribed_text"
        const val COLUMN_FEEDBACK = "feedback"
        const val COLUMN_OVERALL_SCORE = "overall_score"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_SPEAKING_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT NOT NULL DEFAULT 'guest',
                $COLUMN_EXAM_TYPE TEXT NOT NULL,
                $COLUMN_PROMPT TEXT NOT NULL,
                $COLUMN_TRANSCRIBED_TEXT TEXT NOT NULL,
                $COLUMN_FEEDBACK TEXT NOT NULL,
                $COLUMN_OVERALL_SCORE INTEGER,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)

        // Create index for faster queries
        db.execSQL("CREATE INDEX idx_username_timestamp ON $TABLE_SPEAKING_HISTORY ($COLUMN_USERNAME, $COLUMN_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SPEAKING_HISTORY")
        onCreate(db)
    }
}
