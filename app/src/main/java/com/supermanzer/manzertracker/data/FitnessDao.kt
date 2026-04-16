package com.supermanzer.manzertracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {
    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    // Workout Plans
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updateWorkoutPlan(plan: WorkoutPlan)

    @Delete
    suspend fun deleteWorkoutPlan(plan: WorkoutPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercise(planExercise: WorkoutPlanExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(planExercises: List<WorkoutPlanExercise>)

    @Update
    suspend fun updatePlanExercise(planExercise: WorkoutPlanExercise)

    @Delete
    suspend fun deletePlanExercise(planExercise: WorkoutPlanExercise)

    @Delete
    suspend fun deletePlanExercises(planExercises: List<WorkoutPlanExercise>)

    @Query("SELECT * FROM workout_plans ORDER BY name ASC")
    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>>

    @Query("""
        SELECT e.* FROM exercises e
        JOIN workout_plan_exercises wpe ON e.id = wpe.exerciseId
        WHERE wpe.planId = :planId
        ORDER BY wpe.orderIndex ASC
    """)
    fun getExercisesForPlan(planId: Long): Flow<List<Exercise>>

    @Query("SELECT * FROM workout_plan_exercises WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getPlanExercisesSync(planId: Long): List<WorkoutPlanExercise>

    @Query("DELETE FROM workout_plan_exercises WHERE planId = :planId")
    suspend fun deleteExercisesForPlan(planId: Long)

    // Workout Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    // Workout Sets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Update
    suspend fun updateSet(set: WorkoutSet)

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    @Delete
    suspend fun deleteSets(sets: List<WorkoutSet>)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSet>

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Transaction
    suspend fun updateWorkoutPlanWithExercises(plan: WorkoutPlan, exercises: List<Exercise>) {
        updateWorkoutPlan(plan)
        val currentEntries = getPlanExercisesSync(plan.id)
        val newEntries = exercises.mapIndexed { index, exercise ->
            WorkoutPlanExercise(plan.id, exercise.id, index)
        }

        // Surgical update: delete removed, insert new/changed
        val toDelete = currentEntries.filterNot { ce -> 
            newEntries.any { ne -> ne.exerciseId == ce.exerciseId && ne.orderIndex == ce.orderIndex } 
        }
        val toInsert = newEntries.filterNot { ne -> 
            currentEntries.any { ce -> ce.exerciseId == ne.exerciseId && ce.orderIndex == ne.orderIndex } 
        }

        if (toDelete.isNotEmpty()) deletePlanExercises(toDelete)
        if (toInsert.isNotEmpty()) insertPlanExercises(toInsert)
    }

    @Transaction
    suspend fun updateSessionWithSets(session: WorkoutSession, sets: List<WorkoutSet>) {
        updateSession(session)
        val currentSets = getSetsForSessionSync(session.id)
        val newSets = sets.map { it.copy(sessionId = session.id) }

        val toDelete = currentSets.filterNot { cs -> 
            newSets.any { ns -> ns.id == cs.id && ns.id != 0L } 
        }
        val toInsertOrUpdate = newSets

        if (toDelete.isNotEmpty()) deleteSets(toDelete)
        insertSets(toInsertOrUpdate)
    }
}
