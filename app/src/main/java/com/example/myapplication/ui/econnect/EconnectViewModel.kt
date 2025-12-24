// app/src/main/java/com/example/myapplication/ui/econnect/EconnectViewModel.kt
package com.example.myapplication.ui.econnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EconnectViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    init {
        observeCurrentUser()
        observeUnreadNotifications()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch

            callbackFlow {
                val listener = firestore.collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val user = User(
                                uid = snapshot.id,
                                email = snapshot.getString("email") ?: "",
                                username = snapshot.getString("username") ?: "",
                                nickname = snapshot.getString("nickname") ?: "",
                                avatarIndex = snapshot.getLong("avatarIndex")?.toInt() ?: 0,
                                avatarUri = snapshot.getString("avatarUri")
                            )
                            trySend(user)
                        } else {
                            trySend(null)
                        }
                    }

                awaitClose { listener.remove() }
            }.collect { user: User? ->
                _currentUser.value = user
            }
        }
    }

    private fun observeUnreadNotifications() {
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
                _unreadNotificationCount.value = count
            }
        }
    }

    /**
     * Sync user data from local preferences to Firestore
     */
    fun syncUserToFirestore(
        uid: String,
        email: String,
        username: String,
        nickname: String = "",
        avatarIndex: Int = 0,
        avatarUri: String? = null
    ) {
        viewModelScope.launch {
            try {
                val userData = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "username" to username,
                    "nickname" to nickname,
                    "avatarIndex" to avatarIndex,
                    "avatarUri" to avatarUri,
                    "lastActive" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .await()
            } catch (_: Exception) {}
        }
    }
}

