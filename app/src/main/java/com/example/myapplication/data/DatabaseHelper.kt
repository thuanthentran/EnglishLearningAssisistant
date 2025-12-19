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
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
        val hashedPassword = SecurityUtils.hashPassword(passwordRaw)

        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, hashedPassword)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
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
        // Kiểm tra mật khẩu hiện tại
        if (!checkUser(username, currentPasswordRaw)) {
            return ChangePasswordResult.WRONG_CURRENT_PASSWORD
        }

        val db = writableDatabase
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
        db.close()

        return if (rowsAffected > 0) {
            ChangePasswordResult.SUCCESS
        } else {
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
}

enum class ChangePasswordResult {
    SUCCESS,
    WRONG_CURRENT_PASSWORD,
    ERROR
}
