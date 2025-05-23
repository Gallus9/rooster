package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.ParseACL
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun FowlScreen() {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Rooster") }
    var birthDate by remember { mutableStateOf("") }
    var fowls by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun fetchFowls() {
        loading = true
        val query = ParseQuery.getQuery<ParseObject>("Fowl")
        query.whereEqualTo("owner", ParseUser.getCurrentUser())
        query.orderByDescending("createdAt")
        query.findInBackground { result, e ->
            loading = false
            if (e == null && result != null) {
                fowls = result
                // Pin to local datastore for offline support
                ParseObject.pinAllInBackground("Fowl", result)
            } else {
                // Try to load from local datastore if network fails
                val localQuery = ParseQuery.getQuery<ParseObject>("Fowl")
                localQuery.fromLocalDatastore()
                localQuery.whereEqualTo("owner", ParseUser.getCurrentUser())
                localQuery.orderByDescending("createdAt")
                localQuery.findInBackground { localResult, _ ->
                    fowls = localResult ?: emptyList()
                    error = e?.localizedMessage ?: "Failed to load fowls. Showing offline data."
                }
            }
        }
    }

    fun addFowl() {
        val fowl = ParseObject("Fowl")
        fowl.put("name", name)
        fowl.put("type", type)
        fowl.put("birthDate", birthDate)
        fowl.put("owner", ParseUser.getCurrentUser())
        val acl = ParseACL(ParseUser.getCurrentUser())
        acl.setPublicReadAccess(true)
        acl.setWriteAccess(ParseUser.getCurrentUser(), true)
        fowl.acl = acl
        fowl.saveInBackground { e ->
            if (e == null) {
                // Pin to local datastore after saving
                fowl.pinInBackground()
                name = ""
                birthDate = ""
                fetchFowls()
            } else {
                error = e.localizedMessage ?: "Failed to add fowl."
            }
        }
    }

    LaunchedEffect(Unit) { fetchFowls() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Fowl Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = type == "Rooster", onClick = { type = "Rooster" })
            Text("Rooster")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = type == "Hen", onClick = { type = "Hen" })
            Text("Hen")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            label = { Text("Birth Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { addFowl() }, enabled = name.isNotBlank() && birthDate.isNotBlank()) {
            Text("Add Fowl")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(fowls) { fowl ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${fowl.getString("name")}")
                            Text("Type: ${fowl.getString("type")}")
                            Text("Birth Date: ${fowl.getString("birthDate")}")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FowlScreenPreview() {
    // This preview uses default state; for richer previews, mock ParseObject data as needed
    FowlScreen()
}
