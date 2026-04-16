package com.supermanzer.manzertracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supermanzer.manzertracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FitnessViewModel(private val fitnessDao: FitnessDao) : ViewModel() {

    val allExercises: StateFlow<List<Exercise>> = fitnessDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkoutPlans: StateFlow<List<WorkoutPlan>> = fitnessDao.getAllWorkoutPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<WorkoutSession>> = fitnessDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            fitnessDao.insertExercise(exercise)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            fitnessDao.updateExercise(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            fitnessDao.deleteExercise(exercise)
        }
    }

    fun addWorkoutPlan(plan: WorkoutPlan, exercises: List<Exercise>) {
        viewModelScope.launch {
            val planId = fitnessDao.insertWorkoutPlan(plan)
            exercises.forEachIndexed { index, exercise ->
                fitnessDao.insertPlanExercise(
                    WorkoutPlanExercise(
                        planId = planId,
                        exerciseId = exercise.id,
                        orderIndex = index
                    )
                )
            }
        }
    }

    fun updateWorkoutPlan(plan: WorkoutPlan, exercises: List<Exercise>) {
        viewModelScope.launch {
            fitnessDao.updateWorkoutPlanWithExercises(plan, exercises)
        }
    }

    fun getExercisesForPlan(planId: Long): Flow<List<Exercise>> =
        fitnessDao.getExercisesForPlan(planId)

    fun deleteWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            fitnessDao.deleteWorkoutPlan(plan)
        }
    }

    fun addSession(session: WorkoutSession, sets: List<WorkoutSet>) {
        viewModelScope.launch {
            val sessionId = fitnessDao.insertSession(session)
            sets.forEach { set ->
                fitnessDao.insertSet(set.copy(sessionId = sessionId))
            }
        }
    }

    fun updateSession(session: WorkoutSession, sets: List<WorkoutSet>) {
        viewModelScope.launch {
            fitnessDao.updateSessionWithSets(session, sets)
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            fitnessDao.deleteSession(session)
        }
    }

    fun addSet(set: WorkoutSet) {
        viewModelScope.launch {
            fitnessDao.insertSet(set)
        }
    }

    fun updateSet(set: WorkoutSet) {
        viewModelScope.launch {
            fitnessDao.updateSet(set)
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            fitnessDao.deleteSet(set)
        }
    }

    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>> =
        fitnessDao.getSetsForSession(sessionId)
}

class FitnessViewModelFactory(private val fitnessDao: FitnessDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitnessViewModel(fitnessDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
