// app/src/main/java/com/example/myapplication/data/model/EconnectModels.kt
package com.example.myapplication.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Chat type between users
 */
enum class ChatType {
    FRIEND,
    STRANGER
}

/**
 * Notification types
 */
enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    NEW_MESSAGE
}

/**
 * User model for Firestore
 */
data class User(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val nickname: String = "",
    val avatarIndex: Int = 0,
    val avatarUri: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastActive: Timestamp? = null
)

/**
 * Friend relationship
 */
data class Friend(
    @DocumentId
    val friendUid: String = "",
    val friendEmail: String = "",
    val friendUsername: String = "",
    val friendNickname: String = "",
    val friendAvatarIndex: Int = 0,
    @ServerTimestamp
    val addedAt: Timestamp? = null
)

/**
 * Friend request
 */
data class FriendRequest(
    @DocumentId
    val requestId: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val fromUsername: String = "",
    val fromNickname: String = "",
    val fromAvatarIndex: Int = 0,
    val toUid: String = "",
    val toEmail: String = "",
    val toUsername: String = "",
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * Matching queue entry
 */
data class MatchingQueueEntry(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val nickname: String = "",
    val avatarIndex: Int = 0,
    @ServerTimestamp
    val joinedAt: Timestamp? = null
)

/**
 * Chat room
 */
data class ChatRoom(
    @DocumentId
    val roomId: String = "",
    val participants: List<String> = emptyList(),
    val chatType: String = ChatType.STRANGER.name,
    val deletedBy: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTime: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * Chat message
 */
data class ChatMessage(
    @DocumentId
    val messageId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val content: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * Notification
 */
data class Notification(
    @DocumentId
    val notificationId: String = "",
    val toUid: String = "",
    val type: String = NotificationType.NEW_MESSAGE.name,
    val relatedId: String = "", // chatRoomId or requestId
    val title: String = "",
    val content: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * UI model for recent chat display
 */
data class RecentChatItem(
    val roomId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserNickname: String,
    val otherUserAvatarIndex: Int,
    val lastMessage: String,
    val lastMessageTime: Timestamp?,
    val chatType: ChatType,
    val isLastMessageMine: Boolean
)

