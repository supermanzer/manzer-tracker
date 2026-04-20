package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import com.supermanzer.manzertracker.ui.theme.FitnessDark1
import com.supermanzer.manzertracker.ui.theme.FitnessDark2
import com.supermanzer.manzertracker.ui.theme.FitnessDark3
import com.supermanzer.manzertracker.ui.theme.FitnessLight1
import com.supermanzer.manzertracker.ui.theme.FitnessLight2
import com.supermanzer.manzertracker.ui.theme.ManzerTrackerTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supermanzer.manzertracker.ManzerTrackerApplication
import com.supermanzer.manzertracker.data.Exercise
import com.supermanzer.manzertracker.data.WorkoutPlan
import com.supermanzer.manzertracker.data.WorkoutSession
import com.supermanzer.manzertracker.ui.viewmodels.FitnessViewModel
import com.supermanzer.manzertracker.ui.viewmodels.FitnessViewModelFactory

enum class FitnessFormType {
    NONE, SESSION, PLAN, EXERCISE, SESSION_DETAIL, EDIT_SESSION, PLAN_DETAIL, EDIT_PLAN, EDIT_EXERCISE
}

enum class FitnessTab {
    SESSIONS, PLANS, EXERCISES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen() {
    val context = LocalContext.current
    val database = (context.applicationContext as ManzerTrackerApplication).database
    val viewModel: FitnessViewModel = viewModel(
        factory = FitnessViewModelFactory(database.fitnessDao())
    )

    val sessions by viewModel.allSessions.collectAsState()
    val plans by viewModel.allWorkoutPlans.collectAsState()
    val exercises by viewModel.allExercises.collectAsState()

    var activeForm by remember { mutableStateOf(FitnessFormType.NONE) }
    var selectedSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var selectedPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    
    var selectedTab by remember { mutableStateOf(FitnessTab.SESSIONS) }
    
    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val darkTheme = isSystemInDarkTheme()
    val fitnessGradient = if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(FitnessDark1, FitnessDark2, FitnessDark3)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(FitnessLight1, FitnessLight2)
        )
    }

    ManzerTrackerTheme(isFitness = true) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    activeForm = when (selectedTab) {
                        FitnessTab.SESSIONS -> FitnessFormType.SESSION
                        FitnessTab.PLANS -> FitnessFormType.PLAN
                        FitnessTab.EXERCISES -> FitnessFormType.EXERCISE
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fitnessGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal])
                            )
                        }
                    }
                ) {
                    FitnessTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        FitnessTab.SESSIONS -> {
                            Text(text = "Recent Workouts", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sessions, key = { it.id }) { session ->
                                    val plan = plans.find { it.id == session.planId }
                                    WorkoutSessionItem(
                                        session = session,
                                        plan = plan,
                                        onClick = {
                                            selectedSession = session
                                            activeForm = FitnessFormType.SESSION_DETAIL
                                        }
                                    )
                                }
                            }
                        }
                        FitnessTab.PLANS -> {
                            Text(text = "Workout Plans", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(plans, key = { it.id }) { plan ->
                                    WorkoutPlanItem(
                                        plan = plan,
                                        onClick = {
                                            selectedPlan = plan
                                            activeForm = FitnessFormType.PLAN_DETAIL
                                        }
                                    )
                                }
                            }
                        }
                        FitnessTab.EXERCISES -> {
                            Text(text = "Exercises", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(exercises, key = { it.id }) { exercise ->
                                    ExerciseItem(
                                        exercise = exercise,
                                        onClick = {
                                            selectedExercise = exercise
                                            activeForm = FitnessFormType.EDIT_EXERCISE
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeForm != FitnessFormType.NONE) {
            ModalBottomSheet(
                onDismissRequest = { 
                    activeForm = FitnessFormType.NONE
                    selectedSession = null
                    selectedPlan = null
                    selectedExercise = null
                },
                sheetState = sheetState
            ) {
                when (activeForm) {
                    FitnessFormType.SESSION -> WorkoutSessionForm(
                        plans = plans,
                        viewModel = viewModel,
                        onSave = { session, sets ->
                            viewModel.addSession(session, sets)
                            activeForm = FitnessFormType.NONE
                        }
                    )
                    FitnessFormType.EDIT_SESSION -> selectedSession?.let { session ->
                        WorkoutSessionForm(
                            session = session,
                            plans = plans,
                            viewModel = viewModel,
                            onSave = { updatedSession, sets ->
                                viewModel.updateSession(updatedSession, sets)
                                activeForm = FitnessFormType.NONE
                            }
                        )
                    }
                    FitnessFormType.PLAN -> WorkoutPlanForm(
                        availableExercises = exercises,
                        onSave = { plan, selectedExercises ->
                            viewModel.addWorkoutPlan(plan, selectedExercises)
                            activeForm = FitnessFormType.NONE
                        }
                    )
                    FitnessFormType.EDIT_PLAN -> selectedPlan?.let { plan ->
                        val planExercises by viewModel.getExercisesForPlan(plan.id).collectAsState(initial = emptyList())
                        WorkoutPlanForm(
                            plan = plan,
                            availableExercises = exercises,
                            initialExercises = planExercises,
                            onSave = { updatedPlan, selectedExercises ->
                                viewModel.updateWorkoutPlan(updatedPlan, selectedExercises)
                                activeForm = FitnessFormType.NONE
                            }
                        )
                    }
                    FitnessFormType.EXERCISE -> ExerciseForm(
                        onSave = { exercise ->
                            viewModel.addExercise(exercise)
                            activeForm = FitnessFormType.NONE
                        }
                    )
                    FitnessFormType.EDIT_EXERCISE -> selectedExercise?.let { exercise ->
                        ExerciseForm(
                            exercise = exercise,
                            onSave = { updatedExercise ->
                                viewModel.updateExercise(updatedExercise)
                                activeForm = FitnessFormType.NONE
                            }
                        )
                    }
                    FitnessFormType.SESSION_DETAIL -> {
                        selectedSession?.let { session ->
                            val plan = plans.find { it.id == session.planId }
                            WorkoutSessionDetail(
                                session = session,
                                plan = plan,
                                viewModel = viewModel,
                                exercises = exercises,
                                onEditSession = {
                                    activeForm = FitnessFormType.EDIT_SESSION
                                },
                                onDeleteSession = { showDeleteDialog = session }
                            )
                        }
                    }
                    FitnessFormType.PLAN_DETAIL -> {
                        selectedPlan?.let { plan ->
                            val planExercises by viewModel.getExercisesForPlan(plan.id).collectAsState(initial = emptyList())
                            WorkoutPlanDetail(
                                plan = plan,
                                exercises = planExercises,
                                onEditPlan = {
                                    activeForm = FitnessFormType.EDIT_PLAN
                                },
                                onDeletePlan = { showDeleteDialog = plan }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        showDeleteDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete this item?") },
                confirmButton = {
                    TextButton(onClick = {
                        when (item) {
                            is WorkoutSession -> viewModel.deleteSession(item)
                            is WorkoutPlan -> viewModel.deleteWorkoutPlan(item)
                            is Exercise -> viewModel.deleteExercise(item)
                        }
                        showDeleteDialog = null
                        activeForm = FitnessFormType.NONE
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
}
