// app/src/main/java/com/example/myapplication/ui/econnect/EconnectHomeScreen.kt
package com.example.myapplication.ui.econnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.myapplication.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth

val EconnectPrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)

val EconnectAccentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconnectHomeScreen(
    onNavigateToStrangerMatching: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToRecentChats: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: EconnectViewModel = viewModel()
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    // Sync user to Firestore on first load
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val email = auth.currentUser?.email ?: ""

        viewModel.syncUserToFirestore(
            uid = uid,
            email = email,
            username = userPreferences.getUsername() ?: "",
            nickname = userPreferences.getNickname() ?: "",
            avatarIndex = userPreferences.getAvatarIndex(),
            avatarUri = userPreferences.getAvatarUri()
        )
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Econnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                ),
                actions = {
                    // Notification icon with badge
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFf5576c)
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier
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
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main action cards
            Text(
                "Bắt đầu trò chuyện",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stranger matching card
                EconnectActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shuffle,
                    title = "Người lạ",
                    subtitle = "Kết nối ngẫu nhiên",
                    gradient = EconnectPrimaryGradient,
                    onClick = onNavigateToStrangerMatching
                )

                // Friends card
                EconnectActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    title = "Bạn bè",
                    subtitle = "Quản lý bạn bè",
                    gradient = EconnectAccentGradient,
                    onClick = onNavigateToFriends
                )
            }

            Spacer(Modifier.height(8.dp))


            // Recent chats button
            EconnectMenuCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "Chat",
                subtitle = "Xem tất cả cuộc trò chuyện",
                onClick = onNavigateToRecentChats
            )
        }
    }
}

@Composable
fun EconnectActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF667eea).copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EconnectMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF667eea).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

