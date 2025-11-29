package com.aquilesorei.talon.data.local.database

import androidx.room.TypeConverter
import com.aquilesorei.talon.data.local.entities.GoalType
import com.aquilesorei.talon.data.local.entities.GoalStatus
import com.aquilesorei.talon.data.local.entities.ThemePreference
import com.aquilesorei.talon.data.local.entities.ReminderFrequency

class Converters {
    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name

    @TypeConverter
    fun toGoalType(value: String): GoalType = GoalType.valueOf(value)

    @TypeConverter
    fun fromGoalStatus(value: GoalStatus): String = value.name

    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)

    @TypeConverter
    fun fromThemePreference(value: ThemePreference): String = value.name

    @TypeConverter
    fun toThemePreference(value: String): ThemePreference = ThemePreference.valueOf(value)

    @TypeConverter
    fun fromReminderFrequency(value: ReminderFrequency): String = value.name

    @TypeConverter
    fun toReminderFrequency(value: String): ReminderFrequency = ReminderFrequency.valueOf(value)
}
