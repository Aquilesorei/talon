package com.aquilesorei.talon

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

// Modèle pour la liste de découverte
data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val rssi: Int
)

// État de l'interface (UI State)
data class ScaleUiState(
    // Données brutes
    val weight: Float = 0f,
    val impedance: Float = 0f,
    val rawData: String = "",

    // Données calculées (Body Metrics)
    val bodyFat: Float = 0f,
    val water: Float = 0f,
    val muscle: Float = 0f,

    // États de l'application
    val isScanning: Boolean = false,
    val isDeviceListOpen: Boolean = false,
    val scannedDevices: List<BluetoothDeviceUi> = emptyList(),
    val targetAddress: String? = null,
    val timeRemaining: Int = 60
)

class ScaleViewModel(private val context: Context) : ViewModel() {

    // --- Services Système ---
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scanner = bluetoothManager.adapter.bluetoothLeScanner

    // --- Repositories (Données) ---
    private val database = TalonDatabase.getDatabase(context)
    private val userRepository = UserProfileRepository(database.userProfileDao())
    private val measurementRepository = MeasurementRepository(context)

    // --- État & Timer ---
    private val _uiState = MutableStateFlow(ScaleUiState())
    val uiState = _uiState.asStateFlow()

    private var scanTimeoutJob: Job? = null
    private val SCAN_DURATION_SECONDS = 60

    // Variable pour éviter d'enregistrer plusieurs fois la même pesée en une fraction de seconde
    private var lastSavedTimestamp: Long = 0

    // --- CALLBACK BLUETOOTH (Le cœur de l'écoute) ---
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address
                val rssi = result.rssi

                // 1. Mode DÉCOUVERTE : On remplit la liste pour la boîte de dialogue
                if (_uiState.value.isDeviceListOpen) {
                    val newDevice = BluetoothDeviceUi(deviceName, deviceAddress, rssi)
                    _uiState.update { currentState ->
                        // On évite les doublons visuels dans la liste
                        if (currentState.scannedDevices.none { it.address == deviceAddress }) {
                            currentState.copy(scannedDevices = currentState.scannedDevices + newDevice)
                        } else {
                            currentState
                        }
                    }
                }

                // 2. Mode MESURE : On cible la balance sélectionnée
                if (_uiState.value.targetAddress == deviceAddress) {
                    val manufacturerData = result.scanRecord?.manufacturerSpecificData
                    if (manufacturerData != null && manufacturerData.size() > 0) {
                        try {
                            // On prend le premier bloc de données (souvent ID 0x0DC0 ou similaire)
                            val key = manufacturerData.keyAt(0)
                            val bytes = manufacturerData.get(key)

                            // Si le paquet ressemble à une pesée (au moins 4 octets pour Poids+Impédance)
                            if (bytes != null && bytes.size >= 4) {
                                // A. Mettre à jour l'UI en temps réel (pour voir les chiffres bouger)
                                parseDataForUi(bytes)

                                // B. Sauvegarder et Arrêter (Succès)
                                saveMeasurementAndStop(bytes)
                            }
                        } catch (e: Exception) {
                            Log.e("Talon", "Error parsing data: ${e.message}")
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

    // --- PARSING & CALCULS ---

    // Juste pour l'affichage (ne sauvegarde pas)
    private fun parseDataForUi(bytes: ByteArray) {
        val (weight, impedance) = decodeBytes(bytes)

        _uiState.update {
            it.copy(
                weight = weight,
                impedance = impedance,
                rawData = bytes.joinToString("") { byte -> "%02X".format(byte) }
            )
        }
    }

    // Fonction utilitaire pour décoder le binaire
    private fun decodeBytes(bytes: ByteArray): Pair<Float, Float> {
        // Poids (Big Endian)
        val weightMsb = bytes[0].toInt() and 0xFF
        val weightLsb = bytes[1].toInt() and 0xFF
        val weightRaw = (weightMsb shl 8) or weightLsb
        val weight = weightRaw / 100f

        // Impédance (Big Endian)
        val impMsb = bytes[2].toInt() and 0xFF
        val impLsb = bytes[3].toInt() and 0xFF
        val impRaw = (impMsb shl 8) or impLsb
        val impedance = impRaw / 10f

        return Pair(weight, impedance)
    }

    // C'est ici que la magie opère : Calcul + Sauvegarde BDD + Arrêt
    private fun saveMeasurementAndStop(bytes: ByteArray) {
        val now = System.currentTimeMillis()
        // Anti-rebond : on ignore si on a déjà sauvegardé il y a moins de 5 secondes
        if (now - lastSavedTimestamp < 5000) return

        lastSavedTimestamp = now
        val (weight, impedance) = decodeBytes(bytes)

        // On lance une tâche de fond
        viewModelScope.launch {
            // 1. On récupère le profil utilisateur (taille, âge, sexe)
            // .first() prend la valeur actuelle et se déconnecte du flux
            val userProfile = userRepository.userProfile.first()

            // 2. On calcule la graisse, l'eau, etc. grâce à ton algorithme
            val metrics = BodyMetricsCalculator.calculate(weight, impedance, userProfile)

            // 3. On met à jour l'UI avec les résultats finaux calculés
            _uiState.update {
                it.copy(
                    weight = weight,
                    impedance = impedance,
                    bodyFat = metrics.bodyFatPercentage,
                    water = metrics.waterPercentage,
                    muscle = metrics.muscleMass
                )
            }

            // 4. On sauvegarde dans la Base de Données Room
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
            Log.d("TalonDB", "✅ Mesure sauvegardée : ${metrics.bodyFatPercentage}% Fat")

            // 5. On coupe le Bluetooth scanner (Mission accomplie)
            stopScan()
        }
    }

    // --- GESTION DU MATÉRIEL (Permissions & État) ---

    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.adapter?.isEnabled == true
    }

    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    // --- ACTIONS UTILISATEUR ---

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (scanner == null) return

        try {
            // Reset de l'état pour une nouvelle recherche
            _uiState.update { it.copy(
                isScanning = true,
                isDeviceListOpen = true,
                scannedDevices = emptyList(),
                targetAddress = null
            )}

            // Scan Low Latency pour trouver vite
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, scanCallback)

        } catch (e: Exception) {
            Log.e("Talon", "Start discovery error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun selectDevice(device: BluetoothDeviceUi) {
        // L'utilisateur a choisi sa balance
        // On ferme la liste, on enregistre l'adresse cible, et on lance le chrono
        _uiState.update { it.copy(
            isDeviceListOpen = false,
            targetAddress = device.address,
            timeRemaining = SCAN_DURATION_SECONDS
        )}
        startTimeoutTimer()
    }

    @SuppressLint("MissingPermission")
    fun dismissDialog() {
        // Annulation : on arrête tout
        stopScan()
        _uiState.update { it.copy(isDeviceListOpen = false) }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            scanTimeoutJob?.cancel()
            scanTimeoutJob = null

            // Scanner peut être null si le BT a été coupé entre temps
            if (isBluetoothEnabled()) {
                scanner?.stopScan(scanCallback)
            }

            _uiState.update { it.copy(isScanning = false) }
        } catch (e: Exception) {
            Log.e("Talon", "Stop scan error: ${e.message}")
        }
    }

    private fun startTimeoutTimer() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && _uiState.value.isScanning) {
                delay(1000L)
                _uiState.update { it.copy(timeRemaining = it.timeRemaining - 1) }
            }
            // Si le temps est écoulé sans résultat, on arrête
            if (_uiState.value.isScanning) {
                stopScan()
            }
        }
    }
}