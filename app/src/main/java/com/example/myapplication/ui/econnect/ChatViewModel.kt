// app/src/main/java/com/example/myapplication/ui/econnect/ChatViewModel.kt
package com.example.myapplication.ui.econnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatRoom
import com.example.myapplication.data.model.ChatType
import com.example.myapplication.data.model.User
import com.example.myapplication.utils.ChatAIService
import com.example.myapplication.utils.ChatCacheManager
import com.example.myapplication.utils.MessageAnalysis
import com.example.myapplication.utils.TranslationResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatUiState(
    val isLoading: Boolean = true,
    val chatRoom: ChatRoom? = null,
    val messages: List<ChatMessage> = emptyList(),
    val otherUser: User? = null,
    val currentUserId: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val isFriend: Boolean = false,
    val friendRequestSent: Boolean = false,
    val deletedAt: Timestamp? = null, // Timestamp when user deleted chat - only show messages after this
    // AI Learning states
    val translations: Map<String, TranslationResult> = emptyMap(), // messageId -> translation
    val isTranslating: String? = null, // messageId being translated
    val messageAnalysis: MessageAnalysis? = null, // Current message being analyzed
    val isAnalyzing: Boolean = false,
    val showAnalysisDialog: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cacheManager = ChatCacheManager.getInstance(application)

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var roomId: String? = null
    private var roomObserverJob: Job? = null
    private var messagesObserverJob: Job? = null


    /**
     * Load chat room - with safe error handling
     */
    fun loadChatRoom(roomId: String) {
        // Cancel previous observers
        roomObserverJob?.cancel()
        messagesObserverJob?.cancel()

        if (roomId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid room ID"
            )
            return
        }

        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Chưa đăng nhập"
            )
            return
        }

        this.roomId = roomId
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            currentUserId = uid,
            error = null
        )

        // Observe chat room with timeout
        roomObserverJob = viewModelScope.launch {
            var roomFound = false

            val timeoutJob = launch {
                delay(8000) // 8 second timeout
                if (!roomFound && _uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Không tìm thấy phòng chat"
                    )
                }
            }

            try {
                callbackFlow {
                    val listener = firestore.collection("chat_rooms").document(roomId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(null)
                                return@addSnapshotListener
                            }

                            if (snapshot != null && snapshot.exists()) {
                                try {
                                    val room = ChatRoom(
                                        roomId = snapshot.id,
                                        participants = (snapshot.get("participants") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                                        chatType = snapshot.getString("chatType") ?: ChatType.STRANGER.name,
                                        deletedBy = (snapshot.get("deletedBy") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                                        lastMessage = snapshot.getString("lastMessage") ?: "",
                                        lastMessageSenderId = snapshot.getString("lastMessageSenderId") ?: "",
                                        lastMessageTime = snapshot.getTimestamp("lastMessageTime"),
                                        createdAt = snapshot.getTimestamp("createdAt")
                                    )
                                    // Get deletedAt for current user
                                    val deletedAt = snapshot.getTimestamp("deletedAt_$uid")
                                    trySend(Pair(room, deletedAt))
                                } catch (e: Exception) {
                                    trySend(null)
                                }
                            } else {
                                trySend(null)
                            }
                        }

                    awaitClose { listener.remove() }
                }.collect { data: Pair<ChatRoom, Timestamp?>? ->
                    if (data != null) {
                        val (room, deletedAt) = data
                        roomFound = true
                        timeoutJob.cancel()

                        _uiState.value = _uiState.value.copy(
                            chatRoom = room,
                            isLoading = false,
                            error = null,
                            deletedAt = deletedAt
                        )

                        // Load other user info
                        val otherUserId = room.participants.find { it != uid }
                        if (!otherUserId.isNullOrEmpty()) {
                            loadOtherUser(otherUserId)
                            checkFriendship(otherUserId)
                        }
                    }
                }
            } catch (e: Exception) {
                if (!roomFound) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Lỗi tải phòng chat: ${e.message}"
                    )
                }
            }
        }

        // Observe messages
        messagesObserverJob = viewModelScope.launch {
            try {
                callbackFlow {
                    val listener = firestore.collection("chat_rooms").document(roomId)
                        .collection("messages")
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(emptyList<ChatMessage>())
                                return@addSnapshotListener
                            }

                            val messages = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    ChatMessage(
                                        messageId = doc.id,
                                        senderId = doc.getString("senderId") ?: "",
                                        senderUsername = doc.getString("senderUsername") ?: "",
                                        content = doc.getString("content") ?: "",
                                        isRead = doc.getBoolean("isRead") ?: false,
                                        createdAt = doc.getTimestamp("createdAt")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            trySend(messages)
                        }

                    awaitClose { listener.remove() }
                }.collect { allMessages: List<ChatMessage> ->
                    // Filter messages: only show messages created AFTER deletedAt
                    val deletedAt = _uiState.value.deletedAt
                    val filteredMessages = if (deletedAt != null) {
                        allMessages.filter { message ->
                            val msgTime = message.createdAt
                            msgTime != null && msgTime.seconds > deletedAt.seconds
                        }
                    } else {
                        allMessages
                    }
                    _uiState.value = _uiState.value.copy(messages = filteredMessages)
                }
            } catch (_: Exception) {
                // Silently handle message loading errors
            }
        }
    }

    private fun loadOtherUser(userId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val user = User(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        avatarIndex = doc.getLong("avatarIndex")?.toInt() ?: 0,
                        avatarUri = doc.getString("avatarUri")
                    )
                    _uiState.value = _uiState.value.copy(otherUser = user)
                }
            } catch (_: Exception) {
                // Silently handle - user info is optional
            }
        }
    }

    private fun checkFriendship(otherUserId: String) {
        viewModelScope.launch {
            try {
                val uid = currentUserId ?: return@launch
                val doc = firestore.collection("friends").document(uid)
                    .collection("user_friends").document(otherUserId).get().await()
                _uiState.value = _uiState.value.copy(isFriend = doc.exists())
            } catch (_: Exception) {
                // Silently handle
            }
        }
    }

    /**
     * Send message
     */
    fun sendMessage(content: String) {
        val roomId = this.roomId ?: return
        if (content.isBlank()) return
        val uid = currentUserId ?: return

        _uiState.value = _uiState.value.copy(isSending = true)

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val nickname = userDoc.getString("nickname") ?: ""
                val username = userDoc.getString("username") ?: ""
                val senderName = nickname.ifEmpty { username }.ifEmpty { "User" }

                val messageId = firestore.collection("chat_rooms").document(roomId)
                    .collection("messages").document().id

                val message = hashMapOf(
                    "messageId" to messageId,
                    "senderId" to uid,
                    "senderUsername" to senderName,
                    "content" to content.trim(),
                    "isRead" to false,
                    "createdAt" to Timestamp.now()
                )

                firestore.collection("chat_rooms").document(roomId)
                    .collection("messages").document(messageId).set(message).await()

                // Update last message AND clear deletedBy so chat reappears in Recent for both users
                firestore.collection("chat_rooms").document(roomId).update(
                    mapOf(
                        "lastMessage" to content.trim(),
                        "lastMessageSenderId" to uid,
                        "lastMessageTime" to Timestamp.now(),
                        "deletedBy" to emptyList<String>() // Clear deletedBy when new message sent
                    )
                ).await()

                _uiState.value = _uiState.value.copy(isSending = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "Lỗi gửi tin nhắn: ${e.message}"
                )
            }
        }
    }

    /**
     * Send friend request
     */
    fun sendFriendRequest() {
        val otherUser = _uiState.value.otherUser ?: return
        val uid = currentUserId ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()

                val requestId = firestore.collection("friend_requests").document().id
                val request = hashMapOf(
                    "requestId" to requestId,
                    "fromUid" to uid,
                    "fromEmail" to (userDoc.getString("email") ?: ""),
                    "fromUsername" to (userDoc.getString("username") ?: ""),
                    "fromNickname" to (userDoc.getString("nickname") ?: ""),
                    "fromAvatarIndex" to (userDoc.getLong("avatarIndex")?.toInt() ?: 0),
                    "toUid" to otherUser.uid,
                    "toEmail" to otherUser.email,
                    "toUsername" to otherUser.username,
                    "status" to "pending",
                    "createdAt" to Timestamp.now()
                )

                firestore.collection("friend_requests").document(requestId).set(request).await()
                _uiState.value = _uiState.value.copy(friendRequestSent = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Lỗi gửi lời mời: ${e.message}")
            }
        }
    }

    /**
     * Get chat type safely
     */
    fun getChatType(): ChatType {
        return try {
            ChatType.valueOf(_uiState.value.chatRoom?.chatType ?: ChatType.STRANGER.name)
        } catch (_: Exception) {
            ChatType.STRANGER
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ==================== AI LEARNING FUNCTIONS ====================

    /**
     * Translate a message (EN ⇄ VI)
     */
    fun translateMessage(messageId: String, messageContent: String) {
        // Check if already translated (visibility toggle)
        if (_uiState.value.translations.containsKey(messageId)) {
            // Toggle visibility - remove translation
            val newTranslations = _uiState.value.translations.toMutableMap()
            newTranslations.remove(messageId)
            _uiState.value = _uiState.value.copy(translations = newTranslations)
            return
        }

        // Check persistent cache first
        val cachedTranslation = cacheManager.getTranslation(messageContent)
        if (cachedTranslation != null) {
            // Use cached translation - instant display
            val newTranslations = _uiState.value.translations.toMutableMap()
            newTranslations[messageId] = cachedTranslation
            _uiState.value = _uiState.value.copy(translations = newTranslations)
            return
        }

        _uiState.value = _uiState.value.copy(isTranslating = messageId)

        viewModelScope.launch {
            try {
                val result = ChatAIService.translateMessage(messageContent)

                // Save to persistent cache
                cacheManager.saveTranslation(messageContent, result)

                val newTranslations = _uiState.value.translations.toMutableMap()
                newTranslations[messageId] = result
                _uiState.value = _uiState.value.copy(
                    translations = newTranslations,
                    isTranslating = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranslating = null,
                    error = "Lỗi dịch: ${e.message}"
                )
            }
        }
    }

    /**
     * Hide translation for a message
     */
    fun hideTranslation(messageId: String) {
        val newTranslations = _uiState.value.translations.toMutableMap()
        newTranslations.remove(messageId)
        _uiState.value = _uiState.value.copy(translations = newTranslations)
    }

    /**
     * Analyze a message for learning
     */
    fun analyzeMessage(message: String) {
        // Check persistent cache first
        val cachedAnalysis = cacheManager.getAnalysis(message)
        if (cachedAnalysis != null) {
            // Use cached analysis - instant display
            _uiState.value = _uiState.value.copy(
                showAnalysisDialog = true,
                messageAnalysis = cachedAnalysis,
                isAnalyzing = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            showAnalysisDialog = true,
            messageAnalysis = null
        )

        viewModelScope.launch {
            try {
                val result = ChatAIService.analyzeMessage(message)

                // Save to persistent cache
                cacheManager.saveAnalysis(message, result)

                _uiState.value = _uiState.value.copy(
                    messageAnalysis = result,
                    isAnalyzing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    messageAnalysis = MessageAnalysis(
                        message = message,
                        meaning = "Lỗi: ${e.message}",
                        isError = true
                    )
                )
            }
        }
    }

    /**
     * Close analysis dialog
     */
    fun closeAnalysisDialog() {
        _uiState.value = _uiState.value.copy(
            showAnalysisDialog = false,
            messageAnalysis = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        roomObserverJob?.cancel()
        messagesObserverJob?.cancel()
    }
}

