// app/src/main/java/com/example/myapplication/ui/econnect/MatchingViewModel.kt
package com.example.myapplication.ui.econnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.MatchingQueueEntry
import com.example.myapplication.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class MatchingState {
    object Idle : MatchingState()
    object Searching : MatchingState()
    // New state: room created but need to verify before navigation
    data class Verifying(val roomId: String) : MatchingState()
    // Ready state: room verified, safe to navigate
    data class Ready(val roomId: String, val matchedUser: MatchingQueueEntry) : MatchingState()
    data class Error(val message: String) : MatchingState()
    object Cancelled : MatchingState()
}

class MatchingViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _matchingState = MutableStateFlow<MatchingState>(MatchingState.Idle)
    val matchingState: StateFlow<MatchingState> = _matchingState.asStateFlow()

    private val _isInQueue = MutableStateFlow(false)
    val isInQueue: StateFlow<Boolean> = _isInQueue.asStateFlow()

    private var matchingJob: Job? = null
    private var queueObserverJob: Job? = null

    // Timestamp when user started matching - used to filter out old rooms
    private var matchingStartTime: Timestamp? = null

    init {
        observeQueueStatus()
    }

    private fun observeQueueStatus() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch
            observeCurrentUserInQueue(uid).collect { inQueue: Boolean ->
                _isInQueue.value = inQueue
            }
        }
    }

    private fun observeCurrentUserInQueue(uid: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("matching_queue").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Start matching process
     */
    fun startMatching(currentUser: User) {
        if (_matchingState.value is MatchingState.Searching) return

        _matchingState.value = MatchingState.Searching

        // Record the time when matching started - only detect rooms created AFTER this time
        matchingStartTime = Timestamp.now()

        matchingJob = viewModelScope.launch {
            try {
                // Join queue
                val uid = currentUserId ?: return@launch

                val entry = hashMapOf(
                    "uid" to uid,
                    "email" to currentUser.email,
                    "username" to currentUser.username,
                    "nickname" to currentUser.nickname,
                    "avatarIndex" to currentUser.avatarIndex,
                    "joinedAt" to Timestamp.now()
                )
                firestore.collection("matching_queue").document(uid).set(entry).await()

                // Start looking for matches
                var attempts = 0
                val maxAttempts = 60 // 60 seconds max

                while (_matchingState.value is MatchingState.Searching && attempts < maxAttempts) {
                    // Check if we're still in queue
                    val queueDoc = firestore.collection("matching_queue").document(uid).get().await()
                    if (!queueDoc.exists()) {
                        // We were matched by someone else - wait a bit for room to sync
                        // then find the room
                        delay(500) // Wait for Firestore to sync
                        findAndNavigateToMatchedRoom(uid)
                        return@launch
                    }

                    // Try to find a match
                    val match = findValidMatch(currentUser)
                    if (match != null) {
                        // Found a match! Create the room
                        val roomId = createMatch(currentUser, match)
                        if (roomId != null) {
                            // Verify room exists before setting Ready
                            val verified = verifyRoomExists(roomId)
                            if (verified) {
                                _matchingState.value = MatchingState.Ready(roomId, match)
                            } else {
                                _matchingState.value = MatchingState.Error("Không thể tạo phòng chat")
                            }
                            return@launch
                        }
                    }

                    delay(1000)
                    attempts++
                }

                // Timeout - leave queue
                leaveQueue()
                _matchingState.value = MatchingState.Error("Không tìm thấy người phù hợp. Vui lòng thử lại.")

            } catch (e: Exception) {
                leaveQueue()
                _matchingState.value = MatchingState.Error(e.message ?: "Đã có lỗi xảy ra")
            }
        }

        // Also observe for being matched by someone else
        queueObserverJob = viewModelScope.launch {
            val currentUid = currentUserId ?: return@launch

            observeForMatch(currentUid).collect { roomId: String? ->
                if (roomId != null && _matchingState.value is MatchingState.Searching) {
                    // Verify room exists first
                    val verified = verifyRoomExists(roomId)
                    if (!verified) return@collect

                    val parts = roomId.split("_")
                    val otherUid = parts.find { it != currentUid }
                    if (otherUid.isNullOrEmpty()) return@collect

                    try {
                        val doc = firestore.collection("users").document(otherUid).get().await()
                        val otherEmail = doc.getString("email") ?: ""
                        val otherUsername = doc.getString("username") ?: ""
                        val otherNickname = doc.getString("nickname") ?: ""
                        val otherAvatarIndex = doc.getLong("avatarIndex")?.toInt() ?: 0

                        _matchingState.value = MatchingState.Ready(
                            roomId = roomId,
                            matchedUser = MatchingQueueEntry(
                                uid = otherUid,
                                email = otherEmail,
                                username = otherUsername,
                                nickname = otherNickname,
                                avatarIndex = otherAvatarIndex
                            )
                        )
                    } catch (_: Exception) {
                        _matchingState.value = MatchingState.Ready(
                            roomId = roomId,
                            matchedUser = MatchingQueueEntry(
                                uid = otherUid,
                                email = "",
                                username = "User",
                                nickname = "",
                                avatarIndex = 0
                            )
                        )
                    }
                }
            }
        }
    }

    private fun observeForMatch(currentUid: String): Flow<String?> = callbackFlow {
        var alreadyMatched = false
        // Get matching start time - rooms must be created AFTER this
        val startTime = matchingStartTime

        val listener = firestore.collection("chat_rooms")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || alreadyMatched) {
                    trySend(null)
                    return@addSnapshotListener
                }

                // Only consider rooms created AFTER we started matching
                // Add 2 second buffer for clock skew between devices
                val recentRoom = snapshot?.documents?.find { doc ->
                    val createdAt = doc.getTimestamp("createdAt")
                    if (createdAt != null && startTime != null) {
                        // Room must be created at or after (startTime - 2 seconds)
                        createdAt.seconds >= (startTime.seconds - 2)
                    } else {
                        false
                    }
                }

                if (recentRoom != null) {
                    alreadyMatched = true
                    trySend(recentRoom.id)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    private suspend fun findValidMatch(currentUser: User): MatchingQueueEntry? {
        val currentUid = currentUserId ?: return null

        // Get all users in queue
        val queueSnapshot = firestore.collection("matching_queue")
            .orderBy("joinedAt")
            .get()
            .await()

        // Get current user's friends
        val friendsSnapshot = firestore.collection("friends").document(currentUid)
            .collection("user_friends")
            .get()
            .await()

        val friendUids = friendsSnapshot.documents.map { it.id }.toSet()

        // Find first valid match
        for (doc in queueSnapshot.documents) {
            val entryUid = doc.id
            if (entryUid != currentUid && !friendUids.contains(entryUid)) {
                return MatchingQueueEntry(
                    uid = entryUid,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    nickname = doc.getString("nickname") ?: "",
                    avatarIndex = doc.getLong("avatarIndex")?.toInt() ?: 0
                )
            }
        }

        return null
    }

    private suspend fun createMatch(currentUser: User, matchedUser: MatchingQueueEntry): String? {
        val currentUid = currentUserId ?: return null

        return try {
            // Generate deterministic room ID
            val roomId = if (currentUid < matchedUser.uid) {
                "${currentUid}_${matchedUser.uid}"
            } else {
                "${matchedUser.uid}_${currentUid}"
            }

            // Check if room already exists
            val existingRoom = firestore.collection("chat_rooms").document(roomId).get().await()

            if (!existingRoom.exists()) {
                // Create new room
                val newRoom = hashMapOf(
                    "roomId" to roomId,
                    "participants" to listOf(currentUid, matchedUser.uid),
                    "chatType" to "STRANGER",
                    "deletedBy" to emptyList<String>(),
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "createdAt" to Timestamp.now()
                )
                firestore.collection("chat_rooms").document(roomId).set(newRoom).await()

                // CRITICAL: Wait for Firestore to sync before removing from queue
                // This ensures the other user can find the room
                delay(800)
            } else {
                // Room exists, just update deletedBy
                firestore.collection("chat_rooms").document(roomId).update(
                    mapOf(
                        "deletedBy" to emptyList<String>(),
                        "lastMessageTime" to Timestamp.now(),
                        "createdAt" to Timestamp.now() // Update createdAt so other user can find it
                    )
                ).await()

                // Wait for sync
                delay(500)
            }

            // Remove both users from queue AFTER room is confirmed created
            try {
                firestore.collection("matching_queue").document(currentUid).delete().await()
                firestore.collection("matching_queue").document(matchedUser.uid).delete().await()
            } catch (_: Exception) {
                // Ignore queue cleanup errors
            }

            roomId
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verify room exists in Firestore before navigation
     * Retries up to 5 times with exponential delay
     */
    private suspend fun verifyRoomExists(roomId: String): Boolean {
        repeat(5) { attempt ->
            try {
                val doc = firestore.collection("chat_rooms").document(roomId).get().await()
                if (doc.exists()) {
                    // Extra delay to ensure data is synced
                    delay(200)
                    return true
                }
                delay(500L * (attempt + 1))
            } catch (_: Exception) {
                delay(500L * (attempt + 1))
            }
        }
        return false
    }

    /**
     * Find the room we were matched into and navigate
     * Called when we detect we've been removed from queue by another user
     * Retries multiple times to handle sync delay
     */
    private suspend fun findAndNavigateToMatchedRoom(currentUid: String) {
        // Get matching start time - rooms must be created AFTER this
        val startTime = matchingStartTime

        // Retry up to 10 times with increasing delay
        repeat(10) { attempt ->
            try {
                // Search for recently created rooms where we are a participant
                val rooms = firestore.collection("chat_rooms")
                    .whereArrayContains("participants", currentUid)
                    .get()
                    .await()

                // Find room created AFTER we started matching (with 2s buffer for clock skew)
                val recentRoom = rooms.documents.find { doc ->
                    val createdAt = doc.getTimestamp("createdAt")
                    if (createdAt != null && startTime != null) {
                        createdAt.seconds >= (startTime.seconds - 2)
                    } else {
                        false
                    }
                }

                if (recentRoom != null) {
                    val roomId = recentRoom.id
                    val participants = (recentRoom.get("participants") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                    val otherUid = participants.find { it != currentUid }

                    if (!otherUid.isNullOrEmpty()) {
                        // Get other user info
                        val otherDoc = firestore.collection("users").document(otherUid).get().await()

                        _matchingState.value = MatchingState.Ready(
                            roomId = roomId,
                            matchedUser = MatchingQueueEntry(
                                uid = otherUid,
                                email = otherDoc.getString("email") ?: "",
                                username = otherDoc.getString("username") ?: "User",
                                nickname = otherDoc.getString("nickname") ?: "",
                                avatarIndex = otherDoc.getLong("avatarIndex")?.toInt() ?: 0
                            )
                        )
                        return
                    }
                }

                // Wait before retry with exponential backoff
                delay(500L * (attempt + 1))
            } catch (e: Exception) {
                delay(500L * (attempt + 1))
            }
        }

        // After all retries failed, show error
        _matchingState.value = MatchingState.Error("Không tìm thấy phòng chat")
    }

    private suspend fun leaveQueue() {
        currentUserId?.let { uid ->
            try {
                firestore.collection("matching_queue").document(uid).delete().await()
            } catch (_: Exception) {}
        }
    }

    /**
     * Cancel matching
     */
    fun cancelMatching() {
        matchingJob?.cancel()
        queueObserverJob?.cancel()
        matchingStartTime = null

        viewModelScope.launch {
            leaveQueue()
            _matchingState.value = MatchingState.Cancelled
        }
    }

    /**
     * Reset state
     */
    fun resetState() {
        matchingStartTime = null
        _matchingState.value = MatchingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        matchingJob?.cancel()
        queueObserverJob?.cancel()
        viewModelScope.launch {
            leaveQueue()
        }
    }
}

