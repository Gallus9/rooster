package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.launch

@Composable
fun TransfersScreen() {
    val coroutineScope = rememberCoroutineScope()
    var transfers by remember { mutableStateOf(listOf<Transfer>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newTransferTitle by remember { mutableStateOf("") }
    var newTransferTo by remember { mutableStateOf("") }
    var createSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fetchTransfers(onResult = { transfers = it }, onError = { error = it }, setLoading = { isLoading = it })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Transfers", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newTransferTitle,
            onValueChange = { newTransferTitle = it },
            label = { Text("Product/Item Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newTransferTo,
            onValueChange = { newTransferTo = it },
            label = { Text("Transfer To (username)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    createTransfer(newTransferTitle, newTransferTo,
                        onSuccess = {
                            createSuccess = true
                            newTransferTitle = ""
                            newTransferTo = ""
                            fetchTransfers(onResult = { transfers = it }, onError = { error = it }, setLoading = { isLoading = it })
                        },
                        onError = { error = it }
                    )
                }
            },
            enabled = newTransferTitle.isNotBlank() && newTransferTo.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Create Transfer")
        }
        if (createSuccess) {
            Text("Transfer created!", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (transfers.isEmpty()) {
            Text("No transfers found.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(transfers, key = { it.objectId }) { transfer ->
                    TransferCard(transfer)
                }
            }
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}

data class Transfer(val objectId: String, val title: String, val to: String, val status: String)

@Composable
fun TransferCard(transfer: Transfer) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = transfer.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "To: ${transfer.to}")
            Text(text = "Status: ${transfer.status}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun fetchTransfers(
    onResult: (List<Transfer>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Transfer")
        query.whereEqualTo("from", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        query.findInBackground { results, e ->
            setLoading(false)
            if (e != null) {
                onError(e.localizedMessage)
            } else {
                val items = results?.map {
                    Transfer(
                        objectId = it.objectId,
                        title = it.getString("title") ?: "",
                        to = it.getParseUser("to")?.username ?: "",
                        status = it.getString("status") ?: "pending"
                    )
                } ?: emptyList()
                onResult(items)
            }
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

fun createTransfer(
    title: String,
    toUsername: String,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit
) {
    try {
        val toQuery = ParseQuery.getQuery<ParseUser>("_User")
        toQuery.whereEqualTo("username", toUsername)
        toQuery.getFirstInBackground { toUser, e ->
            if (e != null || toUser == null) {
                onError("Recipient not found")
            } else {
                val transfer = ParseObject("Transfer")
                transfer.put("title", title)
                transfer.put("from", ParseUser.getCurrentUser())
                transfer.put("to", toUser)
                transfer.put("status", "pending")
                transfer.saveInBackground { err ->
                    if (err != null) {
                        onError(err.localizedMessage)
                    } else {
                        onSuccess()
                    }
                }
            }
        }
    } catch (e: Exception) {
        onError(e.localizedMessage)
    }
}

