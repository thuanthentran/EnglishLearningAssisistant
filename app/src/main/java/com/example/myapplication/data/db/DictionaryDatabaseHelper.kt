package com.example.myapplication.data.db

import android.content.Context
import java.io.FileOutputStream

class DictionaryDatabaseHelper(private val context: Context) {

    private val dbName = "dictionary.db"

    fun copyDatabaseIfNeeded() {
        val dbFile = context.getDatabasePath(dbName)

        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open(dbName).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun getDatabasePath(): String = context.getDatabasePath(dbName).path
}
