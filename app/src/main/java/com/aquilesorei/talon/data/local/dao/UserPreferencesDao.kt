package com.aquilesorei.talon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.aquilesorei.talon.data.local.entities.UserPreferences
import com.aquilesorei.talon.data.local.entities.ThemePreference

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
    
    @Query("UPDATE user_preferences SET savedScaleAddress = :address, savedScaleName = :name WHERE id = 1")
    suspend fun saveScaleDevice(address: String, name: String)
    
    @Query("UPDATE user_preferences SET savedScaleAddress = NULL, savedScaleName = NULL WHERE id = 1")
    suspend fun clearScaleDevice()

    @Query("UPDATE user_preferences SET autoScanEnabled = :enabled WHERE id = 1")
    suspend fun updateAutoScanEnabled(enabled: Boolean)
}
