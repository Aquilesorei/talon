package com.aquilesorei.talon

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Measurement::class, UserProfile::class, Goal::class, UserPreferences::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TalonDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: TalonDatabase? = null

        // Migration from version 1 to 2: Add user_profile table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        heightCm INTEGER NOT NULL,
                        age INTEGER NOT NULL,
                        isMale INTEGER NOT NULL,
                        isAthlete INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 2 to 3: Add goals, user_preferences, and update measurements
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to measurements table
                db.execSQL("ALTER TABLE measurements ADD COLUMN boneMass REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE measurements ADD COLUMN metabolicAge INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE measurements ADD COLUMN bmr INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE measurements ADD COLUMN bmi REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE measurements ADD COLUMN notes TEXT")

                // Create goals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        targetValue REAL NOT NULL,
                        startValue REAL NOT NULL,
                        deadline INTEGER,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        achievedAt INTEGER
                    )
                """.trimIndent())

                // Create user_preferences table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        id INTEGER PRIMARY KEY NOT NULL,
                        theme TEXT NOT NULL,
                        reminderEnabled INTEGER NOT NULL,
                        reminderTime TEXT NOT NULL,
                        reminderFrequency TEXT NOT NULL,
                        notificationsEnabled INTEGER NOT NULL,
                        achievementNotifications INTEGER NOT NULL,
                        goalProgressNotifications INTEGER NOT NULL,
                        hasCompletedOnboarding INTEGER NOT NULL
                    )
                """.trimIndent())

                // Insert default preferences
                db.execSQL("""
                    INSERT INTO user_preferences VALUES (
                        1, 'SYSTEM', 0, '09:00', 'DAILY', 1, 1, 1, 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): TalonDatabase {
            // Pattern Singleton pour n'avoir qu'une seule instance de la BDD ouverte
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TalonDatabase::class.java,
                    "talon_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}