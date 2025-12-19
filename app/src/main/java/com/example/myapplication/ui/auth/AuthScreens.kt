package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.myapplication.data.DatabaseHelper
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.ui.theme.MyApplicationTheme

// Validation helper functions

fun getUsernameErrorMessage(username: String): String? {
    return when {
        username.isEmpty() -> null
        username.length < 3 -> "Tên người dùng phải có ít nhất 3 ký tự"
        username.length > 20 -> "Tên người dùng không được vượt quá 20 ký tự"
        !username.all { it.isLetterOrDigit() || it == '_' } -> "Tên người dùng chỉ chứa chữ cái, số và dấu gạch dưới"
        else -> null
    }
}

fun getPasswordErrorMessage(password: String): String? {
    return when {
        password.isEmpty() -> null
        password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
        !password.any { it.isUpperCase() } -> "Mật khẩu phải chứa ít nhất một chữ cái in hoa"
        !password.any { it.isDigit() } -> "Mật khẩu phải chứa ít nhất một chữ số"
        else -> null
    }
}

fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() && email.contains("@") && email.contains(".")
}

fun getEmailErrorMessage(email: String): String? {
    return when {
        email.isEmpty() -> null
        !isValidEmail(email) -> "Email không hợp lệ"
        else -> null
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userPreferences = remember { UserPreferences(context) }

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRememberMeChecked by rememberSaveable { mutableStateOf(false) }

    val isFormValid = username.isNotBlank() && password.isNotBlank()

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_WARNING")
    LoginScreenContent(
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        isRememberMeChecked = isRememberMeChecked,
        onRememberMeChange = { isRememberMeChecked = it },
        onLoginClick = {
            if (dbHelper.checkUser(username, password)) {
                val email = dbHelper.getUserEmail(username) ?: ""
                userPreferences.saveUserSession(username, email, isRememberMeChecked)
                Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } else {
                Toast.makeText(context, "Sai tên người dùng hoặc mật khẩu", Toast.LENGTH_SHORT).show()
            }
        },
        isLoginEnabled = isFormValid,
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
fun LoginScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isRememberMeChecked: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    isLoginEnabled: Boolean,
    onNavigateToRegister: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đăng Nhập",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Tên người dùng") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Tên người dùng") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Mật khẩu") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRememberMeChecked,
                        onCheckedChange = onRememberMeChange
                    )
                    Text(text = "Ghi nhớ đăng nhập", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isLoginEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Đăng Nhập", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Chưa có tài khoản? Đăng ký ngay",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyApplicationTheme {
        LoginScreenContent(
            username = "",
            onUsernameChange = {},
            password = "",
            onPasswordChange = {},
            isRememberMeChecked = false,
            onRememberMeChange = {},
            onLoginClick = {},
            isLoginEnabled = false,
            onNavigateToRegister = {}
        )
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val usernameError = getUsernameErrorMessage(username)
    val emailError = getEmailErrorMessage(email)
    val emailExists = if (email.isNotEmpty() && isValidEmail(email)) dbHelper.isEmailExists(email) else false
    val emailExistsError = if (emailExists) "Email này đã được sử dụng" else null
    val passwordError = getPasswordErrorMessage(password)
    val confirmPasswordError = if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Mật khẩu không khớp"
    } else null

    val isFormValid = username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
            usernameError == null && emailError == null && emailExistsError == null && passwordError == null && confirmPasswordError == null

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_WARNING")
    RegisterScreenContent(
        username = username,
        onUsernameChange = { username = it },
        usernameError = usernameError,
        email = email,
        onEmailChange = { email = it },
        emailError = emailError ?: emailExistsError,
        password = password,
        onPasswordChange = { password = it },
        passwordError = passwordError,
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        confirmPasswordError = confirmPasswordError,
        onRegisterClick = {
            if (!isFormValid) {
                Toast.makeText(context, "Vui lòng kiểm tra lại thông tin đăng ký", Toast.LENGTH_SHORT).show()
            } else {
                if (dbHelper.registerUser(username, email, password)) {
                    Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess()
                } else {
                    Toast.makeText(context, "Tên người dùng đã tồn tại", Toast.LENGTH_SHORT).show()
                }
            }
        },
        isRegisterEnabled = isFormValid,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
fun RegisterScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordError: String?,
    onRegisterClick: () -> Unit,
    isRegisterEnabled: Boolean,
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đăng Ký",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Tên người dùng") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Tên người dùng") },
                    isError = usernameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (usernameError != null) {
                    Text(
                        text = usernameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Mật khẩu") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    isError = passwordError != null,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (passwordError != null) {
                    Text(
                        text = passwordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Xác nhận mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Xác nhận mật khẩu") },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    isError = confirmPasswordError != null,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (confirmPasswordError != null) {
                    Text(
                        text = confirmPasswordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isRegisterEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Đăng Ký", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Đã có tài khoản? Đăng nhập",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MyApplicationTheme {
        RegisterScreenContent(
            username = "",
            onUsernameChange = {},
            usernameError = null,
            email = "",
            onEmailChange = {},
            emailError = null,
            password = "",
            onPasswordChange = {},
            passwordError = null,
            confirmPassword = "",
            onConfirmPasswordChange = {},
            confirmPasswordError = null,
            onRegisterClick = {},
            isRegisterEnabled = false,
            onNavigateToLogin = {}
        )
    }
}
