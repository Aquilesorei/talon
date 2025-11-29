package com.aquilesorei.talon

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(), // Date et heure de la pesée
    val weight: Float,
    val bodyFat: Float,
    val water: Float,
    val muscle: Float,
    val impedance: Float,
    val boneMass: Float = 0f,
    val metabolicAge: Int = 0,
    val bmr: Int = 0, // Basal Metabolic Rate
    val bmi: Float = 0f,
    val notes: String? = null // Optional notes/tags
) {
    // Petit bonus : des fonctions pour afficher la date proprement dans l'UI plus tard
    fun dateString(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun timeString(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}