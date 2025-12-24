// app/src/main/java/com/example/myapplication/data/repository/FriendRepository.kt
package com.example.myapplication.data.repository

import com.example.myapplication.data.model.ChatType
import com.example.myapplication.data.model.Friend
import com.example.myapplication.data.model.FriendRequest
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val friendsCollection = firestore.collection("friends")
    private val friendRequestsCollection = firestore.collection("friend_requests")
    private val chatRoomsCollection = firestore.collection("chat_rooms")
    private val notificationsCollection = firestore.collection("notifications")

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Observe friends list in realtime
     */
    fun observeFriends(): Flow<List<Friend>> = callbackFlow {
        val uid = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = friendsCollection.document(uid)
            .collection("user_friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friends = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Friend::class.java)
                } ?: emptyList()

                trySend(friends)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Check if two users are friends
     */
    suspend fun areFriends(uid1: String, uid2: String): Boolean {
        return try {
            val doc = friendsCollection.document(uid1)
                .collection("user_friends")
                .document(uid2)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Observe incoming friend requests
     */
    fun observeIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = friendRequestsCollection
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe outgoing friend requests
     */
    fun observeOutgoingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = friendRequestsCollection
            .whereEqualTo("fromUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send friend request
     */
    suspend fun sendFriendRequest(
        currentUser: User,
        targetUser: User
    ): Result<Unit> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))

            // Check if already friends
            if (areFriends(currentUid, targetUser.uid)) {
                return Result.failure(Exception("Đã là bạn bè"))
            }

            // Check if request already exists
            val existingRequest = friendRequestsCollection
                .whereEqualTo("fromUid", currentUid)
                .whereEqualTo("toUid", targetUser.uid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Đã gửi lời mời kết bạn"))
            }

            // Check if target already sent request to current user
            val reverseRequest = friendRequestsCollection
                .whereEqualTo("fromUid", targetUser.uid)
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (!reverseRequest.isEmpty) {
                return Result.failure(Exception("Người này đã gửi lời mời cho bạn"))
            }

            // Create friend request
            val requestId = friendRequestsCollection.document().id
            val request = hashMapOf(
                "requestId" to requestId,
                "fromUid" to currentUid,
                "fromEmail" to currentUser.email,
                "fromUsername" to currentUser.username,
                "fromNickname" to currentUser.nickname,
                "fromAvatarIndex" to currentUser.avatarIndex,
                "toUid" to targetUser.uid,
                "toEmail" to targetUser.email,
                "toUsername" to targetUser.username,
                "status" to "pending",
                "createdAt" to Timestamp.now()
            )

            friendRequestsCollection.document(requestId).set(request).await()

            // Create notification for target user
            createNotification(
                toUid = targetUser.uid,
                type = NotificationType.FRIEND_REQUEST,
                relatedId = requestId,
                title = "Lời mời kết bạn",
                content = "${currentUser.nickname.ifEmpty { currentUser.username }} muốn kết bạn với bạn",
                fromUid = currentUid,
                fromUsername = currentUser.nickname.ifEmpty { currentUser.username }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))

            val batch = firestore.batch()

            // Update request status
            batch.update(
                friendRequestsCollection.document(request.requestId),
                "status", "accepted"
            )

            // Add friend to current user's friends list
            val friendForMe = hashMapOf(
                "friendUid" to request.fromUid,
                "friendEmail" to request.fromEmail,
                "friendUsername" to request.fromUsername,
                "friendNickname" to request.fromNickname,
                "friendAvatarIndex" to request.fromAvatarIndex,
                "addedAt" to Timestamp.now()
            )
            batch.set(
                friendsCollection.document(currentUid)
                    .collection("user_friends")
                    .document(request.fromUid),
                friendForMe
            )

            // Add current user to requester's friends list
            // Query current user directly from Firestore
            val currentUserDoc = firestore.collection("users").document(currentUid).get().await()
            val currentUserEmail = currentUserDoc.getString("email") ?: ""
            val currentUserUsername = currentUserDoc.getString("username") ?: ""
            val currentUserNickname = currentUserDoc.getString("nickname") ?: ""
            val currentUserAvatarIndex = currentUserDoc.getLong("avatarIndex")?.toInt() ?: 0

            val friendForThem = hashMapOf(
                "friendUid" to currentUid,
                "friendEmail" to currentUserEmail,
                "friendUsername" to currentUserUsername,
                "friendNickname" to currentUserNickname,
                "friendAvatarIndex" to currentUserAvatarIndex,
                "addedAt" to Timestamp.now()
            )
            batch.set(
                friendsCollection.document(request.fromUid)
                    .collection("user_friends")
                    .document(currentUid),
                friendForThem
            )

            batch.commit().await()

            // Update existing chat room to FRIEND type if exists
            updateChatTypeToFriend(currentUid, request.fromUid)

            // Create notification for the requester
            val displayName = currentUserNickname.ifEmpty { currentUserUsername }.ifEmpty { "Người dùng" }

            createNotification(
                toUid = request.fromUid,
                type = NotificationType.FRIEND_ACCEPTED,
                relatedId = request.requestId,
                title = "Lời mời được chấp nhận",
                content = "$displayName đã chấp nhận lời mời kết bạn",
                fromUid = currentUid,
                fromUsername = displayName
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject friend request
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            friendRequestsCollection.document(requestId)
                .update("status", "rejected")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unfriend a user
     */
    suspend fun unfriend(friendUid: String): Result<Unit> {
        return try {
            val currentUid = currentUserId ?: return Result.failure(Exception("Not logged in"))

            val batch = firestore.batch()

            // Remove from current user's friends
            batch.delete(
                friendsCollection.document(currentUid)
                    .collection("user_friends")
                    .document(friendUid)
            )

            // Remove current user from friend's list
            batch.delete(
                friendsCollection.document(friendUid)
                    .collection("user_friends")
                    .document(currentUid)
            )

            batch.commit().await()

            // Update chat room to STRANGER type
            updateChatTypeToStranger(currentUid, friendUid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update chat type to FRIEND
     */
    private suspend fun updateChatTypeToFriend(uid1: String, uid2: String) {
        try {
            val roomId = generateRoomId(uid1, uid2)
            val roomDoc = chatRoomsCollection.document(roomId).get().await()
            if (roomDoc.exists()) {
                chatRoomsCollection.document(roomId)
                    .update("chatType", ChatType.FRIEND.name)
                    .await()
            }
        } catch (_: Exception) {}
    }

    /**
     * Update chat type to STRANGER
     */
    private suspend fun updateChatTypeToStranger(uid1: String, uid2: String) {
        try {
            val roomId = generateRoomId(uid1, uid2)
            val roomDoc = chatRoomsCollection.document(roomId).get().await()
            if (roomDoc.exists()) {
                chatRoomsCollection.document(roomId)
                    .update("chatType", ChatType.STRANGER.name)
                    .await()
            }
        } catch (_: Exception) {}
    }

    /**
     * Generate deterministic room ID
     */
    private fun generateRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /**
     * Create notification
     */
    private suspend fun createNotification(
        toUid: String,
        type: NotificationType,
        relatedId: String,
        title: String,
        content: String,
        fromUid: String,
        fromUsername: String
    ) {
        try {
            val notificationId = notificationsCollection.document().id
            val notification = hashMapOf(
                "notificationId" to notificationId,
                "toUid" to toUid,
                "type" to type.name,
                "relatedId" to relatedId,
                "title" to title,
                "content" to content,
                "fromUid" to fromUid,
                "fromUsername" to fromUsername,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )
            notificationsCollection.document(notificationId).set(notification).await()
        } catch (_: Exception) {}
    }
}

