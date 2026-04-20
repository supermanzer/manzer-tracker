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
        CoffeeBrew::class,
        Exercise::class,
        WorkoutPlan::class,
        WorkoutPlanExercise::class,
        WorkoutSession::class,
        WorkoutSet::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coffeeDao(): CoffeeDao
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // Version 3 → 4: switched TypeConverters from java.util.Date to java.time.Instant /
        // LocalDate. Underlying column type is unchanged (INTEGER epoch-millis), so no DDL needed.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "manzer_tracker_db")
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
