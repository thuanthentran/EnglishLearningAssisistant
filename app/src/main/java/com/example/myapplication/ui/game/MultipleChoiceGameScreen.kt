package com.example.myapplication.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.game.components.AnswerOption
import com.example.myapplication.ui.game.components.ScoreBar
import com.example.myapplication.ui.game.components.WordCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleChoiceGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.multipleChoiceState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initMultipleChoiceGame()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chọn nghĩa đúng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.errorMessage != null -> {
                    ErrorContent(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.onMultipleChoiceEvent(GameEvent.RestartGame) }
                    )
                }
                uiState.isGameFinished -> {
                    GameFinishedContent(
                        score = uiState.score,
                        totalQuestions = uiState.totalQuestions,
                        onRestart = { viewModel.onMultipleChoiceEvent(GameEvent.RestartGame) },
                        onBack = onBack
                    )
                }
                else -> {
                    MultipleChoiceContent(
                        uiState = uiState,
                        onSelectAnswer = { viewModel.onMultipleChoiceEvent(GameEvent.SelectAnswer(it)) },
                        onNext = { viewModel.onMultipleChoiceEvent(GameEvent.NextQuestion) }
                    )
                }
            }
        }
    }
}

@Composable
fun MultipleChoiceContent(
    uiState: MultipleChoiceUiState,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score bar
        ScoreBar(
            currentQuestion = uiState.currentQuestion,
            totalQuestions = uiState.totalQuestions,
            score = uiState.score
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Word card
        uiState.currentWord?.let { word ->
            WordCard(
                word = word.word,
                phonetic = word.phonetic,
                type = word.type
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question prompt
        Text(
            text = "Chọn nghĩa đúng của từ:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Answer options
        uiState.options.forEachIndexed { index, option ->
            AnswerOption(
                text = option,
                index = index,
                isSelected = uiState.selectedAnswerIndex == index,
                isCorrect = uiState.correctAnswerIndex == index,
                isAnswered = uiState.isAnswered,
                onClick = { onSelectAnswer(index) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button (only show when answered)
        if (uiState.isAnswered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text(
                    text = if (uiState.currentQuestion < uiState.totalQuestions - 1)
                        "Câu tiếp theo" else "Xem kết quả",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF667eea)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đang tải...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
fun GameFinishedContent(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val percentage = (score.toFloat() / totalQuestions.toFloat() * 100).toInt()
    val resultMessage = when {
        percentage >= 90 -> "Xuất sắc! 🎉"
        percentage >= 70 -> "Tốt lắm! 👏"
        percentage >= 50 -> "Khá tốt! 👍"
        else -> "Cố gắng hơn nhé! 💪"
    }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Trophy icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hoàn thành!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = resultMessage,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF667eea)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$score / $totalQuestions",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF667eea)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đúng $percentage%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chơi lại",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Quay lại",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF667eea)
                )
            }
        }
    }
}

