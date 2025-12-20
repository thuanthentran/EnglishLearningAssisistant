package com.example.myapplication.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State cho đổi mật khẩu
data class PasswordChangeState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

// State cho quên mật khẩu
data class ForgotPasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _passwordChangeState = MutableStateFlow(PasswordChangeState())
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    /**
     * Đổi mật khẩu người dùng qua Firebase
     * @param currentPassword Mật khẩu hiện tại
     * @param newPassword Mật khẩu mới
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _passwordChangeState.value = PasswordChangeState(isLoading = true)

            authRepository.changePasswordFirebase(currentPassword, newPassword)
                .onSuccess {
                    _passwordChangeState.value = PasswordChangeState(isSuccess = true)
                }
                .onFailure { e ->
                    val errorMessage = e.message ?: "Đã có lỗi xảy ra"
                    _passwordChangeState.value = PasswordChangeState(error = errorMessage)
                }
        }
    }

    /**
     * Gửi email reset mật khẩu qua Firebase
     * Firebase sẽ tự động gửi email với link để user đặt lại mật khẩu
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState(isLoading = true)

            authRepository.sendPasswordResetEmailFirebase(email)
                .onSuccess {
                    _forgotPasswordState.value = ForgotPasswordState(isSuccess = true)
                }
                .onFailure { e ->
                    _forgotPasswordState.value = ForgotPasswordState(error = e.message ?: "Đã có lỗi xảy ra")
                }
        }
    }

    /**
     * Reset state đổi mật khẩu
     */
    fun resetPasswordChangeState() {
        _passwordChangeState.value = PasswordChangeState()
    }

    /**
     * Reset state quên mật khẩu
     */
    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState()
    }

    /**
     * Factory để tạo ViewModel với dependency
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(AuthRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

