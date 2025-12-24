// app/src/main/java/com/example/myapplication/ui/econnect/FriendRequestsScreen.kt
package com.example.myapplication.ui.econnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.FriendRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    onBack: () -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Đã nhận", "Đã gửi")

    val incomingCount = uiState.incomingRequests.size
    val outgoingCount = uiState.outgoingRequests.size
    val totalCount = incomingCount + outgoingCount

    // Show messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Lời mời kết bạn")
                        if (totalCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = Color(0xFFf5576c),
                                contentColor = Color.White
                            ) {
                                Text(totalCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF667eea)
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) incomingCount else outgoingCount

                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = if (count > 0) "$title ($count)" else title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> IncomingRequestsList(
                    requests = uiState.incomingRequests,
                    isLoading = uiState.isLoading,
                    onAccept = { viewModel.acceptRequest(it) },
                    onReject = { viewModel.rejectRequest(it.requestId) }
                )
                1 -> OutgoingRequestsList(
                    requests = uiState.outgoingRequests,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
fun IncomingRequestsList(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF667eea).copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Không có lời mời nào",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Các lời mời kết bạn sẽ xuất hiện ở đây",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { request ->
                IncomingRequestItem(
                    request = request,
                    isLoading = isLoading,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request) }
                )
            }
        }
    }
}

@Composable
fun IncomingRequestItem(
    request: FriendRequest,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarIndex = request.fromAvatarIndex,
                size = 48
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.fromNickname.ifEmpty { request.fromUsername },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    request.fromEmail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Accept button
            IconButton(
                onClick = onAccept,
                enabled = !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF11998e).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Accept",
                    tint = Color(0xFF11998e),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Reject button
            IconButton(
                onClick = onReject,
                enabled = !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFf5576c).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Reject",
                    tint = Color(0xFFf5576c),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OutgoingRequestsList(
    requests: List<FriendRequest>,
    isLoading: Boolean
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF667eea).copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Chưa gửi lời mời nào",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Các lời mời bạn gửi sẽ xuất hiện ở đây",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { request ->
                OutgoingRequestItem(request = request)
            }
        }
    }
}

@Composable
fun OutgoingRequestItem(request: FriendRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarIndex = 0, // Target user avatar not stored in request
                size = 48
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.toUsername,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    request.toEmail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pending status
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Đang chờ",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

