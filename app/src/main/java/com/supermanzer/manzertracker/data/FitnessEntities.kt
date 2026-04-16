package com.supermanzer.manzertracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String? = null // e.g., "Strength", "Cardio"
)

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null
)

@Entity(
    tableName = "workout_plan_exercises",
    primaryKeys = ["planId", "exerciseId", "orderIndex"],
    foreignKeys = [
        ForeignKey(entity = WorkoutPlan::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("planId"), Index("exerciseId")]
)
data class WorkoutPlanExercise(
    val planId: Long,
    val exerciseId: Long,
    val orderIndex: Int // Position in the workout
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(entity = WorkoutPlan::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("planId")]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long? = null,
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val notes: String? = null
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double? = null // Rate of Perceived Exertion
)
