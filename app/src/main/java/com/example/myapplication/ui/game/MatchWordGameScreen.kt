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
import com.example.myapplication.ui.game.components.MatchWordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchWordGameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.matchWordState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initMatchWordGame()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ghép từ",
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
                    MatchWordLoadingContent()
                }
                uiState.errorMessage != null -> {
                    MatchWordErrorContent(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.onMatchWordEvent(GameEvent.RestartMatchGame) }
                    )
                }
                uiState.isGameFinished -> {
                    MatchGameFinishedContent(
                        score = uiState.score,
                        totalPairs = uiState.leftItems.size * uiState.totalRounds,
                        onRestart = { viewModel.onMatchWordEvent(GameEvent.RestartMatchGame) },
                        onBack = onBack
                    )
                }
                uiState.isRoundComplete -> {
                    RoundCompleteContent(
                        currentRound = uiState.currentRound,
                        totalRounds = uiState.totalRounds,
                        score = uiState.score,
                        onNextRound = { viewModel.onMatchWordEvent(GameEvent.NextRound) }
                    )
                }
                else -> {
                    MatchWordContent(
                        uiState = uiState,
                        onSelectLeft = { viewModel.onMatchWordEvent(GameEvent.SelectLeftItem(it)) },
                        onSelectRight = { viewModel.onMatchWordEvent(GameEvent.SelectRightItem(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchWordContent(
    uiState: MatchWordUiState,
    onSelectLeft: (Int) -> Unit,
    onSelectRight: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with round info and score
        MatchScoreHeader(
            currentRound = uiState.currentRound,
            totalRounds = uiState.totalRounds,
            score = uiState.score,
            matchedCount = uiState.matchedPairs.size,
            totalPairs = uiState.leftItems.size
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Instruction
        Text(
            text = "Ghép từ tiếng Anh với nghĩa tiếng Việt",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Two columns: English words and Vietnamese meanings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column - English words
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tiếng Anh",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF667eea),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                uiState.leftItems.forEachIndexed { index, item ->
                    val isMatched = uiState.matchedPairs.contains(index)
                    val isSelected = uiState.selectedLeftIndex == index
                    val isWrong = uiState.wrongPairIndices?.first == index

                    MatchWordItem(
                        text = item.text,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isWrong = isWrong,
                        onClick = { onSelectLeft(index) }
                    )
                }
            }

            // Right column - Vietnamese meanings
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tiếng Việt",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF11998e),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                uiState.rightItems.forEachIndexed { index, item ->
                    // Check if this right item is matched
                    val isMatched = uiState.matchedPairs.any { leftIdx ->
                        val leftItem = uiState.leftItems.getOrNull(leftIdx)
                        leftItem?.originalWordId == item.originalWordId
                    }
                    val isSelected = uiState.selectedRightIndex == index
                    val isWrong = uiState.wrongPairIndices?.second == index

                    MatchWordItem(
                        text = item.text,
                        isSelected = isSelected,
                        isMatched = isMatched,
                        isWrong = isWrong,
                        onClick = { onSelectRight(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchScoreHeader(
    currentRound: Int,
    totalRounds: Int,
    score: Int,
    matchedCount: Int,
    totalPairs: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round info
                Text(
                    text = "Vòng $currentRound/$totalRounds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Score
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$score",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667eea)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            val progress = if (totalPairs > 0) matchedCount.toFloat() / totalPairs.toFloat() else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã ghép: $matchedCount/$totalPairs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF11998e))
                )
            }
        }
    }
}

@Composable
fun RoundCompleteContent(
    currentRound: Int,
    totalRounds: Int,
    score: Int,
    onNextRound: () -> Unit
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Hoàn thành vòng $currentRound!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Điểm hiện tại: $score",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF667eea)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNextRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF11998e)
                )
            ) {
                Text(
                    text = "Vòng tiếp theo",
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
fun MatchGameFinishedContent(
    score: Int,
    totalPairs: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val percentage = if (totalPairs > 0) (score.toFloat() / totalPairs.toFloat() * 100).toInt() else 0
    val resultMessage = when {
        percentage >= 90 -> "Xuất sắc! 🎉"
        percentage >= 70 -> "Tốt lắm! 👏"
        percentage >= 50 -> "Khá tốt! 👍"
        else -> "Cố gắng hơn nhé! 💪"
    }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
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
                color = Color(0xFF11998e)
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
                            text = "$score cặp đúng",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF11998e)
                        )
                    }
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
                    containerColor = Color(0xFF11998e)
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
                    color = Color(0xFF11998e)
                )
            }
        }
    }
}

@Composable
private fun MatchWordLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF11998e)
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
private fun MatchWordErrorContent(
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
                    containerColor = Color(0xFF11998e)
                )
            ) {
                Text("Thử lại")
            }
        }
    }
}

