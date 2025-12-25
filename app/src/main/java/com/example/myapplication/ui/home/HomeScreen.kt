package com.example.myapplication.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.repository.LearningProgressRepository.DailyProgress
import com.example.myapplication.ui.econnect.EconnectNavHost
import com.example.myapplication.ui.econnect.EconnectRoute
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.Calendar

/* =========================
   BOTTOM TAB
   ========================= */
enum class BottomTab {
    ECONNECT,
    HOME,
    CHAT_AI
}

/* =========================
   ENTRY
   ========================= */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onVocabularyClick: () -> Unit,
    onHomeworkClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onGameClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {},
    onSourceScreenSet: (String) -> Unit = {}  // Callback để set source screen
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    HomeScreenContent(
        username = userPreferences.getNickname()?.takeIf { it.isNotEmpty() } ?: userPreferences.getUsername() ?: "User",
        email = userPreferences.getEmail() ?: "No Email",
        avatarIndex = userPreferences.getAvatarIndex(),
        onLogout = {
            userPreferences.clearUserSession()
            onLogout()
        },
        onVocabularyClick = onVocabularyClick,
        onHomeworkClick = onHomeworkClick,
        onSettingsClick = onSettingsClick,
        onGameClick = onGameClick,
        onLearnWordsClick = onLearnWordsClick,
        onWritingPracticeClick = onWritingPracticeClick,
        onImageLearningClick = onImageLearningClick,
        onSpeakingPracticeClick = onSpeakingPracticeClick
    )
}

// Màu sắc gradient đẹp
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
)


/* =========================
   CONTENT
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    username: String,
    email: String,
    avatarIndex: Int = 0,
    onLogout: () -> Unit,
    onVocabularyClick: () -> Unit,
    onHomeworkClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onGameClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {},
    onSourceScreenSet: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    // Sử dụng MaterialTheme colors cho dark mode support
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Bottom Navigation Bar - 3 tabs only: Econnect, Home, AI
    val bottomBar: @Composable () -> Unit = {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Econnect Tab
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedTab == BottomTab.ECONNECT)
                                Color(0xFF5B86E5)
                            else Color.Transparent
                        )
                        .clickable { selectedTab = BottomTab.ECONNECT },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.People,
                        contentDescription = "Econnect",
                        tint = if (selectedTab == BottomTab.ECONNECT)
                            Color.White
                        else Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Home Tab
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedTab == BottomTab.HOME)
                                Color(0xFF5B86E5)
                            else Color.Transparent
                        )
                        .clickable { selectedTab = BottomTab.HOME },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = if (selectedTab == BottomTab.HOME)
                            Color.White
                        else Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // AI Tab
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedTab == BottomTab.CHAT_AI)
                                Color(0xFF5B86E5)
                            else Color.Transparent
                        )
                        .clickable { selectedTab = BottomTab.CHAT_AI },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = "AI",
                        tint = if (selectedTab == BottomTab.CHAT_AI)
                            Color.White
                        else Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Switch between root screens - NO animations, instant switch
    when (selectedTab) {
        /* =========================
           ECONNECT ROOT - Has its own Scaffold
           ========================= */
        BottomTab.ECONNECT -> {
            EconnectRoot(bottomBar = bottomBar)
        }

        /* =========================
           HOME ROOT
           ========================= */
        BottomTab.HOME -> {
            HomeRoot(
                username = username,
                email = email,
                avatarIndex = avatarIndex,
                onLogout = onLogout,
                onVocabularyClick = onVocabularyClick,
                onHomeworkClick = onHomeworkClick,
                onSettingsClick = onSettingsClick,
                onGameClick = onGameClick,
                onLearnWordsClick = onLearnWordsClick,
                onWritingPracticeClick = onWritingPracticeClick,
                onImageLearningClick = onImageLearningClick,
                onSpeakingPracticeClick = onSpeakingPracticeClick,
                bottomBar = bottomBar
            )
        }

        /* =========================
           AI CHAT ROOT
           ========================= */
        BottomTab.CHAT_AI -> {
            AiRoot(
                bottomBar = bottomBar,
                onHomeworkClick = onHomeworkClick,
                onWritingPracticeClick = onWritingPracticeClick,
                onImageLearningClick = onImageLearningClick,
                onSpeakingPracticeClick = onSpeakingPracticeClick,
                onLearnWordsClick = onLearnWordsClick,
                onBack = { selectedTab = BottomTab.HOME }
            )
        }
    }
}

