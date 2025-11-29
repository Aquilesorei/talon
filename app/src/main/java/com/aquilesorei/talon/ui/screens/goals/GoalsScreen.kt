package com.aquilesorei.talon.ui.screens.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import com.aquilesorei.talon.viewmodels.HistoryViewModel
import com.aquilesorei.talon.domain.models.UserProfile
import com.aquilesorei.talon.data.local.entities.GoalStatus
import com.aquilesorei.talon.data.local.entities.GoalType
import com.aquilesorei.talon.data.local.entities.Goal
import com.aquilesorei.talon.data.local.entities.Measurement
import com.aquilesorei.talon.data.repository.UserProfileRepository
import com.aquilesorei.talon.data.repository.GoalRepository
import com.aquilesorei.talon.data.repository.MeasurementRepository
import com.aquilesorei.talon.data.local.database.TalonDatabase
import com.aquilesorei.talon.viewmodels.HistoryViewModelFactory
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TalonDatabase.getDatabase(context) }
    val goalRepo = remember { GoalRepository(database.goalDao()) }
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
    
    val goals by goalRepo.activeGoals.collectAsState(initial = emptyList())
    val measurements by historyViewModel.historyList.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Get latest measurement for current values
    val latestMeasurement = measurements.firstOrNull()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, "Menu")
                }
                Text(
                    text = "Goals",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Add Goal")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (goals.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No active goals", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Set a goal to track your progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Goal")
                    }
                }
            }
        } else {
            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    currentValue = when (goal.type) {
                        GoalType.WEIGHT -> latestMeasurement?.weight ?: goal.startValue
                        GoalType.BODY_FAT -> latestMeasurement?.bodyFat ?: goal.startValue
                        GoalType.MUSCLE_MASS -> latestMeasurement?.muscle ?: goal.startValue
                    },
                    onDelete = {
                        scope.launch {
                            goalRepo.deleteGoal(goal)
                        }
                    },
                    onAchieve = {
                        scope.launch {
                            goalRepo.achieveGoal(goal.id)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    
    if (showCreateDialog) {
        CreateGoalDialog(
            currentWeight = latestMeasurement?.weight ?: 70f,
            currentBodyFat = latestMeasurement?.bodyFat ?: 20f,
            currentMuscle = latestMeasurement?.muscle ?: 50f,
            onDismiss = { showCreateDialog = false },
            onCreate = { goal ->
                scope.launch {
                    goalRepo.createGoal(goal)
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    currentValue: Float,
    onDelete: () -> Unit,
    onAchieve: () -> Unit
) {
    val progress = goal.calculateProgress(currentValue)
    val isAchieved = goal.isAchieved(currentValue)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isAchieved) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (goal.type) {
                            GoalType.WEIGHT -> "Weight Goal"
                            GoalType.BODY_FAT -> "Body Fat Goal"
                            GoalType.MUSCLE_MASS -> "Muscle Mass Goal"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${formatGoalValue(goal.targetValue, goal.type)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isAchieved) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Achieved",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Current: ${formatGoalValue(currentValue, goal.type)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${progress.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Deadline
            goal.deadline?.let { deadline ->
                Spacer(modifier = Modifier.height(8.dp))
                val daysLeft = ((deadline - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                Text(
                    text = if (daysLeft > 0) "$daysLeft days left" else "Deadline passed",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysLeft > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    currentWeight: Float,
    currentBodyFat: Float,
    currentMuscle: Float,
    onDismiss: () -> Unit,
    onCreate: (Goal) -> Unit
) {
    var selectedType by remember { mutableStateOf(GoalType.WEIGHT) }
    var targetValue by remember { mutableStateOf("") }
    var daysToDeadline by remember { mutableStateOf("30") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Goal") },
        text = {
            Column {
                Text("Goal Type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == GoalType.WEIGHT,
                        onClick = { selectedType = GoalType.WEIGHT },
                        label = { Text("Weight") }
                    )
                    FilterChip(
                        selected = selectedType == GoalType.BODY_FAT,
                        onClick = { selectedType = GoalType.BODY_FAT },
                        label = { Text("Body Fat") }
                    )
                    FilterChip(
                        selected = selectedType == GoalType.MUSCLE_MASS,
                        onClick = { selectedType = GoalType.MUSCLE_MASS },
                        label = { Text("Muscle") }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    label = { Text("Target Value") },
                    suffix = { Text(getGoalUnit(selectedType)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = daysToDeadline,
                    onValueChange = { daysToDeadline = it },
                    label = { Text("Days to Deadline") },
                    suffix = { Text("days") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetValue.toFloatOrNull() ?: return@TextButton
                    val days = daysToDeadline.toIntOrNull() ?: 30
                    val deadline = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                    
                    val startValue = when (selectedType) {
                        GoalType.WEIGHT -> currentWeight
                        GoalType.BODY_FAT -> currentBodyFat
                        GoalType.MUSCLE_MASS -> currentMuscle
                    }
                    
                    onCreate(
                        Goal(
                            type = selectedType,
                            targetValue = target,
                            startValue = startValue,
                            deadline = deadline
                        )
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatGoalValue(value: Float, type: GoalType): String {
    return when (type) {
        GoalType.WEIGHT -> String.format("%.1f kg", value)
        GoalType.BODY_FAT -> String.format("%.1f%%", value)
        GoalType.MUSCLE_MASS -> String.format("%.1f kg", value)
    }
}

fun getGoalUnit(type: GoalType): String {
    return when (type) {
        GoalType.WEIGHT -> "kg"
        GoalType.BODY_FAT -> "%"
        GoalType.MUSCLE_MASS -> "kg"
    }
}
