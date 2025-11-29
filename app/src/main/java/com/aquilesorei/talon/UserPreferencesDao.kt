package com.aquilesorei.talon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: UserPreferences)
    
    @Update
    suspend fun update(preferences: UserPreferences)
    
    @Query("UPDATE user_preferences SET theme = :theme WHERE id = 1")
    suspend fun updateTheme(theme: ThemePreference)
    
    @Query("UPDATE user_preferences SET reminderEnabled = :enabled WHERE id = 1")
    suspend fun updateReminderEnabled(enabled: Boolean)
    
    @Query("UPDATE user_preferences SET hasCompletedOnboarding = :completed WHERE id = 1")
    suspend fun updateOnboardingStatus(completed: Boolean)
}
