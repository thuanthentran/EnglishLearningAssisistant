package com.example.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.myapplication.utils.SecurityUtils

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"

        // Homework table
        const val TABLE_HOMEWORK = "homework"
        const val HOMEWORK_ID = "id"
        const val HOMEWORK_USERNAME = "username"
        const val HOMEWORK_IMAGE_PATH = "image_path"
        const val HOMEWORK_RECOGNIZED_TEXT = "recognized_text"
        const val HOMEWORK_SOLUTION = "solution"
        const val HOMEWORK_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        val createHomeworkTable = """
            CREATE TABLE $TABLE_HOMEWORK (
                $HOMEWORK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $HOMEWORK_USERNAME TEXT NOT NULL,
                $HOMEWORK_IMAGE_PATH TEXT NOT NULL,
                $HOMEWORK_RECOGNIZED_TEXT TEXT,
                $HOMEWORK_SOLUTION TEXT,
                $HOMEWORK_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($HOMEWORK_USERNAME) REFERENCES $TABLE_USERS($COLUMN_USERNAME)
            )
        """.trimIndent()
        db.execSQL(createHomeworkTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HOMEWORK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun isUsernameExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(username)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    fun registerUser(username: String, email: String, passwordRaw: String): Boolean {
        if (isUsernameExists(username) || isEmailExists(email)) return false

        val db = writableDatabase
        val hashedPassword = SecurityUtils.hashPassword(passwordRaw)

        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, hashedPassword)
        }

        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    fun checkUser(username: String, passwordRaw: String): Boolean {
        val db = readableDatabase
        return try {
            val hashedPassword = SecurityUtils.hashPassword(passwordRaw)
            android.util.Log.d("DatabaseHelper", "checkUser: username='$username', hashedPassword='$hashedPassword'")

            val cursor = db.rawQuery(
                "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(username, hashedPassword)
            )
            try {
                val found = cursor.moveToFirst()
                android.util.Log.d("DatabaseHelper", "checkUser: User found=$found")
                found
            } finally {
                cursor.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "checkUser: Exception", e)
            false
        }
    }

    fun getUserEmail(username: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_EMAIL FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(username)
        )
        val email = if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
        } else null

        cursor.close()
        db.close()
        return email
    }

    // Đổi mật khẩu - kiểm tra mật khẩu hiện tại và cập nhật mật khẩu mới
    fun changePassword(username: String, currentPasswordRaw: String, newPasswordRaw: String): ChangePasswordResult {
        // Kiểm tra username không rỗng
        if (username.isEmpty()) {
            android.util.Log.e("DatabaseHelper", "changePassword: username is empty")
            return ChangePasswordResult.ERROR
        }

        // Kiểm tra xem user có tồn tại không
        if (!isUsernameExists(username)) {
            android.util.Log.e("DatabaseHelper", "changePassword: username '$username' does not exist")
            return ChangePasswordResult.WRONG_CURRENT_PASSWORD
        }

        android.util.Log.d("DatabaseHelper", "changePassword: Checking password for username '$username'")

        // Kiểm tra mật khẩu hiện tại
        if (!checkUser(username, currentPasswordRaw)) {
            android.util.Log.e("DatabaseHelper", "changePassword: Wrong current password for username '$username'")
            return ChangePasswordResult.WRONG_CURRENT_PASSWORD
        }

        val db = writableDatabase
        return try {
            val hashedNewPassword = SecurityUtils.hashPassword(newPasswordRaw)

            val values = ContentValues().apply {
                put(COLUMN_PASSWORD, hashedNewPassword)
            }

            val rowsAffected = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_USERNAME = ?",
                arrayOf(username)
            )

            if (rowsAffected > 0) {
                android.util.Log.d("DatabaseHelper", "changePassword: Password changed successfully for username '$username'")
                ChangePasswordResult.SUCCESS
            } else {
                android.util.Log.e("DatabaseHelper", "changePassword: No rows affected for username '$username'")
                ChangePasswordResult.ERROR
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "changePassword: Exception", e)
            ChangePasswordResult.ERROR
        }
    }

    // Đặt lại mật khẩu theo email (cho quên mật khẩu)
    fun resetPasswordByEmail(email: String, newPasswordRaw: String): Boolean {
        if (!isEmailExists(email)) return false

        val db = writableDatabase
        val hashedNewPassword = SecurityUtils.hashPassword(newPasswordRaw)

        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, hashedNewPassword)
        }

        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        db.close()

        return rowsAffected > 0
    }

    // Lấy username theo email
    fun getUsernameByEmail(email: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_USERNAME FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        val username = if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
        } else null

        cursor.close()
        db.close()
        return username
    }

    // ===== HOMEWORK FUNCTIONS =====

    // Lưu bài tập mới
    fun saveHomework(
        username: String,
        imagePath: String,
        recognizedText: String,
        solution: String
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(HOMEWORK_USERNAME, username)
            put(HOMEWORK_IMAGE_PATH, imagePath)
            put(HOMEWORK_RECOGNIZED_TEXT, recognizedText)
            put(HOMEWORK_SOLUTION, solution)
            put(HOMEWORK_CREATED_AT, System.currentTimeMillis())
        }
        val result = db.insert(TABLE_HOMEWORK, null, values)
        db.close()
        return result
    }

    // Lấy tất cả bài tập của một user, sắp xếp theo thời gian mới nhất trước
    fun getAllHomeworkByUsername(username: String): List<HomeworkItem> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_HOMEWORK WHERE $HOMEWORK_USERNAME = ? ORDER BY $HOMEWORK_CREATED_AT DESC",
            arrayOf(username)
        )

        val homeworkList = mutableListOf<HomeworkItem>()
        while (cursor.moveToNext()) {
            homeworkList.add(
                HomeworkItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(HOMEWORK_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_USERNAME)),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_IMAGE_PATH)),
                    recognizedText = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_RECOGNIZED_TEXT)),
                    solution = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_SOLUTION)),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(HOMEWORK_CREATED_AT))
                )
            )
        }
        cursor.close()
        db.close()
        return homeworkList
    }

    // Lấy bài tập theo ID
    fun getHomeworkById(id: Long): HomeworkItem? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_HOMEWORK WHERE $HOMEWORK_ID = ?",
            arrayOf(id.toString())
        )

        val homework = if (cursor.moveToFirst()) {
            HomeworkItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(HOMEWORK_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_USERNAME)),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_IMAGE_PATH)),
                recognizedText = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_RECOGNIZED_TEXT)),
                solution = cursor.getString(cursor.getColumnIndexOrThrow(HOMEWORK_SOLUTION)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(HOMEWORK_CREATED_AT))
            )
        } else null

        cursor.close()
        db.close()
        return homework
    }

    // Xóa bài tập theo ID
    fun deleteHomework(id: Long): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(TABLE_HOMEWORK, "$HOMEWORK_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsAffected > 0
    }

    // Xóa tất cả bài tập của một user
    fun deleteAllHomeworkByUsername(username: String): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(TABLE_HOMEWORK, "$HOMEWORK_USERNAME = ?", arrayOf(username))
        db.close()
        return rowsAffected > 0
    }

    // Cập nhật bài tập
    fun updateHomework(
        id: Long,
        imagePath: String,
        recognizedText: String,
        solution: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(HOMEWORK_IMAGE_PATH, imagePath)
            put(HOMEWORK_RECOGNIZED_TEXT, recognizedText)
            put(HOMEWORK_SOLUTION, solution)
            put(HOMEWORK_CREATED_AT, System.currentTimeMillis())
        }
        val rowsAffected = db.update(TABLE_HOMEWORK, values, "$HOMEWORK_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsAffected > 0
    }

    // Lấy số lượng bài tập của một user
    fun getHomeworkCountByUsername(username: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_HOMEWORK WHERE $HOMEWORK_USERNAME = ?",
            arrayOf(username)
        )
        val count = if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else 0
        cursor.close()
        db.close()
        return count
    }
}

// Data class cho Homework
data class HomeworkItem(
    val id: Long,
    val username: String,
    val imagePath: String,
    val recognizedText: String,
    val solution: String,
    val createdAt: Long
)

enum class ChangePasswordResult {
    SUCCESS,
    WRONG_CURRENT_PASSWORD,
    ERROR
}
