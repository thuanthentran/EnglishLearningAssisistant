// app/src/main/java/com/example/myapplication/ui/econnect/EconnectNavigation.kt
package com.example.myapplication.ui.econnect

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class EconnectRoute(val route: String) {
    object Home : EconnectRoute("econnect_home")
    object StrangerMatching : EconnectRoute("stranger_matching")
    object MatchingLoading : EconnectRoute("matching_loading")
    object ChatRoom : EconnectRoute("chat_room/{roomId}") {
        fun createRoute(roomId: String) = "chat_room/$roomId"
    }
    object Friends : EconnectRoute("friends")
    object FriendRequests : EconnectRoute("friend_requests")
    object RecentChats : EconnectRoute("recent_chats")
    object Notifications : EconnectRoute("notifications")
}

@Composable
fun EconnectNavHost(
    navController: NavHostController = rememberNavController(),
    onNavigateToNotifications: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = EconnectRoute.Home.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(EconnectRoute.Home.route) {
            EconnectHomeScreen(
                onNavigateToStrangerMatching = {
                    navController.navigate(EconnectRoute.MatchingLoading.route)
                },
                onNavigateToFriends = {
                    navController.navigate(EconnectRoute.Friends.route)
                },
                onNavigateToRecentChats = {
                    navController.navigate(EconnectRoute.RecentChats.route)
                },
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        composable(EconnectRoute.MatchingLoading.route) {
            MatchingLoadingScreen(
                onMatchFound = { roomId: String ->
                    // Only navigate if roomId is valid
                    if (roomId.isNotBlank()) {
                        navController.navigate(EconnectRoute.ChatRoom.createRoute(roomId)) {
                            popUpTo(EconnectRoute.Home.route)
                        }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                },
                onError = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = EconnectRoute.ChatRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId")

            // If roomId is null or blank, go back
            if (roomId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            ChatRoomScreen(
                roomId = roomId,
                onBack = { navController.popBackStack() },
                onNavigateToFriendRequests = {
                    navController.navigate(EconnectRoute.FriendRequests.route)
                }
            )
        }

        composable(EconnectRoute.Friends.route) {
            FriendsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { roomId: String ->
                    navController.navigate(EconnectRoute.ChatRoom.createRoute(roomId))
                },
                onNavigateToAddFriend = { /* No longer used - handled by BottomSheet */ },
                onNavigateToFriendRequests = {
                    navController.navigate(EconnectRoute.FriendRequests.route)
                }
            )
        }

        composable(EconnectRoute.FriendRequests.route) {
            FriendRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(EconnectRoute.RecentChats.route) {
            RecentChatsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { roomId: String ->
                    navController.navigate(EconnectRoute.ChatRoom.createRoute(roomId))
                }
            )
        }

        composable(EconnectRoute.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { roomId: String ->
                    navController.navigate(EconnectRoute.ChatRoom.createRoute(roomId))
                },
                onNavigateToFriendRequests = {
                    navController.navigate(EconnectRoute.FriendRequests.route)
                },
                onNavigateToFriends = {
                    navController.navigate(EconnectRoute.Friends.route)
                }
            )
        }
    }
}

