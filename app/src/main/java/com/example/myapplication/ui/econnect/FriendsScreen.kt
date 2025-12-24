// app/src/main/java/com/example/myapplication/ui/econnect/FriendsScreen.kt
package com.example.myapplication.ui.econnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Friend
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToAddFriend: () -> Unit,
    onNavigateToFriendRequests: () -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showUnfriendDialog by remember { mutableStateOf<Friend?>(null) }
    var showAddFriendSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                title = { Text("Bạn bè") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Friend requests badge
                    BadgedBox(
                        badge = {
                            if (uiState.incomingRequests.isNotEmpty()) {
                                Badge(containerColor = Color(0xFFf5576c)) {
                                    Text(uiState.incomingRequests.size.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToFriendRequests) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Friend Requests",
                                tint = Color(0xFF667eea)
                            )
                        }
                    }

                    IconButton(onClick = { showAddFriendSheet = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Add Friend",
                            tint = Color(0xFF667eea)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.friends.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF667eea).copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Chưa có bạn bè",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tìm kiếm và thêm bạn bè để bắt đầu trò chuyện",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showAddFriendSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea)
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm bạn bè")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pending requests notification
                if (uiState.incomingRequests.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFriendRequests() },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF667eea).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Color(0xFF667eea)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Lời mời kết bạn đang chờ",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF667eea)
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF667eea)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                items(uiState.friends) { friend ->
                    FriendItem(
                        friend = friend,
                        onChatClick = {
                            scope.launch {
                                val roomId = viewModel.openChatWithFriend(friend)
                                if (roomId != null) {
                                    onNavigateToChat(roomId)
                                }
                            }
                        },
                        onUnfriendClick = {
                            showUnfriendDialog = friend
                        }
                    )
                }
            }
        }
    }

    // Unfriend confirmation dialog
    showUnfriendDialog?.let { friend ->
        AlertDialog(
            onDismissRequest = { showUnfriendDialog = null },
            title = { Text("Hủy kết bạn") },
            text = {
                Text("Bạn có chắc muốn hủy kết bạn với ${friend.friendNickname.ifEmpty { friend.friendUsername }}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unfriend(friend.friendUid)
                        showUnfriendDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFf5576c)
                    )
                ) {
                    Text("Hủy kết bạn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfriendDialog = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Add Friend BottomSheet
    if (showAddFriendSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddFriendSheet = false
                viewModel.clearSearch()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddFriendContent(
                viewModel = viewModel,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showAddFriendSheet = false
                        viewModel.clearSearch()
                    }
                }
            )
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    onChatClick: () -> Unit,
    onUnfriendClick: () -> Unit
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
                avatarIndex = friend.friendAvatarIndex,
                size = 48
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendNickname.ifEmpty { friend.friendUsername },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    friend.friendEmail,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chat button
            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF667eea).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat",
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // More options
            var expanded by remember { mutableStateOf(false) }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Hủy kết bạn", color = Color(0xFFf5576c)) },
                        onClick = {
                            expanded = false
                            onUnfriendClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = Color(0xFFf5576c)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFriendContent(
    viewModel: FriendsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    // Show messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            // Auto close on success
            onDismiss()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Thêm bạn bè",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }


        Spacer(Modifier.height(16.dp))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email hoặc username") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.clearSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.searchUser(searchQuery)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF667eea),
                cursorColor = Color(0xFF667eea)
            )
        )

        Spacer(Modifier.height(12.dp))

        // Search button
        Button(
            onClick = { viewModel.searchUser(searchQuery) },
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank() && !uiState.isSearching,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667eea)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Tìm kiếm")
        }

        Spacer(Modifier.height(24.dp))

        // Search result
        when {
            uiState.searchError != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFf5576c).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFf5576c)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            uiState.searchError!!,
                            color = Color(0xFFf5576c)
                        )
                    }
                }
            }

            uiState.searchResult != null -> {
                val user = uiState.searchResult!!

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            avatarIndex = user.avatarIndex,
                            size = 72
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            user.nickname.ifEmpty { user.username },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        if (user.nickname.isNotEmpty()) {
                            Text(
                                "@${user.username}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            user.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.sendFriendRequest(user) },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF667eea)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gửi lời mời kết bạn")
                        }
                    }
                }
            }

            else -> {
                // Empty state / instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF667eea).copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bắt đầu tìm kiếm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kết quả sẽ hiển thị ở đây",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
