package com.example.myapplication.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DatabaseHelper
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.data.repository.LearnedWordsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val context: Context
) : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val dbHelper = DatabaseHelper(context)
    private val userPreferences = UserPreferences(context)
    private val learnedWordsRepository = LearnedWordsRepository.getInstance(context)

    private val _loginState = MutableStateFlow(AuthState())
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthState())
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(AuthState())
    val forgotPasswordState: StateFlow<AuthState> = _forgotPasswordState.asStateFlow()

    /**
     * Đăng nhập bằng Firebase
     */
    fun loginWithFirebase(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = AuthState(isLoading = true)

            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Lấy username từ local database dựa vào email
                    // Điều này đảm bảo username luôn đúng với username đã đăng ký
                    var username = dbHelper.getUsernameByEmail(email)

                    // Nếu không tìm thấy trong database, fallback sang displayName hoặc email prefix
                    if (username == null) {
                        username = user.displayName ?: email.substringBefore("@")
                    }

                    // Lưu session
                    userPreferences.saveUserSession(username, email, rememberMe)

                    // Sync learned words từ cloud
                    learnedWordsRepository.syncFromCloud()

                    _loginState.value = AuthState(isSuccess = true)
                } else {
                    _loginState.value = AuthState(error = "Đăng nhập thất bại")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "Email chưa được đăng ký"
                    is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không đúng"
                    else -> {
                        when {
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                                "Email hoặc mật khẩu không đúng"
                            e.message?.contains("network") == true ->
                                "Lỗi kết nối mạng"
                            else -> e.message ?: "Đã có lỗi xảy ra"
                        }
                    }
                }
                _loginState.value = AuthState(error = errorMessage)
            }
        }
    }

    /**
     * Đăng ký bằng Firebase
     */
    fun registerWithFirebase(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = AuthState(isLoading = true)

            try {
                // Đăng ký với Firebase
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Cập nhật display name
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    // Lưu vào local database để backup
                    dbHelper.registerUser(username, email, password)

                    _registerState.value = AuthState(isSuccess = true)
                } else {
                    _registerState.value = AuthState(error = "Đăng ký thất bại")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu"
                    is FirebaseAuthInvalidCredentialsException -> "Email không hợp lệ"
                    is FirebaseAuthUserCollisionException -> "Email đã được sử dụng"
                    else -> {
                        when {
                            e.message?.contains("email-already-in-use") == true ->
                                "Email đã được sử dụng"
                            e.message?.contains("network") == true ->
                                "Lỗi kết nối mạng"
                            else -> e.message ?: "Đã có lỗi xảy ra"
                        }
                    }
                }
                _registerState.value = AuthState(error = errorMessage)
            }
        }
    }

    /**
     * Đăng xuất
     */
    fun logout() {
        firebaseAuth.signOut()
        userPreferences.clearUserSession()
        learnedWordsRepository.clearLocalCache()
    }

    /**
     * Kiểm tra đã đăng nhập chưa
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null || userPreferences.isLoggedIn()
    }

    /**
     * Reset states
     */
    fun resetLoginState() {
        _loginState.value = AuthState()
    }

    fun resetRegisterState() {
        _registerState.value = AuthState()
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = AuthState()
    }

    /**
     * Gửi email reset mật khẩu qua Firebase
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = AuthState(isLoading = true)

            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                _forgotPasswordState.value = AuthState(isSuccess = true)
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("user-not-found") == true ||
                    e.message?.contains("USER_NOT_FOUND") == true ->
                        "Email chưa được đăng ký"
                    e.message?.contains("invalid-email") == true ||
                    e.message?.contains("INVALID_EMAIL") == true ->
                        "Email không hợp lệ"
                    e.message?.contains("too-many-requests") == true ->
                        "Quá nhiều yêu cầu. Vui lòng thử lại sau"
                    e.message?.contains("network") == true ->
                        "Lỗi kết nối mạng"
                    else -> e.message ?: "Đã có lỗi xảy ra"
                }
                _forgotPasswordState.value = AuthState(error = errorMessage)
            }
        }
    }

    /**
     * Factory
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

