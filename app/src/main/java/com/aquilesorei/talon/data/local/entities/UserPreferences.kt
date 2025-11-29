package com.aquilesorei.talon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

enum class ReminderFrequency {
    DAILY,
    WEEKLY,
    NEVER
}

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // Single row for preferences
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "09:00", // HH:mm format
    val reminderFrequency: ReminderFrequency = ReminderFrequency.DAILY,
    val notificationsEnabled: Boolean = true,
    val achievementNotifications: Boolean = true,
    val goalProgressNotifications: Boolean = true,
    val hasCompletedOnboarding: Boolean = false
)
