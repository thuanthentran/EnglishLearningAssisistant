package com.example.myapplication.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.ui.theme.MyApplicationTheme

/* =========================
   BOTTOM TAB
   ========================= */
enum class BottomTab {
    CHAT_RANDOM,
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
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {}
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
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    // Sử dụng MaterialTheme colors cho dark mode support
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("E", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "English Learning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textColor
                            )
                            Text(
                                "Học mỗi ngày, tiến bộ mỗi ngày",
                                fontSize = 12.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF667eea)
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF667eea)
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFf5576c)
                        )
                    }
                }
            )
        },

        /* =========================
           BOTTOM NAV BAR
           ========================= */
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.CHAT_RANDOM,
                    onClick = { selectedTab = BottomTab.CHAT_RANDOM },
                    icon = {
                        Icon(
                            if (selectedTab == BottomTab.CHAT_RANDOM) Icons.Filled.People else Icons.Outlined.People,
                            null,
                            tint = if (selectedTab == BottomTab.CHAT_RANDOM) Color(0xFF667eea) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Người lạ",
                            color = if (selectedTab == BottomTab.CHAT_RANDOM) Color(0xFF667eea) else Color.Gray,
                            fontWeight = if (selectedTab == BottomTab.CHAT_RANDOM) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF667eea).copy(alpha = 0.1f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { selectedTab = BottomTab.HOME },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(if (selectedTab == BottomTab.HOME) 52.dp else 40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedTab == BottomTab.HOME) PrimaryGradient
                                    else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            "Home",
                            color = if (selectedTab == BottomTab.HOME) Color(0xFF667eea) else Color.Gray,
                            fontWeight = if (selectedTab == BottomTab.HOME) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == BottomTab.CHAT_AI,
                    onClick = { selectedTab = BottomTab.CHAT_AI },
                    icon = {
                        Icon(
                            if (selectedTab == BottomTab.CHAT_AI) Icons.Filled.SmartToy else Icons.Outlined.SmartToy,
                            null,
                            tint = if (selectedTab == BottomTab.CHAT_AI) Color(0xFF667eea) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "AI",
                            color = if (selectedTab == BottomTab.CHAT_AI) Color(0xFF667eea) else Color.Gray,
                            fontWeight = if (selectedTab == BottomTab.CHAT_AI) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF667eea).copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { padding ->

        when (selectedTab) {

            /* =========================
               HOME TAB
               ========================= */
            BottomTab.HOME -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    UserProfileCard(username, email, avatarIndex)
                    Spacer(Modifier.height(20.dp))

                    DailyGoalSection()
                    Spacer(Modifier.height(20.dp))

                    LearningFeaturesSection(onVocabularyClick, onHomeworkClick, onWritingPracticeClick, onImageLearningClick, onSpeakingPracticeClick)
                    Spacer(Modifier.height(20.dp))

                    StatisticsSection()
                    Spacer(Modifier.height(24.dp))
                }
            }

            /* =========================
               CHAT RANDOM TAB
               ========================= */
            BottomTab.CHAT_RANDOM -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF667eea).copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Chat với người lạ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            "Tính năng đang phát triển",
                            color = secondaryTextColor
                        )
                    }
                }
            }

            /* =========================
               CHAT AI TAB
               ========================= */
            BottomTab.CHAT_AI -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AIFeaturesSection(
                        onHomeworkClick = onHomeworkClick,
                        onWritingPracticeClick = onWritingPracticeClick,
                        onImageLearningClick = onImageLearningClick,
                        onSpeakingPracticeClick = onSpeakingPracticeClick
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/* =========================
   SECTIONS
   ========================= */
@Composable
fun UserProfileCard(username: String, email: String, avatarIndex: Int = 0) {
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
            ),
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
                // Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(currentAvatar.second),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        currentAvatar.first,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
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
                                "7",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Text(
                        "ngày liên tiếp",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DailyGoalSection() {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF11998e).copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Mục tiêu hôm nay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Text(
                            "5/10 từ vựng",
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    "50%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF11998e)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Custom progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF11998e).copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGradient)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Còn 5 từ nữa!", color = secondaryTextColor, fontSize = 12.sp)
                Text("🎯 Cố lên!", color = Color(0xFF11998e), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LearningFeaturesSection(onVocabularyClick: () -> Unit, onHomeworkClick: () -> Unit = {}, onWritingPracticeClick: () -> Unit = {}, onImageLearningClick: () -> Unit = {}, onSpeakingPracticeClick: () -> Unit = {}) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bắt đầu học",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
            TextButton(onClick = {}) {
                Text("Xem tất cả", color = Color(0xFF667eea))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.School,
                title = "Từ vựng",
                description = "Học từ mới",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                ),
                iconBgColor = Color(0xFF4CAF50),
                onClick = onVocabularyClick
            )

            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = "Đọc",
                description = "Đọc hiểu",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF9800), Color(0xFFFFC107))
                ),
                iconBgColor = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun AIFeaturesSection(
    onHomeworkClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onImageLearningClick: () -> Unit = {},
    onSpeakingPracticeClick: () -> Unit = {}
) {
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))

        // Header with AI icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tính năng AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Sử dụng trí tuệ nhân tạo để hỗ trợ học tập",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Nói - Speaking Practice
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.RecordVoiceOver,
                title = "Nói",
                description = "Luyện nói với AI",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF03A9F4))
                ),
                iconBgColor = Color(0xFF2196F3),
                onClick = onSpeakingPracticeClick
            )

            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                title = "Giải Bài",
                description = "Giải bài tập với AI",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A6F))
                ),
                iconBgColor = Color(0xFFFF6B6B),
                onClick = onHomeworkClick
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Viết",
                description = "Luyện viết với AI",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
                ),
                iconBgColor = Color(0xFF9C27B0),
                onClick = onWritingPracticeClick
            )

            ModernFeatureCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
                title = "Học qua ảnh",
                description = "Nhận diện vật với AI",
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF00BCD4), Color(0xFF009688))
                ),
                iconBgColor = Color(0xFF00BCD4),
                onClick = onImageLearningClick
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
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(140.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = iconBgColor.copy(alpha = 0.3f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) {
                onClick?.invoke()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative circle in background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 70.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(iconBgColor.copy(alpha = 0.1f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradient),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

@Composable
fun ModernStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
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
                color = textColor
            )
            Text(
                label,
                fontSize = 11.sp,
                color = secondaryTextColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    MyApplicationTheme {
        HomeScreenContent(
            username = "Nguyen Van A",
            email = "test@gmail.com",
            avatarIndex = 0,
            onLogout = {},
            onVocabularyClick = {},
            onSettingsClick = {}
        )
    }
}
