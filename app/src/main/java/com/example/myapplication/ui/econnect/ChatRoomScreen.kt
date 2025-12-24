// app/src/main/java/com/example/myapplication/ui/econnect/ChatRoomScreen.kt
package com.example.myapplication.ui.econnect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatType
import com.example.myapplication.utils.MessageAnalysis
import com.example.myapplication.utils.TranslationResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    onNavigateToFriendRequests: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Bottom sheet state for message analysis
    val analysisSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load chat room
    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            viewModel.loadChatRoom(roomId)
        }
    }

    // Handle critical errors - go back if room not found
    LaunchedEffect(uiState.error, uiState.isLoading) {
        if (!uiState.isLoading && uiState.error != null && uiState.chatRoom == null) {
            // Critical error - room doesn't exist, go back after showing error
            kotlinx.coroutines.delay(2000)
            onBack()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Safe access to values - handle null cases
    val otherUser = uiState.otherUser
    val chatRoom = uiState.chatRoom
    val chatType = remember(chatRoom) {
        if (chatRoom != null) {
            try {
                ChatType.valueOf(chatRoom.chatType)
            } catch (_: Exception) {
                ChatType.STRANGER
            }
        } else {
            ChatType.STRANGER
        }
    }
    val displayName = otherUser?.nickname?.ifEmpty { otherUser.username } ?: "Đang tải..."

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        UserAvatar(
                            avatarIndex = otherUser?.avatarIndex ?: 0,
                            size = 40
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            // Relationship badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (chatType == ChatType.FRIEND)
                                                Color(0xFF11998e).copy(alpha = 0.1f)
                                            else
                                                Color(0xFF667eea).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (chatType == ChatType.FRIEND) "Bạn bè" else "Người lạ",
                                        fontSize = 11.sp,
                                        color = if (chatType == ChatType.FRIEND)
                                            Color(0xFF11998e)
                                        else
                                            Color(0xFF667eea)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show add friend button if stranger and not already sent request
                    if (chatType == ChatType.STRANGER && !uiState.isFriend && !uiState.friendRequestSent) {
                        IconButton(onClick = { viewModel.sendFriendRequest() }) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Add Friend",
                                tint = Color(0xFF667eea)
                            )
                        }
                    }

                    if (uiState.friendRequestSent) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Request Sent",
                            tint = Color(0xFF11998e),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Message input
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nhập tin nhắn...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667eea),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        maxLines = 4
                    )

                    Spacer(Modifier.width(8.dp))

                    // Send button
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.isNotBlank())
                                    Color(0xFF667eea)
                                else
                                    Color.Gray.copy(alpha = 0.3f)
                            )
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF667eea))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                                items(uiState.messages) { message ->
                    MessageBubbleWithAI(
                        message = message,
                        isMe = message.senderId == uiState.currentUserId,
                        translation = uiState.translations[message.messageId],
                        isTranslating = uiState.isTranslating == message.messageId,
                        onTranslate = { viewModel.translateMessage(message.messageId, message.content) },
                        onHideTranslation = { viewModel.hideTranslation(message.messageId) },
                        onAnalyzeMessage = {
                            viewModel.analyzeMessage(message.content)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Message Analysis Bottom Sheet
            if (uiState.showAnalysisDialog) {
                MessageAnalysisBottomSheet(
                    messageAnalysis = uiState.messageAnalysis,
                    isLoading = uiState.isAnalyzing,
                    sheetState = analysisSheetState,
                    onDismiss = { viewModel.closeAnalysisDialog() }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    val backgroundColor = if (isMe) Color(0xFF667eea) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 15.sp
                )
            }

            // Timestamp
            Text(
                text = formatMessageTime(message.createdAt?.toDate()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun UserAvatar(
    avatarIndex: Int,
    size: Int,
    modifier: Modifier = Modifier
) {
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

    val (icon, color) = avatarIcons.getOrElse(avatarIndex) { avatarIcons[0] }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.6).dp)
        )
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""

    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}

// ==================== AI LEARNING COMPONENTS ====================

/**
 * Message bubble with AI features (translate, analyze message)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleWithAI(
    message: ChatMessage,
    isMe: Boolean,
    translation: TranslationResult?,
    isTranslating: Boolean,
    onTranslate: () -> Unit,
    onHideTranslation: () -> Unit,
    onAnalyzeMessage: () -> Unit
) {
    val backgroundColor = if (isMe) Color(0xFF667eea) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMe) Arrangement.End else Arrangement.Start

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { showMenu = true }
                        )
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }

                // Dropdown menu for AI actions
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF667eea)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (translation != null) "Ẩn bản dịch" else "Dịch")
                            }
                        },
                        onClick = {
                            showMenu = false
                            if (translation != null) {
                                onHideTranslation()
                            } else {
                                onTranslate()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF11998e)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Phân tích tin nhắn")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onAnalyzeMessage()
                        }
                    )
                }
            }

            // Show translation loading
            if (isTranslating) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF667eea)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Đang dịch...",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Show translation result
            if (translation != null && !isTranslating) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(top = 4.dp)
                        .background(
                            color = Color(0xFF667eea).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = if (translation.originalLanguage == "EN") "🇻🇳 Tiếng Việt" else "🇬🇧 English",
                            fontSize = 10.sp,
                            color = Color(0xFF667eea),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = translation.translatedText,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // Timestamp
            Text(
                text = formatMessageTime(message.createdAt?.toDate()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Bottom Sheet showing message analysis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageAnalysisBottomSheet(
    messageAnalysis: MessageAnalysis?,
    isLoading: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = Color(0xFF11998e),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Phân tích tin nhắn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF667eea))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Đang phân tích...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (messageAnalysis != null) {
                // Message analysis content
                Column {
                    // Original message
                    Text(
                        text = "Tin nhắn:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF667eea).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = messageAnalysis.message,
                            fontSize = 15.sp,
                            color = Color(0xFF667eea),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Show error state if parsing failed
                    if (messageAnalysis.isError) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFFD32F2F)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Lỗi phân tích",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = messageAnalysis.meaning,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Meaning
                        Text(
                            text = "Nghĩa:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = messageAnalysis.meaning,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Grammar
                        if (messageAnalysis.grammar.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Ngữ pháp:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = messageAnalysis.grammar,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Vocabulary
                        if (messageAnalysis.vocabulary.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Từ vựng quan trọng:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            messageAnalysis.vocabulary.forEach { vocab ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFF11998e).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = vocab.word,
                                            fontSize = 13.sp,
                                            color = Color(0xFF11998e),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = vocab.meaning,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Style
                        if (messageAnalysis.style.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Phong cách:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = messageAnalysis.style,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Notes
                        if (messageAnalysis.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Ghi chú:",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = messageAnalysis.notes,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

