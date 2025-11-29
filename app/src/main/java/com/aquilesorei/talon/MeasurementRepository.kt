package com.aquilesorei.talon

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(context: Context) {
    private val dao = TalonDatabase.getDatabase(context).measurementDao()

    // On expose la liste des mesures pour l'UI
    val allMeasurements: Flow<List<Measurement>> = dao.getAll()

    // Fonction pour sauvegarder une nouvelle pesée
    suspend fun saveMeasurement(
        weight: Float,
        fat: Float,
        water: Float,
        muscle: Float,
        imp: Float,
        boneMass: Float = 0f,
        metabolicAge: Int = 0,
        bmr: Int = 0,
        bmi: Float = 0f,
        notes: String? = null
    ) {
        val measurement = Measurement(
            weight = weight,
            bodyFat = fat,
            water = water,
            muscle = muscle,
            impedance = imp,
            boneMass = boneMass,
            metabolicAge = metabolicAge,
            bmr = bmr,
            bmi = bmi,
            notes = notes
        )
        dao.insert(measurement)
    }

    // Fonction pour mettre à jour une pesée
    suspend fun update(measurement: Measurement) {
        dao.update(measurement)
    }

    // Fonction pour supprimer une erreur de pesée
    suspend fun delete(measurement: Measurement) {
        dao.delete(measurement)
    }
}