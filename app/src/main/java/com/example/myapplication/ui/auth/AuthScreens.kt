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
import androidx.lifecycle.viewmodel.compose.viewModel
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
        else -> null
    }
}

fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
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
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(context))
    val loginState by viewModel.loginState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRememberMeChecked by rememberSaveable { mutableStateOf(false) }

    val emailError = getEmailErrorMessage(email)
    val isFormValid = email.isNotBlank() && password.isNotBlank() && emailError == null

    // Handle login success
    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess) {
            Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
            onLoginSuccess()
        }
    }

    // Handle login error
    LaunchedEffect(loginState.error) {
        loginState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
        }
    }

    LoginScreenContent(
        email = email,
        onEmailChange = { email = it },
        emailError = emailError,
        password = password,
        onPasswordChange = { password = it },
        isRememberMeChecked = isRememberMeChecked,
        onRememberMeChange = { isRememberMeChecked = it },
        onLoginClick = {
            viewModel.loginWithFirebase(email, password, isRememberMeChecked)
        },
        isLoginEnabled = isFormValid && !loginState.isLoading,
        isLoading = loginState.isLoading,
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    isRememberMeChecked: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    isLoginEnabled: Boolean,
    isLoading: Boolean = false,
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
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    isError = emailError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRememberMeChecked,
                        onCheckedChange = onRememberMeChange,
                        enabled = !isLoading
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Đăng Nhập", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
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
            email = "",
            onEmailChange = {},
            emailError = null,
            password = "",
            onPasswordChange = {},
            isRememberMeChecked = false,
            onRememberMeChange = {},
            onLoginClick = {},
            isLoginEnabled = false,
            isLoading = false,
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
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(context))
    val registerState by viewModel.registerState.collectAsState()

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val usernameError = getUsernameErrorMessage(username)
    val emailError = getEmailErrorMessage(email)
    val passwordError = getPasswordErrorMessage(password)
    val confirmPasswordError = if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Mật khẩu không khớp"
    } else null

    val isFormValid = username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
            usernameError == null && emailError == null && passwordError == null && confirmPasswordError == null

    // Handle register success
    LaunchedEffect(registerState.isSuccess) {
        if (registerState.isSuccess) {
            Toast.makeText(context, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
            viewModel.resetRegisterState()
            onRegisterSuccess()
        }
    }

    // Handle register error
    LaunchedEffect(registerState.error) {
        registerState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetRegisterState()
        }
    }

    RegisterScreenContent(
        username = username,
        onUsernameChange = { username = it },
        usernameError = usernameError,
        email = email,
        onEmailChange = { email = it },
        emailError = emailError,
        password = password,
        onPasswordChange = { password = it },
        passwordError = passwordError,
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        confirmPasswordError = confirmPasswordError,
        onRegisterClick = {
            viewModel.registerWithFirebase(username, email, password)
        },
        isRegisterEnabled = isFormValid && !registerState.isLoading,
        isLoading = registerState.isLoading,
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
    isLoading: Boolean = false,
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Đăng Ký", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
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
            isLoading = false,
            onNavigateToLogin = {}
        )
    }
}
