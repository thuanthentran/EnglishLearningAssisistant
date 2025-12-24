package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.repository.LearningProgressRepository
import com.example.myapplication.data.repository.LearningProgressRepository.DailyProgress
import com.example.myapplication.data.repository.LearningProgressRepository.LearningStats
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình chi tiết tiến trình học tập
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningProgressDetailScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(context)
    )

    val dailyProgress by viewModel.dailyProgress.collectAsState()
    val learningStats by viewModel.learningStats.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncFromCloud()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tiến trình học tập",
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF11998e))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tiến trình hôm nay
                TodayProgressSection(dailyProgress)
                Spacer(modifier = Modifier.height(20.dp))

                // Streak và thống kê tổng hợp
                StatsOverviewSection(learningStats)
                Spacer(modifier = Modifier.height(20.dp))

                // Biểu đồ tuần
                WeeklyChartSection(weeklyProgress, dailyProgress)
                Spacer(modifier = Modifier.height(20.dp))

                // Chi tiết từng mục
                DetailedProgressSection(dailyProgress, learningStats)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TodayProgressSection(dailyProgress: DailyProgress) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tiến trình hôm nay",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Text(
                    "${(dailyProgress.overallProgress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF11998e)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress items
            ProgressItem(
                icon = Icons.Default.School,
                label = "Từ vựng mới",
                current = dailyProgress.wordsLearned,
                goal = LearningProgressRepository.DAILY_WORDS_GOAL,
                progress = dailyProgress.wordsProgress,
                color = Color(0xFF11998e)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProgressItem(
                icon = Icons.Default.Refresh,
                label = "Ôn tập",
                current = dailyProgress.wordsReviewed,
                goal = LearningProgressRepository.DAILY_REVIEW_GOAL,
                progress = dailyProgress.reviewProgress,
                color = Color(0xFF667eea)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProgressItem(
                icon = Icons.Default.Quiz,
                label = "Bài quiz",
                current = dailyProgress.quizCompleted,
                goal = LearningProgressRepository.DAILY_QUIZ_GOAL,
                progress = dailyProgress.quizProgress,
                color = Color(0xFFf093fb)
            )
        }
    }
}

@Composable
private fun ProgressItem(
    icon: ImageVector,
    label: String,
    current: Int,
    goal: Int,
    progress: Float,
    color: Color
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    label,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Text(
                "$current / $goal",
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatsOverviewSection(stats: LearningStats) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Thống kê tổng hợp",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${stats.currentStreak}",
                    label = "Streak",
                    color = Color(0xFFFF6B6B),
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFAA00))
                    )
                )

                StatBox(
                    icon = Icons.Default.MenuBook,
                    value = "${stats.totalWordsLearned}",
                    label = "Từ đã học",
                    color = Color(0xFF11998e),
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                    )
                )

                StatBox(
                    icon = Icons.Default.CheckCircle,
                    value = "${(stats.overallAccuracy * 100).toInt()}%",
                    label = "Độ chính xác",
                    color = Color(0xFF667eea),
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    )
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    gradient: Brush
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyChartSection(weeklyProgress: List<DailyProgress>, todayProgress: DailyProgress) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.forLanguageTag("vi"))
    val today = dateFormat.format(Date())

    // Tạo dữ liệu cho 7 ngày gần nhất
    val last7Days = (0..6).map { daysAgo ->
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6 + daysAgo)
        }.time
    }

    val chartData = last7Days.map { date ->
        val dateStr = dateFormat.format(date)
        val dayLabel = dayFormat.format(date).uppercase()

        // Nếu là hôm nay, dùng todayProgress thay vì từ weeklyProgress
        val progress = if (dateStr == today) {
            todayProgress
        } else {
            weeklyProgress.find { it.date == dateStr }
        }

        Triple(dayLabel, progress?.overallProgress ?: 0f, progress?.wordsLearned ?: 0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Tiến trình 7 ngày qua",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                chartData.forEach { (day, progress, words) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$words",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height((80 * progress.coerceAtLeast(0.05f)).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (progress > 0) Brush.verticalGradient(
                                        colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                                    )
                                    else Brush.verticalGradient(
                                        colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedProgressSection(dailyProgress: DailyProgress, stats: LearningStats) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Chi tiết thống kê",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hôm nay
            DetailRow("Từ học hôm nay", "${dailyProgress.wordsLearned} từ")
            DetailRow("Từ ôn hôm nay", "${dailyProgress.wordsReviewed} từ")
            DetailRow("Quiz hôm nay", "${dailyProgress.quizCompleted} câu")
            DetailRow("Độ chính xác hôm nay", "${(dailyProgress.quizAccuracy * 100).toInt()}%")

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Tổng cộng
            DetailRow("Tổng từ đã học", "${stats.totalWordsLearned} từ")
            DetailRow("Tổng từ đã ôn", "${stats.totalWordsReviewed} từ")
            DetailRow("Tổng câu quiz", "${stats.totalQuizCompleted} câu")
            DetailRow("Độ chính xác tổng", "${(stats.overallAccuracy * 100).toInt()}%")
            DetailRow("Streak hiện tại", "${stats.currentStreak} ngày")
            DetailRow("Streak dài nhất", "${stats.longestStreak} ngày")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

