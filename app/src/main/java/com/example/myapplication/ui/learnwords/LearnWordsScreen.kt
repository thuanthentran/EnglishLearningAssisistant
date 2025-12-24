package com.example.myapplication.ui.learnwords

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Màn hình chính của tính năng Học từ
 * Có 2 card: Học từ mới và Ôn từ đã học
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnWordsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LearnWordsViewModel = viewModel(
        factory = LearnWordsViewModel.Factory(context)
    )

    val uiState by viewModel.uiState.collectAsState()

    var selectedMode by remember { mutableStateOf<LearnMode?>(null) }

    // Khi mở màn hình, load thông tin
    LaunchedEffect(Unit) {
        viewModel.loadDailyStatus()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            // Chỉ hiển thị TopAppBar khi ở màn hình chọn chế độ
            if (selectedMode == null) {
                TopAppBar(
                    title = {
                        Text(
                            "Học từ vựng",
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
        }
    ) { padding ->
        // Sử dụng padding phù hợp tùy thuộc vào selectedMode
        val contentModifier = if (selectedMode == null) {
            Modifier.padding(padding)
        } else {
            Modifier.padding(top = padding.calculateTopPadding())
        }

        when (selectedMode) {
            LearnMode.NEW_WORDS -> {
                LearnNewWordsContent(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.resetNewWordsSession()
                        selectedMode = null
                    },
                    modifier = contentModifier
                )
            }
            LearnMode.REVIEW -> {
                ReviewWordsContent(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.resetReviewSession()
                        selectedMode = null
                    },
                    modifier = contentModifier
                )
            }
            LearnMode.WEEK_REVIEW -> {
                WeekReviewWordsContent(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.resetWeekReviewSession()
                        selectedMode = null
                    },
                    modifier = contentModifier
                )
            }
            null -> {
                // Màn hình chọn chế độ
                LearnModeSelectionContent(
                    uiState = uiState,
                    onSelectNewWords = { selectedMode = LearnMode.NEW_WORDS },
                    onSelectReview = { selectedMode = LearnMode.REVIEW },
                    onSelectWeekReview = { selectedMode = LearnMode.WEEK_REVIEW },
                    modifier = contentModifier
                )
            }
        }
    }
}

enum class LearnMode {
    NEW_WORDS,
    REVIEW,
    WEEK_REVIEW
}

@Composable
private fun LearnModeSelectionContent(
    uiState: LearnWordsUiState,
    onSelectNewWords: () -> Unit,
    onSelectReview: () -> Unit,
    onSelectWeekReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thống kê hôm nay
        DailyProgressCard(
            wordsLearnedToday = uiState.wordsLearnedToday,
            dailyGoal = uiState.dailyGoal,
            totalLearnedWords = uiState.totalLearnedWords
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card Học từ mới
        LearnModeCard(
            icon = Icons.Default.Add,
            title = "Học từ mới",
            description = if (uiState.wordsLearnedToday >= uiState.dailyGoal)
                "Xem lại ${uiState.wordsLearnedToday} từ đã học hôm nay"
            else
                "Học ${uiState.dailyGoal - uiState.wordsLearnedToday} từ mới hôm nay",
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
            ),
            iconBgColor = Color(0xFF11998e),
            enabled = true, // Luôn cho phép vào xem lại
            onClick = onSelectNewWords
        )

        // Card Ôn từ đã học hôm nay
        LearnModeCard(
            icon = Icons.Default.Refresh,
            title = "Ôn từ đã học",
            description = if (uiState.wordsLearnedToday > 0)
                "Ôn lại ${uiState.wordsLearnedToday} từ đã học hôm nay"
            else
                "Ôn lại các từ đã học hôm nay",
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
            ),
            iconBgColor = Color(0xFF667eea),
            enabled = true, // Luôn enable
            onClick = onSelectReview
        )

        // Card Ôn từ đã học trong tuần
        LearnModeCard(
            icon = Icons.Default.CalendarMonth,
            title = "Ôn từ trong tuần",
            description = if (uiState.weekLearnedWordIds.isNotEmpty())
                "Ôn lại ${uiState.weekLearnedWordIds.size} từ đã học trong tuần này"
            else
                "Ôn lại các từ đã học trong tuần",
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFAA00))
            ),
            iconBgColor = Color(0xFFFF6B6B),
            enabled = true,
            onClick = onSelectWeekReview
        )

        // Thông báo nếu đã học đủ
        if (uiState.wordsLearnedToday >= uiState.dailyGoal) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Hoàn thành mục tiêu!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "Bạn đã học đủ ${uiState.dailyGoal} từ hôm nay. Hãy ôn tập để nhớ lâu hơn!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyProgressCard(
    wordsLearnedToday: Int,
    dailyGoal: Int,
    totalLearnedWords: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Tiến độ hôm nay",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            val progress = if (dailyGoal > 0) wordsLearnedToday.toFloat() / dailyGoal else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF11998e),
                trackColor = Color(0xFF11998e).copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$wordsLearnedToday / $dailyGoal từ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF11998e)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.School,
                    value = "$totalLearnedWords",
                    label = "Tổng từ đã học"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF667eea),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LearnModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    iconBgColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon với gradient background
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (enabled) gradient
                        else Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) iconBgColor else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

