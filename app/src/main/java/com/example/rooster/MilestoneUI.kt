package com.example.rooster

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.parse.ParseUser
import java.util.Date

@Composable
fun MilestoneProgressCard(
    fowlData: FowlData,
    progress: FowlMilestoneProgress,
    onAddMilestone: (MilestoneType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header with fowl info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = fowlData.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${fowlData.type} • ${progress.currentAgeWeeks} weeks old",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Breeder qualification badge
                if (progress.isBreederQualified) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Breeder Qualified", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50),
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicators
            MilestoneProgressIndicators(progress)

            Spacer(modifier = Modifier.height(16.dp))

            // Pending milestones
            if (progress.pendingMilestones.isNotEmpty()) {
                Text(
                    text = "Pending Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                progress.pendingMilestones.take(3).forEach { milestone ->
                    PendingMilestoneItem(
                        milestone = milestone,
                        onClick = { onAddMilestone(milestone) },
                    )
                }
            }

            // Overdue milestones (priority)
            if (progress.overdueMilestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "⚠️ Overdue Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
                progress.overdueMilestones.forEach { milestone ->
                    OverdueMilestoneItem(
                        milestone = milestone,
                        onClick = { onAddMilestone(milestone) },
                    )
                }
            }
        }
    }
}

@Composable
fun MilestoneProgressIndicators(progress: FowlMilestoneProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProgressIndicatorItem(
            icon = Icons.Filled.Done,
            count = progress.completedMilestones.size,
            label = "Completed",
            color = Color(0xFF4CAF50),
        )
        ProgressIndicatorItem(
            icon = Icons.Filled.Schedule,
            count = progress.pendingMilestones.size,
            label = "Pending",
            color = Color(0xFFFF9800),
        )
        ProgressIndicatorItem(
            icon = Icons.Filled.Warning,
            count = progress.overdueMilestones.size,
            label = "Overdue",
            color = Color(0xFFF44336),
        )
    }
}