/* =========================
   HOME ROOT SCREEN
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoot(
    username: String,
    email: String,
    avatarIndex: Int,
    onLogout: () -> Unit,
    onVocabularyClick: () -> Unit,
    onHomeworkClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGameClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(context)
    )

    val dailyProgress by viewModel.dailyProgress.collectAsState()
    val learningStats by viewModel.learningStats.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()

    // State để điều hướng đến màn hình chi tiết
    var showProgressDetail by remember { mutableStateOf(false) }

    if (showProgressDetail) {
        LearningProgressDetailScreen(
            onBack = { showProgressDetail = false }
        )
    } else {
        val backgroundColor = MaterialTheme.colorScheme.background
        val surfaceColor = MaterialTheme.colorScheme.surface
        val textColor = MaterialTheme.colorScheme.onBackground
        val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = bottomBar
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // New compact header
                CompactHeader(
                    avatarIndex = avatarIndex,
                    totalWordsLearned = learningStats.totalWordsLearned,
                    currentStreak = learningStats.currentStreak,
                    onAvatarClick = onSettingsClick
                )
                Spacer(Modifier.height(24.dp))

                // Title and Percentage outside the card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            stringResource(com.example.myapplication.R.string.complete),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            stringResource(com.example.myapplication.R.string.todays_goal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Top) {
                            // Use today's overallProgress (average of words/review/quiz progress)
                            val progress = dailyProgress.overallProgress
                            val percentage = (progress * 100).roundToInt().coerceIn(0, 100)
                            Text(
                                "$percentage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "%",
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        // Daily change indicator: compare today's overallProgress with yesterday's
                        Spacer(Modifier.height(4.dp))
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val todayStr = dailyProgress.date.ifEmpty { dateFormat.format(java.util.Date()) }
                        val cal = java.util.Calendar.getInstance()
                        try {
                            cal.time = dateFormat.parse(todayStr) ?: java.util.Date()
                        } catch (e: Exception) {
                            // fallback to now
                            cal.time = java.util.Date()
                        }
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val yesterdayStr = dateFormat.format(cal.time)

                        val yesterdayProgress = weeklyProgress.associateBy { it.date }[yesterdayStr]?.overallProgress ?: 0f
                        val todayProgress = dailyProgress.overallProgress
                        // User requested: show simple percentage-point difference (today% - yesterday%),
                        // e.g. yesterday 90%, today 80% -> -10%; yesterday 70%, today 80% -> +10%
                        val rawDelta = (todayProgress - yesterdayProgress) * 100f
                        val changePercent = kotlin.math.abs(rawDelta)
                        val isPositive = rawDelta >= 0f

                        DailyChangeIndicator(
                            changePercent = changePercent,
                            isPositive = isPositive
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Combined Study Progress Section
                StudyProgressSection(
                    wordsLearned = dailyProgress.wordsLearned,
                    dailyGoal = com.example.myapplication.data.repository.LearningProgressRepository.DAILY_WORDS_GOAL,
                    weeklyProgress = weeklyProgress,
                    onClick = { showProgressDetail = true }
                )
                Spacer(Modifier.height(20.dp))

                LearningFeaturesSection(
                    onVocabularyClick = onVocabularyClick,
                    onHomeworkClick = onHomeworkClick,
                    onGameClick = onGameClick,
                    onLearnWordsClick = onLearnWordsClick,
                    onWritingPracticeClick = onWritingPracticeClick,
                    onImageLearningClick = onImageLearningClick,
                    onSpeakingPracticeClick = onSpeakingPracticeClick
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/* =========================
   ECONNECT ROOT SCREEN - Completely separate Scaffold
   ========================= */
@Composable
fun EconnectRoot(bottomBar: @Composable () -> Unit) {
    val econnectNavController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            EconnectNavHost(
                navController = econnectNavController,
                onNavigateToNotifications = {
                    econnectNavController.navigate(EconnectRoute.Notifications.route)
                }
            )
        }
    }
}

/* =========================
   AI ROOT SCREEN
   ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRoot(
    bottomBar: @Composable () -> Unit,
    onHomeworkClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    "AI Features",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(12.dp))

            AIFeaturesSection(
                onHomeworkClick = onHomeworkClick,
                onWritingPracticeClick = onWritingPracticeClick,
                onImageLearningClick = onImageLearningClick,
                onSpeakingPracticeClick = onSpeakingPracticeClick,
                onLearnWordsClick = onLearnWordsClick
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* =========================
   SECTIONS
   ========================= */

/**
 * Daily change indicator - shows percentage increase/decrease
 */
