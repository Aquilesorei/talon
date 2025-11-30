package com.aquilesorei.talon.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.aquilesorei.talon.data.local.database.TalonDatabase
import com.aquilesorei.talon.data.repository.UserProfileRepository
import com.aquilesorei.talon.data.repository.MeasurementRepository
import com.aquilesorei.talon.data.repository.UserPreferencesRepository
import com.aquilesorei.talon.domain.models.BodyMetricsCalculator

data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val rssi: Int
)

data class MeasurementPacket(
    val weight: Float,
    val impedance: Float,
    val rawBytes: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScaleUiState(
    val weight: Float = 0f,
    val impedance: Float = 0f, // 0 = Inconnu/Instable (affichera "--")
    val rawData: String = "",
    val bodyFat: Float = 0f,
    val water: Float = 0f,
    val muscle: Float = 0f,
    val isScanning: Boolean = false,
    val isDeviceListOpen: Boolean = false,
    val scannedDevices: List<BluetoothDeviceUi> = emptyList(),
    val targetAddress: String? = null,
    val timeRemaining: Int = 60
)

class ScaleViewModel(private val context: Context) : ViewModel() {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scanner = bluetoothManager.adapter.bluetoothLeScanner

    private val database = TalonDatabase.getDatabase(context)
    private val userRepository = UserProfileRepository(database.userProfileDao())
    private val measurementRepository = MeasurementRepository(context)
    private val preferencesRepository = UserPreferencesRepository(database.userPreferencesDao(), context)

    private val _uiState = MutableStateFlow(ScaleUiState())
    val uiState = _uiState.asStateFlow()
    
    // Expose preferences for auto-connect
    val preferences = preferencesRepository.preferences

    private var scanTimeoutJob: Job? = null
    private val SCAN_DURATION_SECONDS = 60

    // --- LE VERROU (C'est ça qui manquait !) ---
    private var isCalculationDone = false
    private var lastSavedTimestamp: Long = 0

    // --- PACKET COLLECTION ---
    private val collectedPackets = mutableListOf<MeasurementPacket>()
    private var packetDebounceJob: Job? = null
    private val PACKET_TIMEOUT_MS = 2000L // 2 seconds without packets = sender stopped


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // 🔒 SÉCURITÉ : Si c'est fini, on ferme la porte !
            if (isCalculationDone) return

            result?.device?.let { device ->
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address
                val rssi = result.rssi

                // 1. Mode Liste
                if (_uiState.value.isDeviceListOpen) {
                    val newDevice = BluetoothDeviceUi(deviceName, deviceAddress, rssi)
                    _uiState.update { currentState ->
                        if (currentState.scannedDevices.none { it.address == deviceAddress }) {
                            currentState.copy(scannedDevices = currentState.scannedDevices + newDevice)
                        } else {
                            currentState
                        }
                    }
                }

                // 2. Mode Mesure
                if (_uiState.value.targetAddress == deviceAddress) {
                    val manufacturerData = result.scanRecord?.manufacturerSpecificData
                    if (manufacturerData != null && manufacturerData.size() > 0) {
                        try {
                            val key = manufacturerData.keyAt(0)
                            val bytes = manufacturerData.get(key)

                            if (bytes != null && bytes.size >= 4) {
                                val (weight, rawImpedance) = decodeBytes(bytes)
                                
                                // Collect ALL packets (including 0 and 500)
                                val packet = MeasurementPacket(weight, rawImpedance, bytes)
                                collectedPackets.add(packet)
                                
                                // Update UI with current reading
                                _uiState.update {
                                    it.copy(
                                        weight = weight,
                                        impedance = rawImpedance,
                                        rawData = bytes.joinToString("") { b -> "%02X".format(b) }
                                    )
                                }
                                
                                // Reset debounce timer - restart waiting for packets to stop
                                startPacketDebounce()
                            }
                        } catch (e: Exception) {
                            Log.e("Talon", "Error: ${e.message}")
                        }
                    }
                }

            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("Talon", "Scan failed: $errorCode")
            stopScan()
        }
    }

    // --- LE "FINISSEUR" (Mise à jour Atomique) ---
    @SuppressLint("MissingPermission")
    private fun saveMeasurementAndStop(weight: Float, impedance: Float, rawBytes: ByteArray) {
        val now = System.currentTimeMillis()
        if (now - lastSavedTimestamp < 5000) return
        lastSavedTimestamp = now

        viewModelScope.launch {
            val userProfile = userRepository.userProfile.first()
            val metrics = BodyMetricsCalculator.calculate(weight, impedance, userProfile)

            // MISE À JOUR FINALE DE L'ÉCRAN
            _uiState.update {
                it.copy(
                    weight = weight,
                    impedance = impedance,    // Affiche 551 Ω
                    bodyFat = metrics.bodyFatPercentage,
                    water = metrics.waterPercentage,
                    muscle = metrics.muscleMass,
                    isScanning = false,       // Arrête l'animation
                    rawData = rawBytes.joinToString("") { b -> "%02X".format(b) }
                )
            }

            try {
                scanTimeoutJob?.cancel()
                if (isBluetoothEnabled()) scanner?.stopScan(scanCallback)
            } catch (e: Exception) { Log.e("Talon", "Stop error: ${e.message}") }

            measurementRepository.saveMeasurement(
                weight = weight,
                fat = metrics.bodyFatPercentage,
                water = metrics.waterPercentage,
                muscle = metrics.muscleMass,
                imp = impedance,
                boneMass = metrics.boneMass,
                metabolicAge = metrics.metabolicAge,
                bmr = metrics.bmr,
                bmi = metrics.bmi
            )
            Log.d("TalonDB", "✅ SUCCÈS : ${metrics.bodyFatPercentage}% Fat ($impedance Ω)")
        }
    }

    // --- DEBOUNCE: Detect when packets stop arriving ---
    private fun startPacketDebounce() {
        packetDebounceJob?.cancel()
        packetDebounceJob = viewModelScope.launch {
            delay(PACKET_TIMEOUT_MS)
            // No new packets for 2 seconds - sender stopped!
            onPacketsComplete()
        }
    }

    // --- SELECTION: Pick the best packet ---
    private fun onPacketsComplete() {
        if (isCalculationDone) return
        isCalculationDone = true

        if (collectedPackets.isEmpty()) {
            Log.w("Talon", "No packets collected")
            stopScan()
            return
        }

        Log.d("Talon", "Collected ${collectedPackets.size} packets, selecting best...")

        // Filter for GOOD impedance values (not 0 and not 500)
        val goodPackets = collectedPackets.filter { 
            it.impedance > 0f && it.impedance != 500.0f 
        }

        val selectedPacket = if (goodPackets.isNotEmpty()) {
            // Pick the most common good impedance value
            val mostCommon = goodPackets
                .groupBy { it.impedance }
                .maxByOrNull { it.value.size }
                ?.value
                ?.first()
            
            Log.d("Talon", "✅ Found ${goodPackets.size} good packets, selected: ${mostCommon?.impedance}Ω")
            mostCommon!!
        } else {
            // Fallback: use the last packet (likely 0 or 500)
            val fallback = collectedPackets.last()
            Log.w("Talon", "⚠️ No good impedance found, falling back to: ${fallback.impedance}Ω")
            fallback
        }

        // Process the winner
        saveMeasurementAndStop(
            selectedPacket.weight,
            selectedPacket.impedance,
            selectedPacket.rawBytes
        )
    }

    private fun decodeBytes(bytes: ByteArray): Pair<Float, Float> {

        val weightMsb = bytes[0].toInt() and 0xFF
        val weightLsb = bytes[1].toInt() and 0xFF
        val weightRaw = (weightMsb shl 8) or weightLsb
        val weight = weightRaw / 100f

        val impMsb = bytes[2].toInt() and 0xFF
        val impLsb = bytes[3].toInt() and 0xFF
        val impRaw = (impMsb shl 8) or impLsb
        val impedance = impRaw / 10f

        return Pair(weight, impedance)
    }

    fun isBluetoothEnabled(): Boolean = bluetoothManager.adapter?.isEnabled == true

    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) { false }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (scanner == null) return
        try {
            isCalculationDone = false // 🔓 DÉVERROUILLAGE au démarrage
            collectedPackets.clear() // Clear previous packets
            packetDebounceJob?.cancel() // Cancel any pending debounce
            
            _uiState.update { it.copy(
                isScanning = true,
                isDeviceListOpen = true,
                scannedDevices = emptyList(),
                targetAddress = null
            )}
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e("Talon", "Start error: ${e.message}")
        }
    }


    @SuppressLint("MissingPermission")
    fun selectDevice(device: BluetoothDeviceUi) {
        collectedPackets.clear() // Start fresh for this device
        packetDebounceJob?.cancel()
        
        // Save device for auto-reconnect
        viewModelScope.launch {
            preferencesRepository.saveScaleDevice(device.address, device.name)
        }
        
        _uiState.update { it.copy(
            isDeviceListOpen = false,
            targetAddress = device.address,
            timeRemaining = SCAN_DURATION_SECONDS
        )}
        startTimeoutTimer()
    }


    @SuppressLint("MissingPermission")
    fun dismissDialog() {
        stopScan()
        _uiState.update { it.copy(isDeviceListOpen = false) }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            scanTimeoutJob?.cancel()
            scanTimeoutJob = null
            packetDebounceJob?.cancel() // Cancel packet processing
            if (isBluetoothEnabled()) scanner?.stopScan(scanCallback)
            _uiState.update { it.copy(isScanning = false) }
        } catch (e: Exception) {
            Log.e("Talon", "Stop error: ${e.message}")
        }
    }


    private fun startTimeoutTimer() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && _uiState.value.isScanning) {
                delay(1000L)
                _uiState.update { it.copy(timeRemaining = it.timeRemaining - 1) }
            }
            // 🛡️ BULLETPROOF: Force save if we have data, even if scale never went silent
            if (_uiState.value.isScanning) {
                if (collectedPackets.isNotEmpty() && !isCalculationDone) {
                    Log.d("Talon", "⏱️ Timeout reached, forcing packet processing...")
                    onPacketsComplete()
                } else {
                    stopScan()
                }
            }
        }
    }
    
    // --- AUTO-CONNECT FEATURE ---
    @SuppressLint("MissingPermission")
    fun autoConnectToSavedDevice(address: String) {
        if (scanner == null) return
        try {
            isCalculationDone = false
            collectedPackets.clear()
            packetDebounceJob?.cancel()
            
            _uiState.update { it.copy(
                isScanning = true,
                isDeviceListOpen = false,
                targetAddress = address,
                timeRemaining = SCAN_DURATION_SECONDS
            )}
            
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(null, settings, scanCallback)
            startTimeoutTimer()
            
            Log.d("Talon", "🔄 Auto-connecting to saved device: $address")
        } catch (e: Exception) {
            Log.e("Talon", "Auto-connect error: ${e.message}")
        }
    }
    
    fun forgetDevice() {
        viewModelScope.launch {
            preferencesRepository.forgetScaleDevice()
            _uiState.update { it.copy(targetAddress = null) }
            Log.d("Talon", "🗑️ Saved device forgotten")
        }
    }
}