package com.aquilesorei.talon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))

    // On écoute la liste des pesées depuis la BDD
    val historyList by viewModel.historyList.collectAsState()

    // Gestion de la navigation interne (Liste <-> Détail)
    var selectedMeasurement by remember { mutableStateOf<Measurement?>(null) }
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("date") } // date, weight, bodyFat
    var sortDescending by remember { mutableStateOf(true) }

    // Si on appuie sur le bouton "Retour" physique d'Android quand on est sur le détail
    BackHandler(enabled = selectedMeasurement != null) {
        selectedMeasurement = null
    }
    
    // Filter and sort measurements
    val filteredList = remember(historyList, searchQuery, sortBy, sortDescending) {
        var filtered = historyList
        
        // Search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { measurement ->
                measurement.weight.toString().contains(searchQuery) ||
                measurement.bodyFat.toString().contains(searchQuery) ||
                measurement.dateString().contains(searchQuery, ignoreCase = true) ||
                measurement.notes?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        // Sort
        filtered = when (sortBy) {
            "weight" -> if (sortDescending) filtered.sortedByDescending { it.weight } else filtered.sortedBy { it.weight }
            "bodyFat" -> if (sortDescending) filtered.sortedByDescending { it.bodyFat } else filtered.sortedBy { it.bodyFat }
            else -> if (sortDescending) filtered.sortedByDescending { it.timestamp } else filtered.sortedBy { it.timestamp }
        }
        
        filtered
    }

    if (selectedMeasurement != null) {
        // --- VUE DÉTAIL ---
        HistoryDetailScreen(
            measurement = selectedMeasurement!!,
            onBack = { selectedMeasurement = null },
            onDelete = {
                viewModel.deleteMeasurement(selectedMeasurement!!)
                selectedMeasurement = null // Retour liste après suppression
            },
            onEdit = { updatedMeasurement ->
                viewModel.updateMeasurement(updatedMeasurement)
                selectedMeasurement = updatedMeasurement // Update local state to show changes immediately
            }
        )
    } else {
        // --- VUE LISTE ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(Icons.Default.FilterList, "Filters")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search measurements...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Filters
            if (showFilters) {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sort by:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = sortBy == "date",
                                onClick = { sortBy = "date" },
                                label = { Text("Date") }
                            )
                            FilterChip(
                                selected = sortBy == "weight",
                                onClick = { sortBy = "weight" },
                                label = { Text("Weight") }
                            )
                            FilterChip(
                                selected = sortBy == "bodyFat",
                                onClick = { sortBy = "bodyFat" },
                                label = { Text("Body Fat") }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Descending", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = sortDescending,
                                onCheckedChange = { sortDescending = it }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No measurements yet" else "No results found",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { measurement ->
                        HistoryItem(
                            measurement = measurement,
                            onClick = { selectedMeasurement = measurement }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(measurement: Measurement, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${measurement.weight} kg",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = measurement.dateString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Petit résumé visuel
            Column(horizontalAlignment = Alignment.End) {
                if (measurement.bodyFat > 0) {
                    Text(
                        text = "${String.format("%.1f", measurement.bodyFat)}% Fat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}