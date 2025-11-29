package com.aquilesorei.talon.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aquilesorei.talon.data.local.dao.UserProfileDao
import com.aquilesorei.talon.domain.models.UserProfile

class UserProfileRepository(private val userProfileDao: UserProfileDao) {

    // Lire les données sous forme de flux (Flow)
    // Si aucun profil n'existe, retourner des valeurs par défaut
    val userProfile: Flow<UserProfile> = userProfileDao.getProfile()
        .map { profile ->
            profile ?: UserProfile(
                id = 1,
                heightCm = 175, // Valeur par défaut
                age = 25,
                isMale = true,
                isAthlete = false
            )
        }

    // Sauvegarder les données
    suspend fun saveProfile(height: Int, age: Int, isMale: Boolean, isAthlete: Boolean) {
        val profile = UserProfile(
            id = 1, // Always 1 since there's only one user profile
            heightCm = height,
            age = age,
            isMale = isMale,
            isAthlete = isAthlete
        )
        userProfileDao.insertOrUpdate(profile)
    }
}