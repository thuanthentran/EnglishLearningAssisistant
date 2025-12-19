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
}
