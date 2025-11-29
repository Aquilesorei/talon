package com.aquilesorei.talon.data.repository

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import java.util.Calendar
import com.aquilesorei.talon.data.local.dao.UserPreferencesDao
import com.aquilesorei.talon.data.local.entities.UserPreferences
import com.aquilesorei.talon.data.local.entities.ThemePreference
import com.aquilesorei.talon.workers.ReminderWorker

class UserPreferencesRepository(
    private val preferencesDao: UserPreferencesDao,
    private val context: Context
) {
    
    val preferences: Flow<UserPreferences> = preferencesDao.getPreferences()
        .map { it ?: UserPreferences() } // Return default if null
    
    suspend fun updateTheme(theme: ThemePreference) {
        preferencesDao.updateTheme(theme)
    }
    
    suspend fun updateReminderEnabled(enabled: Boolean) {
        preferencesDao.updateReminderEnabled(enabled)
        if (enabled) {
            scheduleReminder()
        } else {
            cancelReminder()
        }
    }
    
    suspend fun updatePreferences(preferences: UserPreferences) {
        preferencesDao.insert(preferences)
        if (preferences.reminderEnabled) {
            scheduleReminder()
        } else {
            cancelReminder()
        }
    }
    
    suspend fun completeOnboarding() {
        preferencesDao.updateOnboardingStatus(true)
    }
    
    private fun scheduleReminder() {
        val workManager = WorkManager.getInstance(context)
        
        // Schedule for 8:00 AM daily (simplified for now)
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 8)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("weigh_in_reminder")
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "weigh_in_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }
    
    private fun cancelReminder() {
        WorkManager.getInstance(context).cancelAllWorkByTag("weigh_in_reminder")
    }
}
