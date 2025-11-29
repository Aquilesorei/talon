package com.aquilesorei.talon

import androidx.room.TypeConverter

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
