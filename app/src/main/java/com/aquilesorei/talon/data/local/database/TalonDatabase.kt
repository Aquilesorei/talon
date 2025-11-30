package com.aquilesorei.talon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aquilesorei.talon.data.local.entities.Measurement
import com.aquilesorei.talon.data.local.entities.Goal
import com.aquilesorei.talon.data.local.entities.UserPreferences
import com.aquilesorei.talon.data.local.dao.MeasurementDao
import com.aquilesorei.talon.data.local.dao.GoalDao
import com.aquilesorei.talon.data.local.dao.UserPreferencesDao
import com.aquilesorei.talon.data.local.dao.UserProfileDao
import com.aquilesorei.talon.domain.models.UserProfile

@Database(
    entities = [Measurement::class, UserProfile::class, Goal::class, UserPreferences::class],
    version = 5,
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

        // Migration from version 3 to 4: Add saved scale device fields
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN savedScaleAddress TEXT")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN savedScaleName TEXT")
            }
        }

        // Migration from version 4 to 5: Add autoScanEnabled field
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN autoScanEnabled INTEGER NOT NULL DEFAULT 1")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Ensure default preferences exist on fresh install
                            db.execSQL("""
                                INSERT OR IGNORE INTO user_preferences VALUES (
                                    1, 'SYSTEM', 0, '09:00', 'DAILY', 1, 1, 1, 0, NULL, NULL, 1
                                )
                            """.trimIndent())
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}