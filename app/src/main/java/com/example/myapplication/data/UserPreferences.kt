package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AVATAR_INDEX = "avatar_index"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR_URI = "avatar_uri"
    }

    fun saveUserSession(username: String, email: String, rememberMe: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    fun clearUserSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }

    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUsername(): String? =
        prefs.getString(KEY_USERNAME, null)

    fun getEmail(): String? =
        prefs.getString(KEY_EMAIL, null)

    // Dark Mode
    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun isDarkMode(): Boolean =
        prefs.getBoolean(KEY_DARK_MODE, false)

    // Avatar
    fun setAvatarIndex(index: Int) {
        prefs.edit().putInt(KEY_AVATAR_INDEX, index).apply()
    }

    fun getAvatarIndex(): Int =
        prefs.getInt(KEY_AVATAR_INDEX, 0)

    // Nickname
    fun setNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun getNickname(): String? =
        prefs.getString(KEY_NICKNAME, null)

    // Avatar URI (from gallery)
    fun setAvatarUri(uri: String?) {
        prefs.edit().putString(KEY_AVATAR_URI, uri).apply()
    }

    fun getAvatarUri(): String? =
        prefs.getString(KEY_AVATAR_URI, null)
}
