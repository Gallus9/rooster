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

@Composable
fun DashboardScreen() {
    val coroutineScope = rememberCoroutineScope()
    var farmDetails by remember { mutableStateOf<FarmDetails?>(null) }
    var familyTree by remember { mutableStateOf(listOf<FamilyTreeNode>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fetchFarmDetails(onResult = { farmDetails = it }, onError = { error = it }, setLoading = { isLoading = it })
        fetchFamilyTree(onResult = { familyTree = it }, onError = { error = it }, setLoading = { })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Farm Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            farmDetails?.let { details ->
                Text("Farm Name: ${details.name}")
                Text("Location: ${details.location}")
                Text("Bird Count: ${details.birdCount}")
                Text("Turnover: ${details.turnover}")
                Spacer(modifier = Modifier.height(8.dp))
            } ?: Text("No farm details found.")
            Text("Family Tree:", style = MaterialTheme.typography.titleMedium)
            if (familyTree.isEmpty()) {
                Text("No family tree data.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(familyTree, key = { it.objectId }) { node ->
                        FamilyTreeNodeCard(node)
                    }
                }
            }
        }
        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}

data class FarmDetails(val name: String, val location: String, val birdCount: Int, val turnover: String)

data class FamilyTreeNode(val objectId: String, val name: String, val relation: String)

@Composable
fun FamilyTreeNodeCard(node: FamilyTreeNode) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = node.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Relation: ${node.relation}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun fetchFarmDetails(
    onResult: (FarmDetails?) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    setLoading(true)
    try {
        val query = ParseQuery.getQuery<ParseObject>("Farm")
        query.whereEqualTo("owner", ParseUser.getCurrentUser())
        query.getFirstInBackground { obj, e ->
            setLoading(false)
            if (e != null) {
                onError(e.localizedMessage)
            } else if (obj != null) {
                onResult(
                    FarmDetails(
                        name = obj.getString("name") ?: "",
                        location = obj.getString("location") ?: "",
                        birdCount = obj.getInt("birdCount"),
                        turnover = obj.getString("turnover") ?: "",
                    ),
                )
            } else {
                onResult(null)
            }
        }
    } catch (e: Exception) {
        setLoading(false)
        onError(e.localizedMessage)
    }
}

fun fetchFamilyTree(
    onResult: (List<FamilyTreeNode>) -> Unit,
    onError: (String?) -> Unit,
    setLoading: (Boolean) -> Unit,
) {
    try {
        val query = ParseQuery.getQuery<ParseObject>("FamilyTreeNode")
        query.whereEqualTo("owner", ParseUser.getCurrentUser())
        query.findInBackground { results, e ->
            if (e != null) {
                onError(e.localizedMessage)
            } else {
                val nodes =
                    results?.map {
                        FamilyTreeNode(
                            objectId = it.objectId,
                            name = it.getString("name") ?: "",
                            relation = it.getString("relation") ?: "",
                        )
                    } ?: emptyList()
                onResult(nodes)
            }
        }
    } catch (e: Exception) {
        onError(e.localizedMessage)
    }
}
