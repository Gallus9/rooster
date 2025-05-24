package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.parse.ParseACL
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser

data class FowlData(
    val objectId: String,
    val name: String,
    val type: String,
    val birthDate: String,
    val children: List<FowlData>? = null
)

@Composable
fun LineageTree(fowls: List<FowlData>, depth: Int = 0) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        fowls.forEach { fowl ->
            Text("└── ${fowl.name} (${fowl.type})")
            fowl.children?.let { LineageTree(it, depth + 1) }
        }
    }
}

@Composable
fun FowlCard(fowl: FowlData) {
    var lineage by remember { mutableStateOf<List<FowlData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(fowl) {
        isLoading = true
        lineage = fetchLineage(fowl)
        isLoading = false
    }

    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Name: ${fowl.name}")
            Text("Type: ${fowl.type}")
            Text("Birth Date: ${fowl.birthDate}")
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LineageTree(lineage)
            }
        }
    }
}

suspend fun fetchLineage(fowl: FowlData): List<FowlData> {
    val lineage = mutableListOf<FowlData>()
    var currentFowl: ParseObject? = ParseQuery.getQuery<ParseObject>("Fowl").get(fowl.objectId)

    while (currentFowl?.getParseObject("parentId") != null) {
        val parent = currentFowl.getParseObject("parentId")
        lineage.add(0, FowlData(
            objectId = parent?.objectId ?: "",
            name = parent?.getString("name") ?: "Unknown",
            type = parent?.getString("type") ?: "Unknown",
            birthDate = parent?.getString("birthDate") ?: "Unknown"
        ))
        currentFowl = parent
    }

    return lineage
}

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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
                    FowlCard(fowl = FowlData(
                        objectId = fowl.objectId,
                        name = fowl.getString("name") ?: "Unknown",
                        type = fowl.getString("type") ?: "Unknown",
                        birthDate = fowl.getString("birthDate") ?: "Unknown"
                    ))
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
