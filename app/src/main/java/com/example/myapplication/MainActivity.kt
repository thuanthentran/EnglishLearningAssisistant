package com.example.myapplication.utils

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import com.example.myapplication.ui.homework.HomeworkMainScreen
import com.example.myapplication.ui.homework.HomeworkSolutionScreen
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.vocabulary.DictionaryScreen
import com.example.myapplication.ui.writing.WritingPracticeScreen
import com.example.myapplication.ui.imagelearning.ImageLearningScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val userPreferences = remember { UserPreferences(context) }
            var isDarkMode by remember { mutableStateOf(userPreferences.isDarkMode()) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainApp(
                    isDarkMode = isDarkMode,
                    onDarkModeChanged = { isDarkMode = it }
                )
            }
        }
    }
}

/* =========================
   SCREEN STATE
   ========================= */
enum class Screen {
    LOGIN,
    REGISTER,
    HOME,
    DICTIONARY,
    HOMEWORK,
    HOMEWORK_SOLUTION,
    SETTINGS,
    WRITING_PRACTICE,
    IMAGE_LEARNING
}

/* =========================
   MAIN APP
   ========================= */
@Composable
fun MainApp(
    isDarkMode: Boolean = false,
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var currentScreen by remember {
        mutableStateOf(
            if (userPreferences.isLoggedIn()) Screen.HOME else Screen.LOGIN
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            when (currentScreen) {

                Screen.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = { currentScreen = Screen.HOME },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER }
                    )
                }

                Screen.REGISTER -> {
                    RegisterScreen(
                        onRegisterSuccess = { currentScreen = Screen.LOGIN },
                        onNavigateToLogin = { currentScreen = Screen.LOGIN }
                    )
                }

                Screen.HOME -> {
                    HomeScreen(
                        onLogout = {
                            userPreferences.clearUserSession()
                            currentScreen = Screen.LOGIN
                        },
                        onVocabularyClick = {
                            currentScreen = Screen.DICTIONARY
                        },
                        onHomeworkClick = {
                            currentScreen = Screen.HOMEWORK
                        },
                        onSettingsClick = {
                            currentScreen = Screen.SETTINGS
                        },
                        onWritingPracticeClick = {
                            currentScreen = Screen.WRITING_PRACTICE
                        },
                        onImageLearningClick = {
                            currentScreen = Screen.IMAGE_LEARNING
                        }
                    )
                }

                Screen.DICTIONARY -> {
                    DictionaryScreen(
                        onBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }

                Screen.HOMEWORK -> {
                    HomeworkMainScreen(
                        onNavigateToSolution = {
                            currentScreen = Screen.HOMEWORK_SOLUTION
                        },
                        onBack = {
                            currentScreen = Screen.HOME
                        },
                        currentUsername = userPreferences.getUsername() ?: "User"
                    )
                }

                Screen.HOMEWORK_SOLUTION -> {
                    HomeworkSolutionScreen(
                        onBack = {
                            currentScreen = Screen.HOMEWORK
                        },
                        currentUsername = userPreferences.getUsername() ?: "User"
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        onBack = {
                            currentScreen = Screen.HOME
                        },
                        onDarkModeChanged = onDarkModeChanged
                    )
                }

                Screen.WRITING_PRACTICE -> {
                    WritingPracticeScreen(
                        onBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }

                Screen.IMAGE_LEARNING -> {
                    ImageLearningScreen(
                        onBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }
            }
        }
    }
}


