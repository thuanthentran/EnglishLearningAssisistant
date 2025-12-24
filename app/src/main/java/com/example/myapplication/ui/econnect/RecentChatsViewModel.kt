// app/src/main/java/com/example/myapplication/ui/econnect/RecentChatsViewModel.kt
package com.example.myapplication.ui.econnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ChatType
import com.example.myapplication.data.model.RecentChatItem
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RecentChatsUiState(
    val recentChats: List<RecentChatItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

class RecentChatsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow(RecentChatsUiState())
    val uiState: StateFlow<RecentChatsUiState> = _uiState.asStateFlow()

    // Cache for user info
    private val userCache = mutableMapOf<String, User>()

    private var observerJob: Job? = null

    init {
        observeRecentChats()
    }

    private fun observeRecentChats() {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Chưa đăng nhập"
            )
            return
        }

        observerJob = viewModelScope.launch {
            try {
                callbackFlow {
                    val listener = firestore.collection("chat_rooms")
                        .whereArrayContains("participants", uid)
                        .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                        .limit(50)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(emptyList<RecentChatItem>())
                                return@addSnapshotListener
                            }

                            val chats = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    val participants = (doc.get("participants") as? List<*>)
                                        ?.filterIsInstance<String>() ?: return@mapNotNull null
                                    val deletedBy = (doc.get("deletedBy") as? List<*>)
                                        ?.filterIsInstance<String>() ?: emptyList()

                                    // Skip if deleted by current user
                                    if (deletedBy.contains(uid)) return@mapNotNull null

                                    val otherUserId = participants.find { it != uid } ?: return@mapNotNull null

                                    val chatTypeStr = doc.getString("chatType") ?: "STRANGER"
                                    val chatType = try {
                                        ChatType.valueOf(chatTypeStr)
                                    } catch (_: Exception) {
                                        ChatType.STRANGER
                                    }

                                    val lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""

                                    RecentChatItem(
                                        roomId = doc.id,
                                        otherUserId = otherUserId,
                                        otherUserName = "",
                                        otherUserNickname = "",
                                        otherUserAvatarIndex = 0,
                                        lastMessage = doc.getString("lastMessage") ?: "",
                                        lastMessageTime = doc.getTimestamp("lastMessageTime"),
                                        chatType = chatType,
                                        isLastMessageMine = lastMessageSenderId == uid
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            trySend(chats)
                        }

                    awaitClose { listener.remove() }
                }.collect { chats: List<RecentChatItem> ->
                    // Fetch user info for each chat
                    val enrichedChats = chats.map { chat ->
                        try {
                            val otherUser = getUserInfo(chat.otherUserId)
                            chat.copy(
                                otherUserName = otherUser?.username ?: "Unknown",
                                otherUserNickname = otherUser?.nickname ?: "",
                                otherUserAvatarIndex = otherUser?.avatarIndex ?: 0
                            )
                        } catch (_: Exception) {
                            chat
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        recentChats = enrichedChats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi tải danh sách: ${e.message}"
                )
            }
        }
    }

    private suspend fun getUserInfo(userId: String): User? {
        if (userId.isBlank()) return null

        // Check cache first
        userCache[userId]?.let { return it }

        // Fetch from Firestore
        return try {
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
                userCache[userId] = user
                user
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Delete chat - saves deletedAt timestamp so user won't see old messages
     */
    fun deleteChat(roomId: String) {
        val uid = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Save deletedAt timestamp for this user AND add to deletedBy
                firestore.collection("chat_rooms").document(roomId)
                    .update(
                        mapOf(
                            "deletedBy" to FieldValue.arrayUnion(uid),
                            "deletedAt_$uid" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                _uiState.value = _uiState.value.copy(
                    successMessage = "Đã xóa cuộc trò chuyện"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Lỗi xóa cuộc trò chuyện: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        observerJob?.cancel()
    }
}

