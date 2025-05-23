package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseUser

@Composable
fun ProfileScreen(onLogout: () -> Unit = {}) {
    val user = ParseUser.getCurrentUser()
    val username = user?.username ?: "Unknown"
    val role = user?.getString("role") ?: "Unknown"
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Username: $username")
        Text("Role: $role")
        Spacer(modifier = Modifier.height(32.dp))
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = {
            try {
                ParseUser.logOut()
                onLogout()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Logout failed."
            }
        }) {
            Text("Logout")
        }
    }
}

