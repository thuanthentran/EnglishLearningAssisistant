package com.example.myapplication.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.myapplication.data.db.DictionaryDatabaseHelper
import com.example.myapplication.data.model.DictionaryWord

class DictionaryRepository(context: Context) {

    private val helper = DictionaryDatabaseHelper(context)
    private val db: SQLiteDatabase

    init {
        helper.copyDatabaseIfNeeded()
        db = SQLiteDatabase.openDatabase(
            helper.getDatabasePath(),
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    fun search(keyword: String, limit: Int = 50): List<DictionaryWord> {
        val list = mutableListOf<DictionaryWord>()

        try {
            val cursor = db.rawQuery(
                """
                SELECT 
                    MIN(id) as id,
                    word,
                    phonetic,
                    type,
                    definition
                FROM dictionary
                WHERE word LIKE ?
                GROUP BY word
                ORDER BY word
                LIMIT ?
                """.trimIndent(),
                arrayOf("%$keyword%", limit.toString())
            )

            while (cursor.moveToNext()) {
                list.add(
                    DictionaryWord(
                        id = cursor.getInt(0),
                        word = cursor.getString(1) ?: "",
                        phonetic = cursor.getString(2) ?: "",
                        type = cursor.getString(3) ?: "",
                        definition = cursor.getString(4) ?: ""
                    )
                )
            }
            cursor.close()

        } catch (e: Exception) {
            Log.e("DictionaryRepository", "Search error", e)
        }

        return list
    }

    /**
     * Get random words for game features
     */
    fun getRandomWords(count: Int): List<DictionaryWord> {
        val list = mutableListOf<DictionaryWord>()

        try {
            val cursor = db.rawQuery(
                """
                SELECT 
                    id,
                    word,
                    phonetic,
                    type,
                    definition
                FROM dictionary
                WHERE definition IS NOT NULL AND definition != ''
                ORDER BY RANDOM()
                LIMIT ?
                """.trimIndent(),
                arrayOf(count.toString())
            )

            while (cursor.moveToNext()) {
                list.add(
                    DictionaryWord(
                        id = cursor.getInt(0),
                        word = cursor.getString(1) ?: "",
                        phonetic = cursor.getString(2) ?: "",
                        type = cursor.getString(3) ?: "",
                        definition = cursor.getString(4) ?: ""
                    )
                )
            }
            cursor.close()

        } catch (e: Exception) {
            Log.e("DictionaryRepository", "getRandomWords error", e)
        }

        return list
    }

    /**
     * Get random definitions for wrong answers in multiple choice game
     * Excludes the correct answer's word
     */
    fun getRandomDefinitions(excludeWordId: Int, count: Int): List<String> {
        val definitions = mutableListOf<String>()

        try {
            val cursor = db.rawQuery(
                """
                SELECT definition
                FROM dictionary
                WHERE id != ? AND definition IS NOT NULL AND definition != ''
                ORDER BY RANDOM()
                LIMIT ?
                """.trimIndent(),
                arrayOf(excludeWordId.toString(), count.toString())
            )

            while (cursor.moveToNext()) {
                val definition = cursor.getString(0) ?: ""
                if (definition.isNotBlank()) {
                    definitions.add(definition)
                }
            }
            cursor.close()

        } catch (e: Exception) {
            Log.e("DictionaryRepository", "getRandomDefinitions error", e)
        }

        return definitions
    }

    /**
     * Get words by their IDs
     */
    fun getWordsByIds(ids: Set<Int>): List<DictionaryWord> {
        if (ids.isEmpty()) return emptyList()

        val list = mutableListOf<DictionaryWord>()

        try {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()

            val cursor = db.rawQuery(
                """
                SELECT 
                    id,
                    word,
                    phonetic,
                    type,
                    definition
                FROM dictionary
                WHERE id IN ($placeholders)
                """.trimIndent(),
                args
            )

            while (cursor.moveToNext()) {
                list.add(
                    DictionaryWord(
                        id = cursor.getInt(0),
                        word = cursor.getString(1) ?: "",
                        phonetic = cursor.getString(2) ?: "",
                        type = cursor.getString(3) ?: "",
                        definition = cursor.getString(4) ?: ""
                    )
                )
            }
            cursor.close()

        } catch (e: Exception) {
            Log.e("DictionaryRepository", "getWordsByIds error", e)
        }

        return list
    }
}
