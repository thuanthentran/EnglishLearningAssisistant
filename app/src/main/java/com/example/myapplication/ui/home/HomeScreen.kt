package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.UserPreferences
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val username = userPreferences.getUsername() ?: "User"
    val email = userPreferences.getEmail() ?: "No Email"

    HomeScreenContent(
        username = username,
        email = email,
        onLogout = {
            userPreferences.clearUserSession()
            onLogout()
        }
    )
}

@Composable
fun HomeScreenContent(
    username: String,
    email: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Chào mừng, $username!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Email: $email", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onLogout) {
            Text("Đăng Xuất")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreenContent(
            username = "Nguyen Van A",
            email = "nguyenvana@example.com",
            onLogout = {}
        )
    }
}