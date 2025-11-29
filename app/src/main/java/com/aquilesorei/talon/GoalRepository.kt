package com.aquilesorei.talon

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {
    
    val activeGoals: Flow<List<Goal>> = goalDao.getActiveGoals()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    
    suspend fun createGoal(goal: Goal): Long {
        return goalDao.insert(goal)
    }
    
    suspend fun updateGoal(goal: Goal) {
        goalDao.update(goal)
    }
    
    suspend fun deleteGoal(goal: Goal) {
        goalDao.delete(goal)
    }
    
    suspend fun achieveGoal(goalId: Int) {
        goalDao.updateGoalStatus(goalId, GoalStatus.ACHIEVED, System.currentTimeMillis())
    }
    
    suspend fun abandonGoal(goalId: Int) {
        goalDao.updateGoalStatus(goalId, GoalStatus.ABANDONED, null)
    }
    
    suspend fun checkAndUpdateGoals(currentWeight: Float, currentBodyFat: Float, currentMuscle: Float) {
        val goals = goalDao.getAllGoals()
        // This would need to be called in a coroutine scope
        // For now, just a placeholder for the logic
    }
}
