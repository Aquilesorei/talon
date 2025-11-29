package com.aquilesorei.talon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalStatus {
    ACTIVE,
    ACHIEVED,
    ABANDONED
}

enum class GoalType {
    WEIGHT,
    BODY_FAT,
    MUSCLE_MASS
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: GoalType,
    val targetValue: Float, // Target weight (kg) or body fat (%)
    val startValue: Float, // Starting value when goal was created
    val deadline: Long? = null, // Optional deadline timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val status: GoalStatus = GoalStatus.ACTIVE,
    val achievedAt: Long? = null // Timestamp when goal was achieved
) {
    fun calculateProgress(currentValue: Float): Float {
        val totalChange = targetValue - startValue
        if (totalChange == 0f) return 100f
        
        val currentChange = currentValue - startValue
        return ((currentChange / totalChange) * 100f).coerceIn(0f, 100f)
    }
    
    fun isAchieved(currentValue: Float): Boolean {
        return when (type) {
            GoalType.WEIGHT -> {
                if (targetValue < startValue) currentValue <= targetValue
                else currentValue >= targetValue
            }
            GoalType.BODY_FAT -> currentValue <= targetValue
            GoalType.MUSCLE_MASS -> currentValue >= targetValue
        }
    }
}