@Composable
fun DailyChangeIndicator(
    changePercent: Float,
    isPositive: Boolean
) {
    val color = if (isPositive) Color(0xFF48BB78) else Color(0xFFE53E3E)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Circle with arrow
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.ArrowDownward,
                contentDescription = if (isPositive) "Increase" else "Decrease",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }

        // Show percentage with one decimal place (e.g. +2.3%) as requested
        val display = String.format(java.util.Locale.getDefault(), "%.1f", changePercent)
        Text(
            text = "${if (isPositive) "+" else "-"}$display%",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CompactHeader(
    avatarIndex: Int = 0,
    totalWordsLearned: Int = 0,
    currentStreak: Int = 0,
    onAvatarClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val avatarUri = userPreferences.getAvatarUri()

    val avatarIcons = listOf(
        Icons.Default.Person to Color(0xFF667eea),
        Icons.Default.Face to Color(0xFF11998e),
        Icons.Default.SentimentSatisfied to Color(0xFFf5576c),
        Icons.Default.School to Color(0xFFFF9800),
        Icons.Default.Star to Color(0xFFFFD700),
        Icons.Default.EmojiEmotions to Color(0xFF9C27B0),
        Icons.Default.Pets to Color(0xFF4CAF50),
        Icons.Default.SportsEsports to Color(0xFF2196F3),
    )

    val currentAvatar = avatarIcons.getOrElse(avatarIndex) { avatarIcons[0] }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar với border gradient
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    )
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(currentAvatar.second),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUri)
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        currentAvatar.first,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Metrics Row - Single pill containing both metrics
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Words Learned Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4FC3F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📘", fontSize = 12.sp)
                    }
                    Text(
                        "$totalWordsLearned",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Streak Metric
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", fontSize = 12.sp)
                    }
                    Text(
                        "$currentStreak",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileCard(
    username: String,
    email: String,
    avatarIndex: Int = 0,
    currentStreak: Int = 0,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val avatarUri = userPreferences.getAvatarUri()

    // Avatar options matching SettingsScreen
    val avatarIcons = listOf(
        Icons.Default.Person to Color(0xFF667eea),
        Icons.Default.Face to Color(0xFF11998e),
        Icons.Default.SentimentSatisfied to Color(0xFFf5576c),
        Icons.Default.School to Color(0xFFFF9800),
        Icons.Default.Star to Color(0xFFFFD700),
        Icons.Default.EmojiEmotions to Color(0xFF9C27B0),
        Icons.Default.Pets to Color(0xFF4CAF50),
        Icons.Default.SportsEsports to Color(0xFF2196F3),
    )

    val currentAvatar = avatarIcons.getOrElse(avatarIndex) { avatarIcons[0] }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF667eea).copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryGradient)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with custom image support
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(currentAvatar.second),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        // Show custom uploaded image
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Show icon avatar
                        Icon(
                            currentAvatar.first,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            email,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Streak badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 16.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$currentStreak",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Text(
                        stringResource(com.example.myapplication.R.string.consecutive_days),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StudyProgressSection(
    wordsLearned: Int = 0,
    dailyGoal: Int = 10,
    weeklyProgress: List<DailyProgress> = emptyList(),
    onClick: () -> Unit = {}
) {
    // Get day names based on current locale
    val dayLabels = if (java.util.Locale.getDefault().language == "vi") {
        listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    // Generate Monday to Sunday of current week
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val last7Days = mutableListOf<String>()
    val calendar = Calendar.getInstance()

    // Set to Monday of current week (Calendar.MONDAY = 2)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    calendar.add(Calendar.DAY_OF_YEAR, -daysToMonday)

    // Add 7 days from Monday
    repeat(7) {
        last7Days.add(dateFormat.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Create a map of date -> DailyProgress for easier lookup
    val progressMap = weeklyProgress.associateBy { it.date }

    // Build display data for 7 days
    val displayData = last7Days.map { date ->
        progressMap[date] ?: DailyProgress(date = date, wordsLearned = 0)
    }

    // Find max words learned to normalize the chart
    val maxWords = displayData.maxOfOrNull { it.wordsLearned }?.coerceAtLeast(1) ?: dailyGoal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header: Study Success + Learn more button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(com.example.myapplication.R.string.study_success),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2D3748)
                )

                // Learn more button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Text(
                        stringResource(com.example.myapplication.R.string.learn_more),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color(0xFF2D3748),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Weekly bar chart showing words learned per day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Display 7 days
                repeat(7) { dayIndex ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Words count label above bar
                        if (dayIndex < displayData.size) {
                            Text(
                                displayData[dayIndex].wordsLearned.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D3748),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Bar chart
                        val barHeight = if (dayIndex < displayData.size) {
                            (displayData[dayIndex].wordsLearned.toFloat() / maxWords * 45).dp
                        } else {
                            0.dp
                        }

                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (dayIndex < displayData.size && displayData[dayIndex].wordsLearned > 0) {
                                        Color(0xFF5B86E5)
                                    } else {
                                        Color(0xFF5B86E5).copy(alpha = 0.2f)
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF718096),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DailyGoalSection(
    wordsLearned: Int = 0,
    dailyGoal: Int = 10,
    currentStreak: Int = 0,
    overallProgress: Float = 0f,
    onClick: () -> Unit = {}
) {
    val progress = if (dailyGoal > 0) (wordsLearned.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()
    val changePercent = if (percentage > 0) "+${(percentage * 0.03f).toInt()}.${(percentage * 0.3f).toInt() % 10}%" else "0%"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Complete",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Today's Goal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "$percentage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 42.sp
                        )
                        Text(
                            "%",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF48BB78),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            changePercent,
                            color = Color(0xFF48BB78),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress bar with rounded ends
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE8EDF3))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(com.example.myapplication.R.string.words_learned_progress, wordsLearned, dailyGoal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (percentage >= 100) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF48BB78).copy(alpha = 0.15f)
                    ) {
                        Text(
                            stringResource(com.example.myapplication.R.string.completed),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF48BB78),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LearningFeaturesSection(
    onVocabularyClick: () -> Unit,
    onHomeworkClick: () -> Unit = {},
    onGameClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {}
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        // Section Header
        Text(
            stringResource(com.example.myapplication.R.string.activities),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        // Activity Item 1 - Dictionary (Pastel yellow)
        ActivityCard(
            icon = "📖",
            cardBgColor = Color(0xFFFFF4E0),
            title = stringResource(com.example.myapplication.R.string.dictionary),
            subtitle = stringResource(com.example.myapplication.R.string.dictionary_subtitle),
            onClick = onVocabularyClick
        )

        Spacer(Modifier.height(12.dp))

        // Activity Item 2 - Games (Pastel green)
        ActivityCard(
            icon = "🎮",
            cardBgColor = Color(0xFFdff5e0),
            title = stringResource(com.example.myapplication.R.string.games),
            subtitle = stringResource(com.example.myapplication.R.string.games_subtitle),
            onClick = onGameClick
        )

        Spacer(Modifier.height(12.dp))

        // Activity Item 3 - Learn Words (Pastel purple)
        ActivityCard(
            icon = "📚",
            cardBgColor = Color(0xFFe8e0f5), // Pastel purple
            title = stringResource(com.example.myapplication.R.string.learn_words),
            subtitle = stringResource(com.example.myapplication.R.string.learn_words_subtitle),
            onClick = onLearnWordsClick
        )
    }
}

@Composable
fun ActivityCard(
    icon: String,
    cardBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // Define badge border colors based on card
    val badgeBorderColor = when (cardBgColor) {
        Color(0xFFFFF4E0) -> Color(0xFFe8c46f) // Dictionary - darker yellow border
        Color(0xFFdff5e0) -> Color(0xFFa8daa1) // Games - darker green border
        Color(0xFFe8e0f5) -> Color(0xFFc5b3dd) // Learn Words - darker purple border
        else -> Color(0xFFc5b3dd) // Default
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle with border
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(4.dp, badgeBorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 28.sp)
            }

            Spacer(Modifier.width(14.dp))

            // Title and subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF2D3748)
                )
                Text(
                    subtitle,
                    color = Color(0xFF718096),
                    fontSize = 13.sp
                )
            }

            // More options icon
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = "More",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ChallengeCard(
    icon: String,
    iconBgColor: Color,
    cardBgColor: Color,
    title: String,
    subtitle: String,
    points: String,
    streakBonus: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 28.sp)
            }

            Spacer(Modifier.width(14.dp))

            // Title and subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            // Points badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (streakBonus != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4FC3F7).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("💎", fontSize = 10.sp)
                            Text(
                                streakBonus,
                                color = Color(0xFF0288D1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFD54F).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("🪙", fontSize = 10.sp)
                        Text(
                            points,
                            color = Color(0xFFF57C00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // More options
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = "More",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AIFeaturesSection(
    onHomeworkClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {},
    onLearnWordsClick: () -> Unit = {}
) {
    // Enable Homework Help only in debug builds to safely test; wrap click in try/catch to avoid crash.
    val homeworkEnabled = BuildConfig.DEBUG
    var showHomeworkDisabledDialog by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))

        // Homework Help - Pastel Purple (added so the feature is visible)
        AICard(
            icon = Icons.Default.School,
            title = stringResource(com.example.myapplication.R.string.homework_help),
            description = stringResource(com.example.myapplication.R.string.homework_help_subtitle),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFB39DDB), Color(0xFF9575CD))
            ),
            arrowColor = Color(0xFF9575CD),
            onClick = {
                if (homeworkEnabled) {
                    try {
                        onHomeworkClick()
                    } catch (e: Exception) {
                        android.util.Log.e("HomeScreen", "onHomeworkClick failed", e)
                        showHomeworkDisabledDialog = true
                    }
                } else {
                    showHomeworkDisabledDialog = true
                }
            }
        )

        if (showHomeworkDisabledDialog) {
            AlertDialog(
                onDismissRequest = { showHomeworkDisabledDialog = false },
                title = { Text("Tạm thời vô hiệu") },
                text = { Text("Tính năng Trợ giúp bài tập đang tạm thời bị vô hiệu hóa để tránh lỗi. Chúng tôi sẽ khôi phục sớm.") },
                confirmButton = {
                    TextButton(onClick = { showHomeworkDisabledDialog = false }) {
                        Text("Đóng")
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Speaking Practice - Pastel Red
        AICard(
            icon = Icons.Default.RecordVoiceOver,
            title = stringResource(com.example.myapplication.R.string.speaking_practice),
            description = stringResource(com.example.myapplication.R.string.speaking_practice_subtitle),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFFF9999), Color(0xFFFF7777))
            ),
            arrowColor = Color(0xFFFF7777),
            onClick = onSpeakingPracticeClick
        )

        Spacer(Modifier.height(12.dp))

        // Writing Practice - Pastel Blue
        AICard(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(com.example.myapplication.R.string.writing_practice),
            description = stringResource(com.example.myapplication.R.string.writing_practice_subtitle),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7))
            ),
            arrowColor = Color(0xFF4FC3F7),
            onClick = onWritingPracticeClick
        )

        Spacer(Modifier.height(12.dp))

        // Image Learning - Pastel Green
        AICard(
            icon = Icons.Default.CameraAlt,
            title = stringResource(com.example.myapplication.R.string.image_learning),
            description = stringResource(com.example.myapplication.R.string.image_learning_subtitle),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF81C784), Color(0xFF66BB6A))
            ),
            arrowColor = Color(0xFF66BB6A),
            onClick = onImageLearningClick
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AICard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    arrowColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    .background(gradient),
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = arrowColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CleanFeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ModernFeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    iconBgColor: Color,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
                .height(120.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
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
}

