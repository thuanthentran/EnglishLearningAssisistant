package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.ChangePasswordResult
import com.example.myapplication.data.DatabaseHelper
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Đổi mật khẩu người dùng qua Firebase
     * @param currentPassword Mật khẩu hiện tại (để re-authenticate)
     * @param newPassword Mật khẩu mới
     * @return Result chứa Unit nếu thành công, Exception nếu thất bại
     */
    suspend fun changePasswordFirebase(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Chưa đăng nhập"))

            val email = user.email
                ?: return Result.failure(Exception("Không tìm thấy email"))

            // Re-authenticate trước khi đổi mật khẩu (bắt buộc với Firebase)
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()

            // Đổi mật khẩu trên Firebase
            user.updatePassword(newPassword).await()

            // Cập nhật luôn trong local database nếu user tồn tại (để backup)
            try {
                val username = dbHelper.getUsernameByEmail(email)
                if (username != null) {
                    // Cập nhật password trong local database
                    val db = dbHelper.writableDatabase
                    val hashedPassword = com.example.myapplication.utils.SecurityUtils.hashPassword(newPassword)
                    val values = android.content.ContentValues().apply {
                        put("password", hashedPassword)
                    }
                    db.update("users", values, "username = ?", arrayOf(username))
                }
            } catch (e: Exception) {
                // Không sao nếu local database fail, miễn Firebase thành công
                android.util.Log.w("AuthRepository", "Failed to update local database", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("wrong-password") == true ||
                e.message?.contains("invalid-credential") == true ||
                e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                    "Mật khẩu hiện tại không đúng"
                e.message?.contains("requires-recent-login") == true ->
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
                e.message?.contains("weak-password") == true ->
                    "Mật khẩu mới quá yếu (cần ít nhất 6 ký tự)"
                e.message?.contains("network") == true ->
                    "Lỗi kết nối mạng"
                else -> e.message ?: "Đã có lỗi xảy ra"
            }
            Result.failure(Exception(errorMessage))
        }
    }


    /**
     * Gửi email reset mật khẩu qua Firebase
     * Firebase sẽ tự động gửi email với link reset password
     * @param email Email của người dùng
     * @return Result chứa Unit nếu thành công
     */
    suspend fun sendPasswordResetEmailFirebase(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
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
                else -> e.message ?: "Đã có lỗi xảy ra"
            }
            Result.failure(Exception(errorMessage))
        }
    }


    /**
     * Gửi yêu cầu reset mật khẩu (kiểm tra local database)
     */
    fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            if (dbHelper.isEmailExists(email)) {
                Result.success(Unit)
            } else {
                Result.failure(EmailNotFoundException("Email chưa được đăng ký trong hệ thống"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset mật khẩu theo email (local database)
     */
    fun resetPassword(email: String, newPassword: String): Result<Unit> {
        return try {
            if (dbHelper.resetPasswordByEmail(email, newPassword)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Không thể reset mật khẩu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đăng ký tài khoản Firebase
     */
    suspend fun registerFirebase(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("email-already-in-use") == true ->
                    "Email đã được sử dụng"
                e.message?.contains("weak-password") == true ->
                    "Mật khẩu quá yếu"
                e.message?.contains("invalid-email") == true ->
                    "Email không hợp lệ"
                else -> e.message ?: "Đã có lỗi xảy ra"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Đăng nhập Firebase
     */
    suspend fun loginFirebase(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("user-not-found") == true ->
                    "Email chưa được đăng ký"
                e.message?.contains("wrong-password") == true ||
                e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                    "Sai mật khẩu"
                e.message?.contains("invalid-email") == true ->
                    "Email không hợp lệ"
                else -> e.message ?: "Đã có lỗi xảy ra"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Đăng xuất Firebase
     */
    fun logoutFirebase() {
        firebaseAuth.signOut()
    }

    /**
     * Kiểm tra user đã đăng nhập Firebase chưa
     */
    fun isLoggedInFirebase(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Lấy email của user Firebase hiện tại
     */
    fun getCurrentFirebaseEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    /**
     * Kiểm tra email có tồn tại không (local)
     */
    fun isEmailExists(email: String): Boolean {
        return dbHelper.isEmailExists(email)
    }

    /**
     * Lấy username theo email (local)
     */
    fun getUsernameByEmail(email: String): String? {
        return dbHelper.getUsernameByEmail(email)
    }
}

// Custom Exceptions
class WrongPasswordException(message: String) : Exception(message)
class EmailNotFoundException(message: String) : Exception(message)

