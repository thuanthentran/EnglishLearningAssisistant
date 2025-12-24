package com.example.myapplication.ui.learnwords

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nội dung màn hình học từ mới
 */
@Composable
fun LearnNewWordsContent(
    viewModel: LearnWordsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load từ mới khi mở
    LaunchedEffect(Unit) {
        viewModel.loadNewWords()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header với nút quay lại
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.resetNewWordsSession()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Học từ mới",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            // Progress indicator
            if (uiState.newWordsToLearn.isNotEmpty()) {
                Text(
                    "${uiState.currentNewWordIndex + 1}/${uiState.newWordsToLearn.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            uiState.isLoadingNewWords -> {
                LoadingContent()
            }
            uiState.errorMessage != null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadNewWords() },
                    onBack = onBack
                )
            }
            uiState.isNewWordsSessionComplete -> {
                SessionCompleteContent(
                    title = "Hoàn thành học từ mới!",
                    score = uiState.newWordsScore,
                    total = uiState.newWordsToLearn.size,
                    onRestart = { viewModel.loadNewWords() },
                    onBack = {
                        viewModel.resetNewWordsSession()
                        viewModel.loadDailyStatus()
                        onBack()
                    }
                )
            }
            uiState.newWordsToLearn.isNotEmpty() -> {
                val currentWord = uiState.newWordsToLearn[uiState.currentNewWordIndex]
                val isAlreadyLearned = uiState.todayLearnedWordIds.contains(currentWord.id)

                // Hiển thị thông tin từ thay vì quiz
                LearnWordInfoContent(
                    word = currentWord,
                    onMarkLearned = { viewModel.markCurrentWordAsLearned() },
                    onNext = { viewModel.nextNewWord() },
                    isLastWord = uiState.currentNewWordIndex >= uiState.newWordsToLearn.size - 1,
                    accentColor = Color(0xFF11998e),
                    isAlreadyLearned = isAlreadyLearned
                )
            }
        }
    }
}

/**
 * Nội dung màn hình ôn từ đã học
 */
@Composable
fun ReviewWordsContent(
    viewModel: LearnWordsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load từ ôn tập khi mở
    LaunchedEffect(Unit) {
        viewModel.loadReviewWords()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header với nút quay lại
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.resetReviewSession()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Ôn từ đã học",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            // Progress indicator
            if (uiState.reviewWords.isNotEmpty()) {
                Text(
                    "${uiState.currentReviewIndex + 1}/${uiState.reviewWords.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            uiState.isLoadingReview -> {
                LoadingContent()
            }
            uiState.errorMessage != null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadReviewWords() },
                    onBack = onBack
                )
            }
            uiState.isReviewSessionComplete -> {
                SessionCompleteContent(
                    title = "Hoàn thành ôn tập!",
                    score = uiState.reviewScore,
                    total = uiState.reviewWords.size,
                    onRestart = { viewModel.loadReviewWords() },
                    onBack = {
                        viewModel.resetReviewSession()
                        onBack()
                    }
                )
            }
            uiState.reviewWords.isNotEmpty() -> {
                QuizContent(
                    word = uiState.reviewWords[uiState.currentReviewIndex],
                    answerOptions = uiState.reviewAnswerOptions,
                    selectedAnswer = uiState.selectedReviewAnswer,
                    isCorrect = uiState.isReviewAnswerCorrect,
                    onSelectAnswer = { viewModel.selectReviewAnswer(it) },
                    onNext = { viewModel.nextReviewWord() },
                    isLastWord = uiState.currentReviewIndex >= uiState.reviewWords.size - 1,
                    accentColor = Color(0xFF667eea)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF11998e))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Đang tải từ vựng...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
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
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Quay lại")
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11998e))
                ) {
                    Text("Thử lại")
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteContent(
    title: String,
    score: Int,
    total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Score
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Kết quả",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$score / $total",
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = Color(0xFF11998e)
                    )
                    Text(
                        "${if (total > 0) (score * 100 / total) else 0}% chính xác",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Quay lại")
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11998e))
                ) {
                    Text("Học tiếp")
                }
            }
        }
    }
}

/**
 * Hiển thị thông tin từ vựng để học với chức năng lật card
 */
