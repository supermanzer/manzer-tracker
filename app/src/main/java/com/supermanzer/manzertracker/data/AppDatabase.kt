package com.supermanzer.manzertracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Roaster::class,
        CoffeeBag::class,
        CoffeeBrew::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coffeeDao(): CoffeeDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // Version 3 → 4: switched TypeConverters from java.util.Date to java.time.Instant /
        // LocalDate. Underlying column type is unchanged (INTEGER epoch-millis), so no DDL needed.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        // Version 4 → 5: fitness tracking removed (app rebranded to BrewBuddy). Dropped
        // children-first so foreign key constraints are never violated. Coffee tables untouched.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS workout_sets")
                db.execSQL("DROP TABLE IF EXISTS workout_plan_exercises")
                db.execSQL("DROP TABLE IF EXISTS workout_sessions")
                db.execSQL("DROP TABLE IF EXISTS workout_plans")
                db.execSQL("DROP TABLE IF EXISTS exercises")
            }
        }

        // Version 5 → 6: brews gain a nullable "ideas for next brew" note. Existing rows get NULL.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coffee_brews ADD COLUMN nextBrewIdeas TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                // File name predates the BrewBuddy rebrand; renaming it would orphan existing data.
                Room.databaseBuilder(context, AppDatabase::class.java, "manzer_tracker_db")
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
