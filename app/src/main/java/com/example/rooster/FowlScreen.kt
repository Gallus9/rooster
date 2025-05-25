package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.launch

data class FowlData(
    val objectId: String,
    val name: String,
    val type: String,
    val birthDate: String,
    val children: List<FowlData>? = null,
)

// Enhanced FowlScreen with milestone tracking
@Composable
fun FowlScreen() {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Rooster") }
    var birthDate by remember { mutableStateOf("") }
    var fowls by remember { mutableStateOf(listOf<ParseObject>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showAddFowlDialog by remember { mutableStateOf(false) }
    var selectedMilestone by remember { mutableStateOf<Pair<MilestoneType, FowlData>?>(null) }

    // Milestone tracking service
    val milestoneService = remember { MilestoneTrackingService() }
    val coroutineScope = rememberCoroutineScope()

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
                showAddFowlDialog = false
                fetchFowls()
            } else {
                error = e.localizedMessage ?: "Failed to add fowl."
            }
        }
    }

    LaunchedEffect(Unit) { fetchFowls() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Fowl Management",
                style = MaterialTheme.typography.headlineSmall,
            )
            FloatingActionButton(
                onClick = { showAddFowlDialog = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Fowl")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (error.isNotEmpty()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(fowls) { fowl ->
                    val fowlData =
                        FowlData(
                            objectId = fowl.objectId,
                            name = fowl.getString("name") ?: "Unknown",
                            type = fowl.getString("type") ?: "Unknown",
                            birthDate = fowl.getString("birthDate") ?: "Unknown",
                        )

                    EnhancedFowlCard(
                        fowlData = fowlData,
                        milestoneService = milestoneService,
                        onAddMilestone = { milestone ->
                            selectedMilestone = milestone to fowlData
                        },
                    )
                }
            }
        }
    }

    // Add fowl dialog
    if (showAddFowlDialog) {
        AddFowlDialog(
            name = name,
            onNameChange = { name = it },
            type = type,
            onTypeChange = { type = it },
            birthDate = birthDate,
            onBirthDateChange = { birthDate = it },
            onDismiss = {
                showAddFowlDialog = false
                name = ""
                birthDate = ""
            },
            onConfirm = { addFowl() },
        )
    }

    // Milestone recording dialog
    selectedMilestone?.let { (milestone, fowlData) ->
        val currentAge = milestoneService.calculateAgeInWeeks(fowlData.birthDate)
        MilestoneRecordingDialog(
            milestone = milestone,
            fowlData = fowlData,
            currentAgeWeeks = currentAge,
            onDismiss = { selectedMilestone = null },
            onSave = { milestoneData ->
                // Save milestone using coroutine scope
                coroutineScope.launch {
                    milestoneService.saveMilestone(
                        milestoneData = milestoneData,
                        onSuccess = {
                            selectedMilestone = null
                            // Refresh the fowl list to update milestone counts
                            fetchFowls()
                        },
                        onError = { errorMsg ->
                            error = errorMsg
                            selectedMilestone = null
                        },
                    )
                }
            },
        )
    }
}

@Composable
fun EnhancedFowlCard(
    fowlData: FowlData,
    milestoneService: MilestoneTrackingService,
    onAddMilestone: (MilestoneType) -> Unit,
) {
    var progress by remember { mutableStateOf<FowlMilestoneProgress?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var lineage by remember { mutableStateOf<List<FowlData>>(emptyList()) }

    LaunchedEffect(fowlData) {
        isLoading = true

        // Fetch milestone progress
        milestoneService.getFowlMilestoneProgress(
            fowlId = fowlData.objectId,
            fowlType = fowlData.type,
            birthDate = fowlData.birthDate,
            onResult = { milestoneProgress ->
                progress = milestoneProgress
                isLoading = false
            },
            onError = {
                isLoading = false
            },
        )

        // Fetch lineage
        lineage = fetchLineage(fowlData)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Basic fowl information
            Text("Name: ${fowlData.name}")
            Text("Type: ${fowlData.type}")
            Text("Birth Date: ${fowlData.birthDate}")

            Spacer(modifier = Modifier.height(12.dp))

            // Enhanced milestone progress
            progress?.let { milestoneProgress ->
                MilestoneProgressCard(
                    fowlData = fowlData,
                    progress = milestoneProgress,
                    onAddMilestone = onAddMilestone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Health Records Section (existing functionality)
            HealthRecordsSection(fowlId = fowlData.objectId)

            // Lineage Tree (existing functionality)
            if (lineage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Lineage:",
                    style = MaterialTheme.typography.titleSmall,
                )
                LineageTree(lineage)
            }

            if (isLoading) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AddFowlDialog(
    name: String,
    onNameChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    birthDate: String,
    onBirthDateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Fowl") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Column {
                    Text("Type:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = type == "Rooster",
                            onClick = { onTypeChange("Rooster") },
                        )
                        Text("Rooster")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = type == "Hen", onClick = { onTypeChange("Hen") })
                        Text("Hen")
                    }
                }

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = onBirthDateChange,
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank() && birthDate.isNotBlank(),
            ) {
                Text("Add Fowl")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun LineageTree(
    fowls: List<FowlData>,
    depth: Int = 0,
) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        fowls.forEach { fowl ->
            Text("└── ${fowl.name} (${fowl.type})")
            fowl.children?.let { LineageTree(it, depth + 1) }
        }
    }
}

@Composable
fun HealthRecordsSection(fowlId: String) {
    var records by remember { mutableStateOf(listOf<ParseObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fowlId) {
        fetchHealthRecords(
            fowlId = fowlId,
            onResult = { records = it },
            onError = { error = it },
            setLoading = { isLoading = it },
        )
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text("Health Records:", style = MaterialTheme.typography.titleSmall)
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else if (records.isEmpty()) {
            Text("No health records.", style = MaterialTheme.typography.bodySmall)
        } else {
            records.take(3).forEach { record ->
                Text(
                    "${record.getString("date")}: ${record.getString("description")}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (records.size > 3) {
                Text(
                    "... and ${records.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        error?.let {
            Text(
                "Error: $it",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

suspend fun fetchLineage(fowl: FowlData): List<FowlData> {
    val lineage = mutableListOf<FowlData>()
    var currentFowl: ParseObject? = ParseQuery.getQuery<ParseObject>("Fowl").get(fowl.objectId)

    while (currentFowl?.getParseObject("parentId") != null) {
        val parent = currentFowl.getParseObject("parentId")
        lineage.add(
            0,
            FowlData(
                objectId = parent?.objectId ?: "",
                name = parent?.getString("name") ?: "Unknown",
                type = parent?.getString("type") ?: "Unknown",
                birthDate = parent?.getString("birthDate") ?: "Unknown",
            ),
        )
        currentFowl = parent
    }

    return lineage
}

@Preview(showBackground = true)
@Composable
fun FowlScreenPreview() {
    // This preview uses default state; for richer previews, mock ParseObject data as needed
    FowlScreen()
}
