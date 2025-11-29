package com.aquilesorei.talon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.aquilesorei.talon.domain.models.UserProfile

@Dao
interface UserProfileDao {
    // Get the user profile (there's only one, with id = 1)
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    // Insert or update the profile (REPLACE strategy handles both cases)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    // Delete all profiles (useful for testing/reset)
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
