// app/src/main/java/com/example/myapplication/data/repository/ChatRepository.kt
package com.example.myapplication.data.repository

import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatRoom
import com.example.myapplication.data.model.ChatType
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.data.model.RecentChatItem
import com.example.myapplication.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val chatRoomsCollection = firestore.collection("chat_rooms")
    private val usersCollection = firestore.collection("users")
    private val notificationsCollection = firestore.collection("notifications")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Generate deterministic room ID for two users
     */
    fun generateRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /**
     * Get or create chat room
     */
    suspend fun getOrCreateChatRoom(
        currentUser: User,
        otherUser: User,
        chatType: ChatType = ChatType.STRANGER
    ): Result<ChatRoom> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))
            val roomId = generateRoomId(currentUid, otherUser.uid)

            val roomDoc = chatRoomsCollection.document(roomId).get().await()

            if (roomDoc.exists()) {
                // Room exists, return it
                val room = roomDoc.toObject(ChatRoom::class.java)
                    ?: return Result.failure(Exception("Failed to parse chat room"))

                // Remove current user from deletedBy if present
                if (room.deletedBy.contains(currentUid)) {
                    chatRoomsCollection.document(roomId)
                        .update("deletedBy", FieldValue.arrayRemove(currentUid))
                        .await()
                }

                Result.success(room.copy(
                    deletedBy = room.deletedBy.filter { it != currentUid }
                ))
            } else {
                // Create new room
                val newRoom = hashMapOf(
                    "roomId" to roomId,
                    "participants" to listOf(currentUid, otherUser.uid),
                    "chatType" to chatType.name,
                    "deletedBy" to emptyList<String>(),
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "createdAt" to Timestamp.now()
                )

                chatRoomsCollection.document(roomId).set(newRoom).await()

                Result.success(ChatRoom(
                    roomId = roomId,
                    participants = listOf(currentUid, otherUser.uid),
                    chatType = chatType.name,
                    deletedBy = emptyList(),
                    lastMessage = "",
                    lastMessageSenderId = "",
                    lastMessageTime = Timestamp.now(),
                    createdAt = Timestamp.now()
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe chat room by ID
     */
    fun observeChatRoom(roomId: String): Flow<ChatRoom?> = callbackFlow {
        val listener = chatRoomsCollection.document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val room = snapshot?.toObject(ChatRoom::class.java)
                trySend(room)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe messages in a chat room
     */
    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatRoomsCollection.document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        roomId: String,
        content: String,
        senderUsername: String
    ): Result<ChatMessage> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))

            val messageId = chatRoomsCollection.document(roomId)
                .collection("messages")
                .document().id

            val message = hashMapOf(
                "messageId" to messageId,
                "senderId" to currentUid,
                "senderUsername" to senderUsername,
                "content" to content,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )

            // Add message
            chatRoomsCollection.document(roomId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()

            // Update room's last message
            chatRoomsCollection.document(roomId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastMessageSenderId" to currentUid,
                        "lastMessageTime" to Timestamp.now()
                    )
                )
                .await()

            // Get room to find other participant
            val room = chatRoomsCollection.document(roomId).get().await()
                .toObject(ChatRoom::class.java)

            // Create notification for other participant
            room?.participants?.find { it != currentUid }?.let { otherUid ->
                createMessageNotification(
                    toUid = otherUid,
                    roomId = roomId,
                    senderUsername = senderUsername,
                    messagePreview = if (content.length > 50) content.take(50) + "..." else content
                )
            }

            Result.success(ChatMessage(
                messageId = messageId,
                senderId = currentUid,
                senderUsername = senderUsername,
                content = content,
                isRead = false,
                createdAt = Timestamp.now()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe recent chats for current user
     */
    fun observeRecentChats(): Flow<List<RecentChatItem>> = callbackFlow {
        val uid = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = chatRoomsCollection
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)
                }?.filter { room ->
                    // Filter out rooms deleted by current user
                    !room.deletedBy.contains(uid)
                } ?: emptyList()

                // Convert to RecentChatItem (we'll need to fetch other user info)
                val recentChats = rooms.map { room ->
                    val otherUserId = room.participants.find { it != uid } ?: ""
                    RecentChatItem(
                        roomId = room.roomId,
                        otherUserId = otherUserId,
                        otherUserName = "", // Will be populated by ViewModel
                        otherUserNickname = "",
                        otherUserAvatarIndex = 0,
                        lastMessage = room.lastMessage,
                        lastMessageTime = room.lastMessageTime,
                        chatType = try {
                            ChatType.valueOf(room.chatType)
                        } catch (_: Exception) {
                            ChatType.STRANGER
                        },
                        isLastMessageMine = room.lastMessageSenderId == uid
                    )
                }

                trySend(recentChats)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Soft delete a chat (only for current user)
     */
    suspend fun softDeleteChat(roomId: String): Result<Unit> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))

            chatRoomsCollection.document(roomId)
                .update("deletedBy", FieldValue.arrayUnion(currentUid))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get chat room by ID
     */
    suspend fun getChatRoom(roomId: String): Result<ChatRoom?> {
        return try {
            val doc = chatRoomsCollection.document(roomId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(ChatRoom::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create message notification
     */
    private suspend fun createMessageNotification(
        toUid: String,
        roomId: String,
        senderUsername: String,
        messagePreview: String
    ) {
        try {
            val currentUid = currentUserId ?: return

            val notificationId = notificationsCollection.document().id
            val notification = hashMapOf(
                "notificationId" to notificationId,
                "toUid" to toUid,
                "type" to NotificationType.NEW_MESSAGE.name,
                "relatedId" to roomId,
                "title" to "Tin nhắn mới",
                "content" to "$senderUsername: $messagePreview",
                "fromUid" to currentUid,
                "fromUsername" to senderUsername,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )
            notificationsCollection.document(notificationId).set(notification).await()
        } catch (_: Exception) {}
    }
}

