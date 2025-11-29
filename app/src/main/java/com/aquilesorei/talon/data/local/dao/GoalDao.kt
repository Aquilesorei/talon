package com.aquilesorei.talon.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.aquilesorei.talon.data.local.entities.Goal
import com.aquilesorei.talon.data.local.entities.GoalStatus

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): Goal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal): Long
    
    @Update
    suspend fun update(goal: Goal)
    
    @Delete
    suspend fun delete(goal: Goal)
    
    @Query("UPDATE goals SET status = :status, achievedAt = :achievedAt WHERE id = :goalId")
    suspend fun updateGoalStatus(goalId: Int, status: GoalStatus, achievedAt: Long?)
}
