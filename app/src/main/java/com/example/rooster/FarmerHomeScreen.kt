package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FarmerHomeScreen() {
    var rankings by remember { mutableStateOf(listOf("Farmer John", "Farmer Jane", "Farmer Lee")) }
    var healthTips by remember { mutableStateOf(listOf("Keep waterers clean.", "Rotate pasture regularly.", "Monitor for parasites.")) }
    var alerts by remember { mutableStateOf(listOf("Vaccination due for flock #3", "Feed delivery tomorrow")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Farmer Home", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Rankings", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(rankings) { name ->
                Text("• $name", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Health Tips", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(healthTips) { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Alerts", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(alerts) { alert ->
                Text("• $alert", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// TODO: Connect rankings, health tips, and alerts to backend (Parse) for dynamic data.
