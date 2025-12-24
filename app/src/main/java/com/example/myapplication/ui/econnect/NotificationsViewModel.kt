// app/src/main/java/com/example/myapplication/ui/econnect/NotificationsViewModel.kt
package com.example.myapplication.ui.econnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
        observeUnreadCount()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            callbackFlow {
                val listener = firestore.collection("notifications")
                    .whereEqualTo("toUid", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList<Notification>())
                            return@addSnapshotListener
                        }

                        val notifications = snapshot?.documents?.mapNotNull { doc ->
                            Notification(
                                notificationId = doc.id,
                                toUid = doc.getString("toUid") ?: "",
                                type = doc.getString("type") ?: NotificationType.NEW_MESSAGE.name,
                                relatedId = doc.getString("relatedId") ?: "",
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: "",
                                fromUid = doc.getString("fromUid") ?: "",
                                fromUsername = doc.getString("fromUsername") ?: "",
                                isRead = doc.getBoolean("isRead") ?: false,
                                createdAt = doc.getTimestamp("createdAt")
                            )
                        } ?: emptyList()

                        trySend(notifications)
                    }

                awaitClose { listener.remove() }
            }.collect { notifications: List<Notification> ->
                _uiState.value = _uiState.value.copy(
                    notifications = notifications,
                    isLoading = false
                )
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            callbackFlow {
                val listener = firestore.collection("notifications")
                    .whereEqualTo("toUid", uid)
                    .whereEqualTo("isRead", false)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(0)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.size() ?: 0)
                    }

                awaitClose { listener.remove() }
            }.collect { count: Int ->
                _uiState.value = _uiState.value.copy(unreadCount = count)
            }
        }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications").document(notificationId)
                    .update("isRead", true)
                    .await()
            } catch (_: Exception) {}
        }
    }

    /**
     * Mark all as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val uid = currentUserId ?: return@launch

                val unreadNotifications = firestore.collection("notifications")
                    .whereEqualTo("toUid", uid)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val batch = firestore.batch()
                unreadNotifications.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("notifications").document(notificationId).delete().await()
            } catch (_: Exception) {}
        }
    }

    /**
     * Get notification type
     */
    fun getNotificationType(notification: Notification): NotificationType {
        return try {
            NotificationType.valueOf(notification.type)
        } catch (_: Exception) {
            NotificationType.NEW_MESSAGE
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

