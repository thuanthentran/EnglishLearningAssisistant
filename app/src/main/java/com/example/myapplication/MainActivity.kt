package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.RegisterScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

enum class Screen {
    LOGIN, REGISTER, HOME
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    
    // Determine initial state based on login status
    var currentScreen by remember { 
        mutableStateOf(if (userPreferences.isLoggedIn()) Screen.HOME else Screen.LOGIN) 
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Apply padding to content to respect edge-to-edge
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = { currentScreen = Screen.HOME },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER }
                    )
                }
                Screen.REGISTER -> {
                    RegisterScreen(
                        onRegisterSuccess = { currentScreen = Screen.LOGIN }, // Or go directly to HOME if desired
                        onNavigateToLogin = { currentScreen = Screen.LOGIN }
                    )
                }
                Screen.HOME -> {
                    HomeScreen(
                        onLogout = { currentScreen = Screen.LOGIN }
                    )
                }
            }
        }
    }
}