@Composable
fun StatisticsSection() {
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Thống kê của bạn",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                value = "127",
                label = "Điểm",
                color = Color(0xFFf5576c)
            )
            ModernStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DateRange,
                value = "7",
                label = "Ngày streak",
                color = Color(0xFF667eea)
            )
            ModernStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = "42",
                label = "Hoàn thành",
                color = Color(0xFF11998e)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Simple preview without full HomeScreenContent
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header preview
                CompactHeader(
                    avatarIndex = 0,
                    totalWordsLearned = 144,
                    currentStreak = 7,
                    onAvatarClick = {}
                )

                Spacer(Modifier.height(24.dp))

                // Title and Percentage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Complete",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF2D3748)
                        )
                        Text(
                            "Today's Goal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF2D3748)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "78",
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = Color(0xFF2D3748)
                            )
                            Text(
                                "%",
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = Color(0xFF2D3748),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        DailyChangeIndicator(
                            changePercent = 2.3f,
                            isPositive = true
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Study Progress preview
                StudyProgressSection(
                    wordsLearned = 8,
                    dailyGoal = 10,
                    onClick = {}
                )

                Spacer(Modifier.height(20.dp))

                // Activities title
                Text(
                    "Activities",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2D3748)
                )

                Spacer(Modifier.height(16.dp))

                // Activity cards preview with correct icons
                Column(Modifier.padding(horizontal = 20.dp)) {
                    ActivityCard(
                        icon = "📖",
                        cardBgColor = Color(0xFFFFF4E0),
                        title = "Dictionary",
                        subtitle = "Look up words",
                        onClick = {}
                    )

                    Spacer(Modifier.height(12.dp))

                    ActivityCard(
                        icon = "🎮",
                        cardBgColor = Color(0xFFdff5e0),
                        title = "Games",
                        subtitle = "Learn through games",
                        onClick = {}
                    )

                    Spacer(Modifier.height(12.dp))

                    ActivityCard(
                        icon = "📚",
                        cardBgColor = Color(0xFFe8e0f5),
                        title = "Learn Words",
                        subtitle = "Daily vocabulary",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
