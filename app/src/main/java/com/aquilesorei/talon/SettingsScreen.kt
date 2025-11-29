package com.aquilesorei.talon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TalonDatabase.getDatabase(context) }
    val preferencesRepo = remember { UserPreferencesRepository(database.userPreferencesDao(), context) }
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
    
    val preferences by preferencesRepo.preferences.collectAsState(initial = UserPreferences())
    val measurements by historyViewModel.historyList.collectAsState()
    val userProfileRepo = remember { UserProfileRepository(database.userProfileDao()) }
    val userProfile by userProfileRepo.userProfile.collectAsState(initial = null)
    
    var showExportDialog by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, "Menu")
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Appearance Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Current: ${preferences.theme.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.menuAnchor()
                        ) {
                            Text(preferences.theme.name)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ThemePreference.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.name) },
                                    onClick = {
                                        scope.launch {
                                            preferencesRepo.updateTheme(theme)
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Data Management Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Export CSV
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val file = ExportUtils.exportToCSV(context, measurements)
                                ExportUtils.shareFile(context, file, "text/csv")
                                exportMessage = "Exported ${measurements.size} measurements"
                                showExportDialog = true
                            } catch (e: Exception) {
                                exportMessage = "Export failed: ${e.message}"
                                showExportDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export to CSV")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Backup JSON
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val file = ExportUtils.exportToJSON(context, measurements, userProfile)
                                ExportUtils.shareFile(context, file, "application/json")
                                exportMessage = "Backup created successfully"
                                showExportDialog = true
                            } catch (e: Exception) {
                                exportMessage = "Backup failed: ${e.message}"
                                showExportDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Backup (JSON)")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "${measurements.size} measurements stored",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notifications Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notifications", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weigh-in Reminders")
                    Switch(
                        checked = preferences.reminderEnabled,
                        onCheckedChange = {
                            scope.launch {
                                preferencesRepo.updateReminderEnabled(it)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Achievement Notifications")
                    Switch(
                        checked = preferences.achievementNotifications,
                        onCheckedChange = {
                            scope.launch {
                                preferencesRepo.updatePreferences(
                                    preferences.copy(achievementNotifications = it)
                                )
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // About Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Talon - Smart Scale Companion", style = MaterialTheme.typography.bodyLarge)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Track your body composition with precision and insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export") },
            text = { Text(exportMessage) },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
