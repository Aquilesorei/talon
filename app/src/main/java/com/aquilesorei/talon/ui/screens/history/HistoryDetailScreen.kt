package com.aquilesorei.talon.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquilesorei.talon.data.local.entities.Measurement
import com.aquilesorei.talon.domain.models.BodyComposition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    measurement: Measurement,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (Measurement) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${measurement.dateString()} at ${measurement.timeString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gros indicateur de poids
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("WEIGHT", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${measurement.weight} kg",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BMI Card with Category
            if (measurement.bmi > 0) {
                val bmiCategory = BodyComposition(
                    bmi = measurement.bmi,
                    bodyFatPercentage = measurement.bodyFat,
                    waterPercentage = measurement.water,
                    muscleMass = measurement.muscle,
                    boneMass = measurement.boneMass,
                    metabolicAge = measurement.metabolicAge,
                    bmr = measurement.bmr
                ).getBMICategory()
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(bmiCategory.color).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("BMI", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = String.format("%.1f", measurement.bmi),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bmiCategory.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(bmiCategory.color)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Grille des détails
            Text("Body Composition", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Body Fat",
                    value = String.format("%.1f %%", measurement.bodyFat),
                    icon = Icons.Default.Opacity,
                    color = Color(0xFFE57373) // Rouge/Rose
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Water",
                    value = String.format("%.1f %%", measurement.water),
                    icon = Icons.Default.WaterDrop,
                    color = Color(0xFF64B5F6) // Bleu
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Muscle",
                    value = String.format("%.1f kg", measurement.muscle),
                    icon = Icons.Default.FitnessCenter,
                    color = Color(0xFF81C784) // Vert
                )
                MetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Impedance",
                    value = "${measurement.impedance.toInt()} Ω",
                    icon = Icons.Default.ElectricBolt,
                    color = Color(0xFFFFD54F) // Jaune
                )
            }
            
            // Additional Metrics Section
            if (measurement.boneMass > 0 || measurement.metabolicAge > 0 || measurement.bmr > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Additional Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (measurement.boneMass > 0) {
                        MetricItem(
                            modifier = Modifier.weight(1f),
                            label = "Bone Mass",
                            value = String.format("%.2f kg", measurement.boneMass),
                            icon = Icons.Default.FitnessCenter,
                            color = Color(0xFFBCAAA4) // Brown
                        )
                    }
                    if (measurement.metabolicAge > 0) {
                        MetricItem(
                            modifier = Modifier.weight(1f),
                            label = "Metabolic Age",
                            value = "${measurement.metabolicAge} yrs",
                            icon = Icons.Default.Opacity,
                            color = Color(0xFFBA68C8) // Purple
                        )
                    }
                }
                
                if (measurement.bmr > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF7043).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFFFF7043))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("${measurement.bmr} kcal/day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Basal Metabolic Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }


    if (showEditDialog) {
        EditMeasurementDialog(
            measurement = measurement,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedMeasurement ->
                onEdit(updatedMeasurement)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditMeasurementDialog(
    measurement: Measurement,
    onDismiss: () -> Unit,
    onConfirm: (Measurement) -> Unit
) {
    var weight by remember { mutableStateOf(measurement.weight.toString()) }
    var bodyFat by remember { mutableStateOf(measurement.bodyFat.toString()) }
    var notes by remember { mutableStateOf(measurement.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Measurement") },
        text = {
            Column {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it },
                    label = { Text("Body Fat (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newWeight = weight.toFloatOrNull() ?: measurement.weight
                    val newBodyFat = bodyFat.toFloatOrNull() ?: measurement.bodyFat
                    
                    onConfirm(
                        measurement.copy(
                            weight = newWeight,
                            bodyFat = newBodyFat,
                            notes = notes.ifBlank { null }
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MetricItem(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}