@Composable
fun ProgressIndicatorItem(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Badge(
            containerColor = color,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White,
                )
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    color = Color.White,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PendingMilestoneItem(
    milestone: MilestoneType,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milestone.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OverdueMilestoneItem(
    milestone: MilestoneType,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "URGENT: ${milestone.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun MilestoneRecordingDialog(
    milestone: MilestoneType,
    fowlData: FowlData,
    currentAgeWeeks: Int,
    onDismiss: () -> Unit,
    onSave: (MilestoneData) -> Unit,
) {
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var healthStatus by rememberSaveable { mutableStateOf("Healthy") }
    var vaccineType by rememberSaveable { mutableStateOf("") }
    var vaccineBatch by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var qualificationScore by rememberSaveable { mutableStateOf("") }
    var causeOfDeath by rememberSaveable { mutableStateOf("") }
    var reproductiveHealth by rememberSaveable { mutableStateOf("") }
    var physicalCondition by rememberSaveable { mutableStateOf("") }

    val photoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            photoUri = uri
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = milestone.name,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = milestone.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Photo section (always first for user-friendly flow)
                if (milestone.requiresPhoto) {
                    item {
                        PhotoCaptureSection(
                            photoUri = photoUri,
                            onPhotoClick = { photoLauncher.launch("image/*") },
                        )
                    }
                }

                // Dynamic fields based on milestone requirements
                milestone.requiredFields.forEach { field ->
                    when (field) {
                        "weight" ->
                            item {
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("Weight (grams)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                        "height" ->
                            item {
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text("Height (cm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                        "color" ->
                            item {
                                ColorSelectionField(
                                    selectedColor = color,
                                    onColorChange = { color = it },
                                )
                            }

                        "confirmedGender", "gender" ->
                            item {
                                GenderSelectionField(
                                    selectedGender = gender,
                                    onGenderChange = { gender = it },
                                )
                            }

                        "healthStatus" ->
                            item {
                                HealthStatusField(
                                    selectedStatus = healthStatus,
                                    onStatusChange = { healthStatus = it },
                                )
                            }

                        "vaccineType" ->
                            item {
                                VaccineTypeField(
                                    vaccineType = vaccineType,
                                    onTypeChange = { vaccineType = it },
                                )
                            }

                        "vaccineBatch" ->
                            item {
                                OutlinedTextField(
                                    value = vaccineBatch,
                                    onValueChange = { vaccineBatch = it },
                                    label = { Text("Vaccine Batch Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                        "qualificationScore" ->
                            item {
                                OutlinedTextField(
                                    value = qualificationScore,
                                    onValueChange = { qualificationScore = it },
                                    label = { Text("Qualification Score (0-100)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                        "causeOfDeath" ->
                            item {
                                CauseOfDeathField(
                                    causeOfDeath = causeOfDeath,
                                    onCauseChange = { causeOfDeath = it },
                                )
                            }

                        "reproductiveHealth" ->
                            item {
                                ReproductiveHealthField(
                                    reproductiveHealth = reproductiveHealth,
                                    onHealthChange = { reproductiveHealth = it },
                                )
                            }

                        "physicalCondition" ->
                            item {
                                PhysicalConditionField(
                                    physicalCondition = physicalCondition,
                                    onConditionChange = { physicalCondition = it },
                                )
                            }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val milestoneData =
                        MilestoneData(
                            objectId = "",
                            fowlId = fowlData.objectId,
                            milestoneType = milestone.id,
                            recordedDate = Date(),
                            fowlAgeWeeks = currentAgeWeeks,
                            recordedBy = ParseUser.getCurrentUser()?.objectId ?: "",
                            photo = null,
                            photoVerificationHash = null,
                            gpsLocation = null,
                            weight = weight.toDoubleOrNull(),
                            height = height.toDoubleOrNull(),
                            color = color.takeIf { it.isNotBlank() },
                            gender = gender.takeIf { it.isNotBlank() },
                            healthStatus = healthStatus.takeIf { it.isNotBlank() },
                            vaccineType = vaccineType.takeIf { it.isNotBlank() },
                            vaccineBatch = vaccineBatch.takeIf { it.isNotBlank() },
                            physicalCondition = physicalCondition.takeIf { it.isNotBlank() },
                            reproductiveHealth = reproductiveHealth.takeIf { it.isNotBlank() },
                            qualificationScore = qualificationScore.toIntOrNull(),
                            causeOfDeath = causeOfDeath.takeIf { it.isNotBlank() },
                            parentId = null,
                            birthWeight = null,
                            veterinaryReport = null,
                            notes = notes.takeIf { it.isNotBlank() },
                            isVerified = false,
                            verificationTimestamp = null,
                            fraudPreventionData = MilestoneTrackingService().generateFraudPreventionData(),
                        )
                    onSave(milestoneData)
                },
            ) {
                Text("Save Milestone")
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
fun PhotoCaptureSection(
    photoUri: Uri?,
    onPhotoClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clickable { onPhotoClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        border =
            BorderStroke(
                2.dp,
                if (photoUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Overlay with camera icon for re-capture
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp),
                            )
                            .padding(8.dp),
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Retake photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to add photo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Photo required for verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// Specialized input fields with farmer-friendly interfaces

@Composable
fun ColorSelectionField(
    selectedColor: String,
    onColorChange: (String) -> Unit,
) {
    val colors =
        listOf(
            "Red", "Black", "White", "Brown", "Mixed Red-Black",
            "Mixed Brown-White", "Golden", "Silver", "Other",
        )

    Column {
        Text("Select Color:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        colors.chunked(3).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowColors.forEach { color ->
                    FilterChip(
                        onClick = { onColorChange(color) },
                        label = { Text(color, fontSize = 12.sp) },
                        selected = selectedColor == color,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun GenderSelectionField(
    selectedGender: String,
    onGenderChange: (String) -> Unit,
) {
    Column {
        Text("Confirmed Gender:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf("Rooster", "Hen", "Uncertain").forEach { gender ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedGender == gender,
                        onClick = { onGenderChange(gender) },
                    )
                    Text(gender)
                }
            }
        }
    }
}

@Composable
fun HealthStatusField(
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
) {
    val statuses = listOf("Healthy", "Minor Issues", "Major Concerns", "Under Treatment")

    Column {
        Text("Health Status:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        statuses.forEach { status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onStatusChange(status) },
            ) {
                RadioButton(
                    selected = selectedStatus == status,
                    onClick = { onStatusChange(status) },
                )
                Text(status)
            }
        }
    }
}

@Composable
fun VaccineTypeField(
    vaccineType: String,
    onTypeChange: (String) -> Unit,
) {
    val vaccines =
        listOf(
            "Newcastle Disease",
            "Infectious Bronchitis",
            "Fowl Pox",
            "Infectious Bursal Disease",
            "Marek's Disease",
            "Other",
        )

    Column {
        Text("Vaccine Type:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        vaccines.chunked(2).forEach { rowVaccines ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowVaccines.forEach { vaccine ->
                    FilterChip(
                        onClick = { onTypeChange(vaccine) },
                        label = { Text(vaccine, fontSize = 11.sp) },
                        selected = vaccineType == vaccine,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun CauseOfDeathField(
    causeOfDeath: String,
    onCauseChange: (String) -> Unit,
) {
    val causes =
        listOf(
            "Disease",
            "Predator Attack",
            "Accident",
            "Natural Causes",
            "Unknown",
            "Veterinary Decision",
            "Other",
        )

    Column {
        Text("Cause of Death:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        causes.chunked(2).forEach { rowCauses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCauses.forEach { cause ->
                    FilterChip(
                        onClick = { onCauseChange(cause) },
                        label = { Text(cause, fontSize = 12.sp) },
                        selected = causeOfDeath == cause,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ReproductiveHealthField(
    reproductiveHealth: String,
    onHealthChange: (String) -> Unit,
) {
    val healthOptions =
        listOf(
            "Excellent",
            "Good",
            "Fair",
            "Poor",
            "Not Assessed",
        )

    Column {
        Text("Reproductive Health:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            healthOptions.forEach { option ->
                FilterChip(
                    onClick = { onHealthChange(option) },
                    label = { Text(option, fontSize = 12.sp) },
                    selected = reproductiveHealth == option,
                )
            }
        }
    }
}

@Composable
fun PhysicalConditionField(
    physicalCondition: String,
    onConditionChange: (String) -> Unit,
) {
    val conditions =
        listOf(
            "Excellent",
            "Good",
            "Average",
            "Below Average",
            "Poor",
        )

    Column {
        Text("Physical Condition:", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            conditions.forEach { condition ->
                FilterChip(
                    onClick = { onConditionChange(condition) },
                    label = { Text(condition, fontSize = 12.sp) },
                    selected = physicalCondition == condition,
                )
            }
        }
    }
}
