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
import com.example.myapplication.data.repository.LearnedWordsRepository
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.RegisterScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.homework.HomeworkMainScreen
import com.example.myapplication.ui.homework.HomeworkSolutionScreen
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.vocabulary.DictionaryScreen
import com.example.myapplication.ui.game.GameScreen
import com.example.myapplication.ui.learnwords.LearnWordsScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
    GAME,
    LEARN_WORDS
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
    val learnedWordsRepository = remember { LearnedWordsRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf(
            if (userPreferences.isLoggedIn()) Screen.HOME else Screen.LOGIN
        )
    }

    // Sync learned words khi user đăng nhập hoặc app khởi động với user đã đăng nhập
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.HOME) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                // User đã đăng nhập, sync learned words từ cloud
                android.util.Log.d("MainActivity", "Syncing learned words for user: ${firebaseUser.uid}")
                learnedWordsRepository.syncFromCloud()
            }
        }
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
                        onGameClick = {
                            currentScreen = Screen.GAME
                        },
                        onLearnWordsClick = {
                            currentScreen = Screen.LEARN_WORDS
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

                Screen.LEARN_WORDS -> {
                    LearnWordsScreen(
                        onBack = {
                            currentScreen = Screen.HOME
                        }
                    )
                }

                Screen.GAME -> {
                    GameScreen(
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
                        onDarkModeChanged = onDarkModeChanged,
                        onLogout = {
                            userPreferences.clearUserSession()
                            currentScreen = Screen.LOGIN
                        }
                    )
                }
            }
        }
    }
}


