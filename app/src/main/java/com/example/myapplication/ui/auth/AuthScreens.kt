package com.example.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.DatabaseHelper
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userPreferences = remember { UserPreferences(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRememberMeChecked by remember { mutableStateOf(false) }

    LoginScreenContent(
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        isRememberMeChecked = isRememberMeChecked,
        onRememberMeChange = { isRememberMeChecked = it },
        onLoginClick = {
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            } else {
                if (dbHelper.checkUser(username, password)) {
                    val email = dbHelper.getUserEmail(username) ?: ""
                    userPreferences.saveUserSession(username, email, isRememberMeChecked)
                    Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Sai tên người dùng hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }
        },
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
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đăng Nhập", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Tên người dùng") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isRememberMeChecked,
                onCheckedChange = onRememberMeChange
            )
            Text(text = "Ghi nhớ đăng nhập")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng Nhập")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Chưa có tài khoản? Đăng ký ngay")
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

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    RegisterScreenContent(
        username = username,
        onUsernameChange = { username = it },
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        onRegisterClick = {
            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            } else {
                if (dbHelper.registerUser(username, email, password)) {
                    Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess()
                } else {
                    Toast.makeText(context, "Đăng ký thất bại (Có thể tên người dùng đã tồn tại)", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
fun RegisterScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Đăng Ký", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Tên người dùng") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Xác nhận mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng Ký")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập")
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
            email = "",
            onEmailChange = {},
            password = "",
            onPasswordChange = {},
            confirmPassword = "",
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onNavigateToLogin = {}
        )
    }
}