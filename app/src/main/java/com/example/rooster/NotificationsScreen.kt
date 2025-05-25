package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseObject

@Composable
fun NotificationsScreen() {
    var notifications by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fetchNotifications(
            onResult = { notifications = it },
            onError = { error = it },
            setLoading = { isLoading = it },
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notifications", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (notifications.isEmpty()) {
            Text("No notifications found.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications) { notification ->
                    Card(modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(notification.getString("title") ?: "Notification", style = MaterialTheme.typography.titleMedium)
                            Text(notification.getString("message") ?: "", style = MaterialTheme.typography.bodyMedium)
                            Text(notification.createdAt?.toString() ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}
