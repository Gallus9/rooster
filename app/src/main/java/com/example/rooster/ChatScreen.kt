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
fun ChatScreen(chatId: String) {
    var messages by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newMessage by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        fetchChatMessages(
            chatId = chatId,
            onResult = { messages = it },
            onError = { error = it },
            setLoading = { isLoading = it },
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chat", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (messages.isEmpty()) {
            Text("No messages yet.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(messages) { msg ->
                    Card(modifier = Modifier.padding(vertical = 2.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(msg.getParseUser("user")?.username ?: "User", style = MaterialTheme.typography.labelSmall)
                            Text(msg.getString("text") ?: "", style = MaterialTheme.typography.bodyMedium)
                            Text(msg.createdAt?.toString() ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newMessage,
            onValueChange = { newMessage = it },
            label = { Text("Type a message...") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                // Send message logic
                sendChatMessage(
                    chatId = chatId,
                    message = newMessage,
                    onSuccess = {
                        newMessage = ""
                        fetchChatMessages(
                            chatId = chatId,
                            onResult = { messages = it },
                            onError = { error = it },
                            setLoading = { isLoading = it },
                        )
                    },
                    onError = { error = it },
                )
            },
            enabled = newMessage.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Send")
        }
    }
}

fun sendChatMessage(
    chatId: String,
    message: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit,
) {
    try {
        val chatMessage = ParseObject("ChatMessage")
        chatMessage.put("chatId", chatId)
        chatMessage.put("user", com.parse.ParseUser.getCurrentUser())
        chatMessage.put("text", message)
        chatMessage.saveInBackground { e ->
            if (e == null) onSuccess() else onError(e.localizedMessage)
        }
    } catch (e: Exception) {
        onError(e.localizedMessage)
    }
}
