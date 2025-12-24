// app/src/main/java/com/example/myapplication/ui/econnect/FriendsViewModel.kt
package com.example.myapplication.ui.econnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ChatType
import com.example.myapplication.data.model.Friend
import com.example.myapplication.data.model.FriendRequest
import com.example.myapplication.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val outgoingRequests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val searchResult: User? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null
)

class FriendsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        observeFriends()
        observeIncomingRequests()
        observeOutgoingRequests()
    }

    private fun observeFriends() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            try {
                callbackFlow {
                    val listener = firestore.collection("friends").document(uid)
                        .collection("user_friends")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(emptyList<Friend>())
                                return@addSnapshotListener
                            }

                            val friends = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    Friend(
                                        friendUid = doc.id,
                                        friendEmail = doc.getString("friendEmail") ?: "",
                                        friendUsername = doc.getString("friendUsername") ?: "",
                                        friendNickname = doc.getString("friendNickname") ?: "",
                                        friendAvatarIndex = doc.getLong("friendAvatarIndex")?.toInt() ?: 0,
                                        addedAt = doc.getTimestamp("addedAt")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            trySend(friends)
                        }

                    awaitClose { listener.remove() }
                }.collect { friends: List<Friend> ->
                    _uiState.value = _uiState.value.copy(friends = friends)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun observeIncomingRequests() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            try {
                callbackFlow {
                    val listener = firestore.collection("friend_requests")
                        .whereEqualTo("toUid", uid)
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                // Index might not exist yet, just return empty
                                trySend(emptyList<FriendRequest>())
                                return@addSnapshotListener
                            }

                            val requests = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    FriendRequest(
                                        requestId = doc.id,
                                        fromUid = doc.getString("fromUid") ?: "",
                                        fromEmail = doc.getString("fromEmail") ?: "",
                                        fromUsername = doc.getString("fromUsername") ?: "",
                                        fromNickname = doc.getString("fromNickname") ?: "",
                                        fromAvatarIndex = doc.getLong("fromAvatarIndex")?.toInt() ?: 0,
                                        toUid = doc.getString("toUid") ?: "",
                                        toEmail = doc.getString("toEmail") ?: "",
                                        toUsername = doc.getString("toUsername") ?: "",
                                        status = doc.getString("status") ?: "pending",
                                        createdAt = doc.getTimestamp("createdAt")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            trySend(requests)
                        }

                    awaitClose { listener.remove() }
                }.collect { requests: List<FriendRequest> ->
                    android.util.Log.d("FriendsViewModel", "Incoming requests updated: ${requests.size}")
                    _uiState.value = _uiState.value.copy(incomingRequests = requests)
                }
            } catch (e: Exception) {
                // Silently handle - might be index not ready
            }
        }
    }

    private fun observeOutgoingRequests() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            try {
                callbackFlow {
                    val listener = firestore.collection("friend_requests")
                        .whereEqualTo("fromUid", uid)
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(emptyList<FriendRequest>())
                                return@addSnapshotListener
                            }

                            val requests = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    FriendRequest(
                                        requestId = doc.id,
                                        fromUid = doc.getString("fromUid") ?: "",
                                        fromEmail = doc.getString("fromEmail") ?: "",
                                        fromUsername = doc.getString("fromUsername") ?: "",
                                        fromNickname = doc.getString("fromNickname") ?: "",
                                        fromAvatarIndex = doc.getLong("fromAvatarIndex")?.toInt() ?: 0,
                                        toUid = doc.getString("toUid") ?: "",
                                        toEmail = doc.getString("toEmail") ?: "",
                                        toUsername = doc.getString("toUsername") ?: "",
                                        status = doc.getString("status") ?: "pending",
                                        createdAt = doc.getTimestamp("createdAt")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            trySend(requests)
                        }

                    awaitClose { listener.remove() }
                }.collect { requests: List<FriendRequest> ->
                    android.util.Log.d("FriendsViewModel", "Outgoing requests updated: ${requests.size}")
                    _uiState.value = _uiState.value.copy(outgoingRequests = requests)
                }
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    /**
     * Search for user by email or username
     */
    fun searchUser(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResult = null,
                searchError = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isSearching = true,
            searchQuery = query,
            searchError = null
        )

        viewModelScope.launch {
            try {
                // Check if it's an email
                val querySnapshot = if (query.contains("@")) {
                    firestore.collection("users")
                        .whereEqualTo("email", query.lowercase().trim())
                        .limit(1)
                        .get()
                        .await()
                } else {
                    firestore.collection("users")
                        .whereEqualTo("username", query.trim())
                        .limit(1)
                        .get()
                        .await()
                }

                if (querySnapshot.isEmpty) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResult = null,
                        searchError = "Không tìm thấy người dùng"
                    )
                } else {
                    val doc = querySnapshot.documents[0]
                    val user = User(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        username = doc.getString("username") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        avatarIndex = doc.getLong("avatarIndex")?.toInt() ?: 0,
                        avatarUri = doc.getString("avatarUri")
                    )

                    if (user.uid == currentUserId) {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchResult = null,
                            searchError = "Không thể tự kết bạn với chính mình"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            searchResult = user,
                            searchError = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResult = null,
                    searchError = e.message ?: "Lỗi tìm kiếm"
                )
            }
        }
    }

    /**
     * Send friend request
     */
    fun sendFriendRequest(targetUser: User) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val uid = currentUserId ?: throw Exception("Chưa đăng nhập")
                val userDoc = firestore.collection("users").document(uid).get().await()

                // Check if already friends
                val friendDoc = firestore.collection("friends").document(uid)
                    .collection("user_friends").document(targetUser.uid).get().await()
                if (friendDoc.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Đã là bạn bè"
                    )
                    return@launch
                }

                // Check existing request - allow re-sending if not pending
                val existingRequests = firestore.collection("friend_requests")
                    .whereEqualTo("fromUid", uid)
                    .whereEqualTo("toUid", targetUser.uid)
                    .get()
                    .await()

                // Delete old non-pending requests, block if pending exists
                for (doc in existingRequests.documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "pending") {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Đã gửi lời mời kết bạn"
                        )
                        return@launch
                    } else {
                        // Delete old rejected/accepted requests to allow re-send
                        doc.reference.delete().await()
                    }
                }

                // Also check reverse direction and clean up
                val reverseRequests = firestore.collection("friend_requests")
                    .whereEqualTo("fromUid", targetUser.uid)
                    .whereEqualTo("toUid", uid)
                    .get()
                    .await()
                for (doc in reverseRequests.documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status != "pending") {
                        doc.reference.delete().await()
                    }
                }

                // Create friend request
                val requestId = firestore.collection("friend_requests").document().id
                val request = hashMapOf(
                    "requestId" to requestId,
                    "fromUid" to uid,
                    "fromEmail" to (userDoc.getString("email") ?: ""),
                    "fromUsername" to (userDoc.getString("username") ?: ""),
                    "fromNickname" to (userDoc.getString("nickname") ?: ""),
                    "fromAvatarIndex" to (userDoc.getLong("avatarIndex")?.toInt() ?: 0),
                    "toUid" to targetUser.uid,
                    "toEmail" to targetUser.email,
                    "toUsername" to targetUser.username,
                    "status" to "pending",
                    "createdAt" to Timestamp.now()
                )

                firestore.collection("friend_requests").document(requestId).set(request).await()

                // Create notification
                val notificationId = firestore.collection("notifications").document().id
                val notification = hashMapOf(
                    "notificationId" to notificationId,
                    "toUid" to targetUser.uid,
                    "type" to "FRIEND_REQUEST",
                    "relatedId" to requestId,
                    "title" to "Lời mời kết bạn",
                    "content" to "${userDoc.getString("nickname")?.takeIf { it.isNotEmpty() } ?: userDoc.getString("username")} muốn kết bạn với bạn",
                    "fromUid" to uid,
                    "fromUsername" to (userDoc.getString("nickname")?.takeIf { it.isNotEmpty() } ?: userDoc.getString("username") ?: ""),
                    "isRead" to false,
                    "createdAt" to Timestamp.now()
                )
                firestore.collection("notifications").document(notificationId).set(notification).await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã gửi lời mời kết bạn",
                    searchResult = null,
                    searchQuery = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Lỗi gửi lời mời"
                )
            }
        }
    }

    /**
     * Accept friend request
     */
    fun acceptRequest(request: FriendRequest) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val uid = currentUserId ?: throw Exception("Chưa đăng nhập")

                // Get current user info
                val currentUserDoc = firestore.collection("users").document(uid).get().await()
                val currentUserEmail = currentUserDoc.getString("email") ?: ""
                val currentUserUsername = currentUserDoc.getString("username") ?: ""
                val currentUserNickname = currentUserDoc.getString("nickname") ?: ""
                val currentUserAvatarIndex = currentUserDoc.getLong("avatarIndex")?.toInt() ?: 0

                val batch = firestore.batch()

                // Update request status
                batch.update(
                    firestore.collection("friend_requests").document(request.requestId),
                    "status", "accepted"
                )

                // Add friend to current user's list
                val friendForMe = hashMapOf(
                    "friendUid" to request.fromUid,
                    "friendEmail" to request.fromEmail,
                    "friendUsername" to request.fromUsername,
                    "friendNickname" to request.fromNickname,
                    "friendAvatarIndex" to request.fromAvatarIndex,
                    "addedAt" to Timestamp.now()
                )
                batch.set(
                    firestore.collection("friends").document(uid)
                        .collection("user_friends").document(request.fromUid),
                    friendForMe
                )

                // Add current user to requester's list
                val friendForThem = hashMapOf(
                    "friendUid" to uid,
                    "friendEmail" to currentUserEmail,
                    "friendUsername" to currentUserUsername,
                    "friendNickname" to currentUserNickname,
                    "friendAvatarIndex" to currentUserAvatarIndex,
                    "addedAt" to Timestamp.now()
                )
                batch.set(
                    firestore.collection("friends").document(request.fromUid)
                        .collection("user_friends").document(uid),
                    friendForThem
                )

                batch.commit().await()

                // Update existing chat room to FRIEND if exists
                val roomId = if (uid < request.fromUid) "${uid}_${request.fromUid}" else "${request.fromUid}_${uid}"
                try {
                    val roomDoc = firestore.collection("chat_rooms").document(roomId).get().await()
                    if (roomDoc.exists()) {
                        firestore.collection("chat_rooms").document(roomId)
                            .update("chatType", ChatType.FRIEND.name).await()
                    }
                } catch (_: Exception) {}

                // Create notification
                val notificationId = firestore.collection("notifications").document().id
                val displayName = currentUserNickname.ifEmpty { currentUserUsername }.ifEmpty { "Người dùng" }
                val notification = hashMapOf(
                    "notificationId" to notificationId,
                    "toUid" to request.fromUid,
                    "type" to "FRIEND_ACCEPTED",
                    "relatedId" to request.requestId,
                    "title" to "Lời mời được chấp nhận",
                    "content" to "$displayName đã chấp nhận lời mời kết bạn",
                    "fromUid" to uid,
                    "fromUsername" to displayName,
                    "isRead" to false,
                    "createdAt" to Timestamp.now()
                )
                firestore.collection("notifications").document(notificationId).set(notification).await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã chấp nhận lời mời"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Reject friend request
     */
    fun rejectRequest(requestId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                firestore.collection("friend_requests").document(requestId)
                    .update("status", "rejected").await()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã từ chối lời mời"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Unfriend a user
     */
    fun unfriend(friendUid: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val uid = currentUserId ?: throw Exception("Chưa đăng nhập")

                val batch = firestore.batch()

                // Remove from current user's friends
                batch.delete(
                    firestore.collection("friends").document(uid)
                        .collection("user_friends").document(friendUid)
                )

                // Remove current user from friend's list
                batch.delete(
                    firestore.collection("friends").document(friendUid)
                        .collection("user_friends").document(uid)
                )

                batch.commit().await()

                // Delete ALL friend_request documents between these users (both directions)
                try {
                    // Delete requests from me to them
                    val requestsFromMe = firestore.collection("friend_requests")
                        .whereEqualTo("fromUid", uid)
                        .whereEqualTo("toUid", friendUid)
                        .get().await()
                    for (doc in requestsFromMe.documents) {
                        doc.reference.delete().await()
                    }

                    // Delete requests from them to me
                    val requestsFromThem = firestore.collection("friend_requests")
                        .whereEqualTo("fromUid", friendUid)
                        .whereEqualTo("toUid", uid)
                        .get().await()
                    for (doc in requestsFromThem.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {}

                // Update chat room to STRANGER
                val roomId = if (uid < friendUid) "${uid}_${friendUid}" else "${friendUid}_${uid}"
                try {
                    val roomDoc = firestore.collection("chat_rooms").document(roomId).get().await()
                    if (roomDoc.exists()) {
                        firestore.collection("chat_rooms").document(roomId)
                            .update("chatType", ChatType.STRANGER.name).await()
                    }
                } catch (_: Exception) {}

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã hủy kết bạn"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Open chat with friend
     */
    suspend fun openChatWithFriend(friend: Friend): String? {
        val uid = currentUserId ?: return null

        try {
            val roomId = if (uid < friend.friendUid) "${uid}_${friend.friendUid}" else "${friend.friendUid}_${uid}"

            val roomDoc = firestore.collection("chat_rooms").document(roomId).get().await()

            if (!roomDoc.exists()) {
                // Create new room
                val newRoom = hashMapOf(
                    "roomId" to roomId,
                    "participants" to listOf(uid, friend.friendUid),
                    "chatType" to ChatType.FRIEND.name,
                    "deletedBy" to emptyList<String>(),
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "createdAt" to Timestamp.now()
                )
                firestore.collection("chat_rooms").document(roomId).set(newRoom).await()
            }

            return roomId
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null,
            searchError = null
        )
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResult = null,
            searchError = null
        )
    }
}

