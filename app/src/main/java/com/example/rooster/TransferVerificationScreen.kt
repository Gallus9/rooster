package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseCloud
import kotlinx.coroutines.launch

@Composable
fun TransferVerificationScreen(orderId: String, onVerified: () -> Unit) {
    var color by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Transfer Verification", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("Fowl Color") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = condition,
            onValueChange = { condition = it },
            label = { Text("Fowl Condition") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                try {
                    val params = mapOf("orderId" to orderId, "color" to color, "condition" to condition)
                    val result = ParseCloud.callFunctionInBackground<String>("verifyTransfer", params)
                    status = result.result // Use .result to get the String from Task
                    error = null
                    onVerified()
                } catch (e: Exception) {
                    error = e.localizedMessage
                }
            }
        }, enabled = color.isNotBlank() && condition.isNotBlank()) {
            Text("Verify Transfer")
        }
        status?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