@Composable
private fun LearnWordInfoContent(
    word: WordToLearn,
    onMarkLearned: () -> Unit,
    onNext: () -> Unit,
    isLastWord: Boolean,
    accentColor: Color,
    isAlreadyLearned: Boolean = false
) {
    var isMarkedLearned by remember(word.id) { mutableStateOf(isAlreadyLearned) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Flippable Word Card
        FlippableWordCard(
            word = word,
            accentColor = accentColor,
            isAlreadyLearned = isAlreadyLearned
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nút đánh dấu đã học hoặc nút tiếp theo
        if (!isMarkedLearned) {
            Button(
                onClick = {
                    onMarkLearned()
                    isMarkedLearned = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đã học xong",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            // Nút tiếp theo (cho cả trường hợp vừa đánh dấu hoặc đang xem lại)
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    if (isLastWord) "Hoàn thành" else "Từ tiếp theo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Card có thể lật: mặt trước hiển thị từ tiếng Anh, mặt sau hiển thị nghĩa
 */
@Composable
private fun FlippableWordCard(
    word: WordToLearn,
    accentColor: Color,
    isAlreadyLearned: Boolean = false
) {
    var isFlipped by remember(word.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "card_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clickable { isFlipped = !isFlipped }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Lật nội dung khi card xoay quá 90 độ
                    rotationY = if (rotation > 90f) 180f else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Mặt trước: hiển thị từ tiếng Anh
                CardFrontContent(
                    word = word,
                    accentColor = accentColor,
                    isAlreadyLearned = isAlreadyLearned
                )
            } else {
                // Mặt sau: hiển thị nghĩa
                CardBackContent(
                    word = word,
                    accentColor = accentColor
                )
            }
        }
    }

    // Hướng dẫn
    Text(
        "Bấm vào card để xem nghĩa",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/**
 * Mặt trước của card: hiển thị từ tiếng Anh
 */
@Composable
private fun CardFrontContent(
    word: WordToLearn,
    accentColor: Color,
    isAlreadyLearned: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Badge đã học nếu đang xem lại
        if (isAlreadyLearned) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Đã học",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Từ vựng
        Text(
            word.word,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = accentColor,
            textAlign = TextAlign.Center
        )

        // Phiên âm
        if (word.phonetic.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                word.phonetic,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Loại từ
        if (word.type.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    word.type,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Icon gợi ý lật card
        Icon(
            Icons.Default.TouchApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Mặt sau của card: hiển thị nghĩa
 */
@Composable
private fun CardBackContent(
    word: WordToLearn,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon check
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Từ vựng (nhỏ hơn)
        Text(
            word.word,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(modifier = Modifier.height(24.dp))

        // Nghĩa
        Text(
            "Nghĩa:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            word.definition,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun QuizContent(
    word: WordToLearn,
    answerOptions: List<String>,
    selectedAnswer: Int?,
    isCorrect: Boolean?,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    isLastWord: Boolean,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Từ vựng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    word.word,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = accentColor
                )

                if (word.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        word.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (word.type.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "(${word.type})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Chọn nghĩa đúng:",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Answer options
        answerOptions.forEachIndexed { index, answer ->
            AnswerOptionCard(
                answer = answer,
                index = index,
                isSelected = selectedAnswer == index,
                isCorrect = if (selectedAnswer != null) answer == word.shortDefinition else null,
                showResult = selectedAnswer != null,
                accentColor = accentColor,
                onClick = { onSelectAnswer(index) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Show correct answer if wrong
        if (selectedAnswer != null && isCorrect == false) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Đáp án đúng:",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        word.shortDefinition,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Định nghĩa đầy đủ:",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        word.definition,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next button
        if (selectedAnswer != null) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    if (isLastWord) "Hoàn thành" else "Tiếp theo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AnswerOptionCard(
    answer: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean?,
    showResult: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        showResult && isCorrect == true -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        showResult && isSelected && isCorrect == false -> Color(0xFFE53935).copy(alpha = 0.15f)
        isSelected -> accentColor.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect == true -> Color(0xFF4CAF50)
        showResult && isSelected && isCorrect == false -> Color(0xFFE53935)
        isSelected -> accentColor
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val textColor = when {
        showResult && isCorrect == true -> Color(0xFF4CAF50)
        showResult && isSelected && isCorrect == false -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !showResult) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(borderColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${('A'.code + index).toChar()}",
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                answer,
                modifier = Modifier.weight(1f),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )

            // Result icon
            if (showResult) {
                when {
                    isCorrect == true -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    isSelected && isCorrect == false -> Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}


/**
 * Nội dung màn hình ôn từ đã học trong tuần
 */
@Composable
fun WeekReviewWordsContent(
    viewModel: LearnWordsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load từ ôn tập trong tuần khi mở
    LaunchedEffect(Unit) {
        viewModel.loadWeekReviewWords()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header với nút quay lại
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.resetWeekReviewSession()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Ôn từ trong tuần",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            // Progress indicator
            if (uiState.weekReviewWords.isNotEmpty()) {
                Text(
                    "${uiState.currentWeekReviewIndex + 1}/${uiState.weekReviewWords.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            uiState.isLoadingWeekReview -> {
                LoadingContent()
            }
            uiState.errorMessage != null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadWeekReviewWords() },
                    onBack = onBack
                )
            }
            uiState.isWeekReviewSessionComplete -> {
                SessionCompleteContent(
                    title = "Hoàn thành ôn tập tuần!",
                    score = uiState.weekReviewScore,
                    total = uiState.weekReviewWords.size,
                    onRestart = { viewModel.loadWeekReviewWords() },
                    onBack = {
                        viewModel.resetWeekReviewSession()
                        onBack()
                    }
                )
            }
            uiState.weekReviewWords.isNotEmpty() -> {
                QuizContent(
                    word = uiState.weekReviewWords[uiState.currentWeekReviewIndex],
                    answerOptions = uiState.weekReviewAnswerOptions,
                    selectedAnswer = uiState.selectedWeekReviewAnswer,
                    isCorrect = uiState.isWeekReviewAnswerCorrect,
                    onSelectAnswer = { viewModel.selectWeekReviewAnswer(it) },
                    onNext = { viewModel.nextWeekReviewWord() },
                    isLastWord = uiState.currentWeekReviewIndex >= uiState.weekReviewWords.size - 1,
                    accentColor = Color(0xFFFF6B6B)
                )
            }
        }
    }
}

