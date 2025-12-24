// app/src/main/java/com/example/myapplication/ui/econnect/MatchingLoadingScreen.kt
package com.example.myapplication.ui.econnect

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingLoadingScreen(
    onMatchFound: (String) -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit,
    viewModel: MatchingViewModel = viewModel()
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val matchingState by viewModel.matchingState.collectAsState()

    // Start matching on first composition
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val email = auth.currentUser?.email ?: ""

        val currentUser = User(
            uid = uid,
            email = email,
            username = userPreferences.getUsername() ?: "",
            nickname = userPreferences.getNickname() ?: "",
            avatarIndex = userPreferences.getAvatarIndex()
        )

        viewModel.startMatching(currentUser)
    }

    // Handle state changes
    LaunchedEffect(matchingState) {
        when (val state = matchingState) {
            is MatchingState.Ready -> {
                // Room verified, safe to navigate
                onMatchFound(state.roomId)
            }
            is MatchingState.Error -> {
                // Show error briefly then navigate back
                kotlinx.coroutines.delay(2000)
                viewModel.resetState()
                onError()
            }
            is MatchingState.Cancelled -> {
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm người lạ") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelMatching()
                        onCancel()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated circles
                Box(contentAlignment = Alignment.Center) {
                    // Outer pulsing circle
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color(0xFF667eea).copy(alpha = alpha * 0.3f))
                    )

                    // Middle circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF667eea).copy(alpha = 0.2f))
                    )

                    // Inner circle with icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(EconnectPrimaryGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                when (val state = matchingState) {
                    is MatchingState.Searching -> {
                        Text(
                            "Đang tìm kiếm...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Đang tìm người phù hợp để kết nối",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        CircularProgressIndicator(
                            color = Color(0xFF667eea),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    is MatchingState.Verifying -> {
                        Text(
                            "Đang kết nối...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF667eea)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Đang tạo phòng chat...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        CircularProgressIndicator(
                            color = Color(0xFF667eea),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    is MatchingState.Ready -> {
                        Text(
                            "Đã tìm thấy!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF11998e)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Đang kết nối với ${state.matchedUser.nickname.ifEmpty { state.matchedUser.username }}...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    is MatchingState.Error -> {
                        Text(
                            "Không thể tìm thấy",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFf5576c)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        Text(
                            "Chuẩn bị...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Cancel button
                if (matchingState is MatchingState.Searching || matchingState is MatchingState.Idle) {
                    OutlinedButton(
                        onClick = {
                            viewModel.cancelMatching()
                            onCancel()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFf5576c)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hủy tìm kiếm")
                    }
                }
            }
        }
    }
}

