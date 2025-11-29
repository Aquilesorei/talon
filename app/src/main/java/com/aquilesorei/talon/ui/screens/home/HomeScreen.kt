package com.aquilesorei.talon.ui.screens.home

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquilesorei.talon.viewmodels.ScaleViewModel
import com.aquilesorei.talon.viewmodels.BluetoothDeviceUi

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current

    // Initialisation du ViewModel
    val viewModel: ScaleViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScaleViewModel(context) as T
            }
        }
    )

    val state by viewModel.uiState.collectAsState()

    // État pour la boîte de dialogue "GPS requis"
    var showLocationDialog by remember { mutableStateOf(false) }

    // --- 1. Launcher: Activer Bluetooth ---
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (!viewModel.isLocationEnabled()) {
                showLocationDialog = true
            } else {
                viewModel.startDiscovery()
            }
        }
    }

    // --- 2. Launcher: Permissions ---
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            if (!viewModel.isBluetoothEnabled()) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else if (!viewModel.isLocationEnabled()) {
                showLocationDialog = true
            } else {
                viewModel.startDiscovery()
            }
        }
    }

    // --- ALERTE GPS ---
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("Location Required") },
            text = { Text("To scan for Bluetooth devices, Android requires Location (GPS) to be enabled. Please turn it on.") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOGUE SÉLECTION APPAREIL ---
    if (state.isDeviceListOpen) {
        DeviceSelectionDialog(
            devices = state.scannedDevices,
            onDismissRequest = { viewModel.dismissDialog() },
            onDeviceSelected = { device -> viewModel.selectDevice(device) }
        )
    }

    // --- ÉCRAN PRINCIPAL ---
    Box(modifier = Modifier.fillMaxSize()) {
        // Menu Button
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

        // --- SECTION POIDS ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "WEIGHT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.2f", state.weight),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- GRILLE DES MÉTRIQUES (NOUVEAU) ---
        // Affiche les résultats calculés par BodyMetricsCalculator
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoCard(
                    label = "Body Fat",
                    value = if (state.bodyFat > 0) String.format("%.1f %%", state.bodyFat) else "-- %",
                    icon = Icons.Default.Opacity // Icône goutte/gras
                )
                InfoCard(
                    label = "Water",
                    value = if (state.water > 0) String.format("%.1f %%", state.water) else "-- %",
                    icon = Icons.Default.WaterDrop
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoCard(
                    label = "Muscle",
                    value = if (state.muscle > 0) String.format("%.1f kg", state.muscle) else "-- kg",
                    icon = Icons.Default.FitnessCenter
                )
                InfoCard(
                    label = "Impedance",
                    value = if (state.impedance > 0) "${state.impedance.toInt()} Ω" else "-- Ω",
                    icon = Icons.Default.ElectricBolt
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- ZONE DE SCAN / TIMER ---
        val isMeasuring = state.isScanning && !state.isDeviceListOpen && state.targetAddress != null

        AnimatedVisibility(
            visible = isMeasuring,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Step on the scale now...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { state.timeRemaining / 60f },
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = "${state.timeRemaining}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Espace vide pour éviter le saut d'interface
        if (!isMeasuring) {
            Spacer(modifier = Modifier.height(106.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOUTON SCAN ---
        LargeFloatingActionButton(
            onClick = {
                if (state.isScanning) {
                    viewModel.stopScan()
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            },
            containerColor = if (state.isScanning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (state.isScanning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = if (state.isScanning) Icons.Default.Stop else Icons.Default.BluetoothSearching,
                contentDescription = if (state.isScanning) "Stop Scan" else "Start Scan",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
    }
}

// --- COMPOSANTS UI ---

@Composable
fun InfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(160.dp)
            .height(85.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDeviceUi>,
    onDismissRequest: () -> Unit,
    onDeviceSelected: (BluetoothDeviceUi) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Device",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp, start = 8.dp)
                )

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Scanning nearby...", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn {
                        items(devices) { device ->
                            DeviceItem(device = device, onClick = { onDeviceSelected(device) })
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: BluetoothDeviceUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name.ifEmpty { "Unknown Device" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
            Text(
                text = "${device.rssi}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}