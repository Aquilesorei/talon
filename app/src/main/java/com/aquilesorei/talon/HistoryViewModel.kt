package com.aquilesorei.talon

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(context: Context) : ViewModel() {

    private val repository = MeasurementRepository(context)

    // Le flux de données : se met à jour automatiquement quand la BDD change !
    val historyList: StateFlow<List<Measurement>> = repository.allMeasurements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            repository.delete(measurement)
        }
    }

    fun updateMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            repository.update(measurement)
        }
    }
}

// Factory pour créer le ViewModel avec le Context (comme pour ScaleViewModel)
class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}