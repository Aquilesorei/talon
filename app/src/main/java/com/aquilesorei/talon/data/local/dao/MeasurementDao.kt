package com.aquilesorei.talon.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.aquilesorei.talon.data.local.entities.Measurement

@Dao
interface MeasurementDao {
    // Récupère tout l'historique, du plus récent au plus ancien
    // Le type 'Flow' permet à l'écran Historique de se mettre à jour tout seul !
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Measurement>>

    @Insert
    suspend fun insert(measurement: Measurement)

    @androidx.room.Update
    suspend fun update(measurement: Measurement)

    @Delete
    suspend fun delete(measurement: Measurement)
}