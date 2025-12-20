package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }

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

    // Handle forgot password success
    LaunchedEffect(forgotPasswordState.isSuccess) {
        if (forgotPasswordState.isSuccess) {
            // Dialog will show success state
        }
    }

    LoginScreenContent(
        email = email,
        onEmailChange = { email = it },
        emailError = emailError,
        password = password,
        onPasswordChange = { password = it },
        onLoginClick = {
            viewModel.loginWithFirebase(email, password, false)
        },
        isLoginEnabled = isFormValid && !loginState.isLoading,
        isLoading = loginState.isLoading,
        onNavigateToRegister = onNavigateToRegister,
        onForgotPasswordClick = { showForgotPasswordDialog = true }
    )

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = {
                showForgotPasswordDialog = false
                viewModel.resetForgotPasswordState()
            },
            onSendReset = { resetEmail ->
                viewModel.sendPasswordResetEmail(resetEmail)
            },
            isLoading = forgotPasswordState.isLoading,
            isSuccess = forgotPasswordState.isSuccess,
            externalError = forgotPasswordState.error,
            defaultEmail = email
        )
    }
}

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    isLoginEnabled: Boolean,
    isLoading: Boolean = false,
    onNavigateToRegister: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),  // Very light blue
                        Color(0xFFFFFFFF)   // Pure white
                    )
                )
            )
    ) {
        // Top curved gradient background - soft blue colors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF90CAF9),  // Light blue
                            Color(0xFF64B5F6),  // Medium light blue
                            Color(0xFF42A5F5)   // Bright light blue
                        )
                    )
                )
        )

        // Decorative shapes with softer colors
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 280.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 320.dp, y = 180.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = 40.dp, y = 140.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.2f))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            AppLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "English Learning",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Học tiếng Anh mỗi ngày",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đăng Nhập",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A5F)
                    )

                    Text(
                        text = "Chào mừng bạn trở lại!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color(0xFF2196F3)
                            )
                        },
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3),
                            cursorColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (emailError != null) {
                        Text(
                            text = emailError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Mật khẩu",
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3),
                            cursorColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Forgot Password Link
                    TextButton(
                        onClick = onForgotPasswordClick,
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Quên mật khẩu?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Login Button with blue color
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = isLoginEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            disabledContainerColor = Color(0xFF2196F3).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Đăng Nhập",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Chưa có tài khoản?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                        TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
                            Text(
                                "Đăng ký ngay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Forgot Password Dialog
@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendReset: (email: String) -> Unit,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    externalError: String? = null,
    defaultEmail: String
) {
    var email by remember { mutableStateOf(defaultEmail) }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = externalError ?: localError

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSuccess) Icons.Default.MarkEmailRead else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFF64B5F6),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isSuccess) "Email đã được gửi!" else "Quên mật khẩu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF1a1a2e)
                )
            }
        },
        text = {
            if (isSuccess) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Chúng tôi đã gửi link đặt lại mật khẩu đến email:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        email,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF667eea),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Vui lòng kiểm tra hộp thư đến (và cả thư rác) để tiếp tục.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    Text(
                        "Nhập email đã đăng ký để nhận link đặt lại mật khẩu",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null },
                        label = { Text("Email") },
                        placeholder = { Text("example@email.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667eea),
                            focusedLabelColor = Color(0xFF667eea),
                            cursorColor = Color(0xFF667eea)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = Color(0xFFE57373), fontSize = 13.sp)
                    }
                    if (isLoading) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF667eea),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Đang gửi...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đã hiểu", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        when {
                            email.isEmpty() -> localError = "Vui lòng nhập email"
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                localError = "Email không hợp lệ"
                            else -> onSendReset(email)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea),
                        disabledContainerColor = Color(0xFF667eea).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gửi email", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("Hủy", color = if (isLoading) Color.Gray.copy(alpha = 0.5f) else Color.Gray)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
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
            onLoginClick = {},
            isLoginEnabled = false,
            isLoading = false,
            onNavigateToRegister = {},
            onForgotPasswordClick = {}
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFC8E6C9),  // Very light green
                        Color(0xFFFFFFFF)   // Pure white
                    )
                )
            )
    ) {
        // Top curved gradient background - soft green colors
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF81C784),  // Light green
                            Color(0xFF66BB6A),  // Medium light green
                            Color(0xFF4CAF50)   // Bright light green
                        )
                    )
                )
        )

        // Decorative circles with softer colors
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 300.dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 280.dp, y = 650.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF).copy(alpha = 0.18f))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            AppLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tạo tài khoản mới",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Bắt đầu hành trình học tiếng Anh",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Register Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đăng Ký",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A5F)
                    )

                    Text(
                        text = "Tạo tài khoản mới để bắt đầu học",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF4CAF50))
                        },
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (emailError != null) {
                        Text(
                            text = emailError,
                            color = Color(0xFFEF5350),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = { Text("Tên người dùng") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Tên người dùng", tint = Color(0xFF4CAF50))
                        },
                        isError = usernameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (usernameError != null) {
                        Text(
                            text = usernameError,
                            color = Color(0xFFEF5350),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Mật khẩu", tint = Color(0xFF4CAF50))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError,
                            color = Color(0xFFEF5350),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Xác nhận mật khẩu", tint = Color(0xFF4CAF50))
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        isError = confirmPasswordError != null,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedLabelColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    if (confirmPasswordError != null) {
                        Text(
                            text = confirmPasswordError,
                            color = Color(0xFFEF5350),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Register Button
                    Button(
                        onClick = onRegisterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = isRegisterEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Đăng Ký",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Đã có tài khoản?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                        TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
                            Text(
                                "Đăng nhập",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * App Logo Component - Using image from drawable
 * Current logo file: logo.png in drawable folder
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(size / 4),
                ambientColor = Color(0xFF2196F3).copy(alpha = 0.3f),
                spotColor = Color(0xFF2196F3).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(size / 4))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Load logo.png from drawable using R.drawable
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(com.example.myapplication.R.drawable.logo)
                .crossfade(true)
                .build(),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(size * 0.85f)
                .padding(size * 0.05f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppLogoPreview() {
    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AppLogo(size = 120.dp)
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
