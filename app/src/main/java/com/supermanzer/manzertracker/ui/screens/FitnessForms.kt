package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.Exercise
import com.supermanzer.manzertracker.data.WorkoutPlan
import com.supermanzer.manzertracker.data.WorkoutSession
import com.supermanzer.manzertracker.data.WorkoutSet
import com.supermanzer.manzertracker.ui.viewmodels.FitnessViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionForm(
    session: WorkoutSession? = null,
    plans: List<WorkoutPlan>,
    viewModel: FitnessViewModel,
    onSave: (WorkoutSession, List<WorkoutSet>) -> Unit
) {
    var selectedPlan by remember { mutableStateOf(plans.find { it.id == session?.planId }) }
    var notes by remember { mutableStateOf(session?.notes ?: "") }
    var expanded by remember { mutableStateOf(false) }
    
    val planExercises by if (selectedPlan != null) {
        viewModel.getExercisesForPlan(selectedPlan!!.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<Exercise>()) }
    }

    var sets by remember { mutableStateOf(emptyList<WorkoutSet>()) }

    // Initialize sets from existing session or plan exercises
    LaunchedEffect(selectedPlan, session, planExercises) {
        if (session != null && sets.isEmpty()) {
            viewModel.getSetsForSession(session.id).collect { fetchedSets ->
                if (sets.isEmpty()) {
                    sets = fetchedSets
                }
            }
        } else if (selectedPlan != null && session == null && sets.isEmpty() && planExercises.isNotEmpty()) {
            // Pre-fill sets based on plan exercises (one set per exercise as a starter)
            sets = planExercises.map { exercise ->
                WorkoutSet(
                    sessionId = 0, // Will be set on save
                    exerciseId = exercise.id,
                    setNumber = 1,
                    weight = 0.0,
                    reps = 0
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (session == null) "Log Workout" else "Edit Workout",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPlan?.name ?: "Select Workout Plan",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                plans.forEach { plan ->
                    DropdownMenuItem(
                        text = { Text(plan.name) },
                        onClick = {
                            selectedPlan = plan
                            expanded = false
                            sets = emptyList() // Reset sets to trigger re-fill
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedPlan != null) {
            Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
            planExercises.forEach { exercise ->
                val exerciseSets = sets.filter { it.exerciseId == exercise.id }
                
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                    
                    exerciseSets.forEachIndexed { index, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (set.weight == 0.0) "" else set.weight.toString(),
                                onValueChange = { val value = it.toDoubleOrNull() ?: 0.0
                                    sets = sets.map { s -> if (s === set) s.copy(weight = value) else s }
                                },
                                label = { Text("Lbs") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = if (set.reps == 0) "" else set.reps.toString(),
                                onValueChange = { val value = it.toIntOrNull() ?: 0
                                    sets = sets.map { s -> if (s === set) s.copy(reps = value) else s }
                                },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            IconButton(onClick = {
                                sets = sets.filter { it !== set }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Set")
                            }
                        }
                    }
                    
                    TextButton(onClick = {
                        val nextSetNum = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
                        sets = sets + WorkoutSet(
                            sessionId = 0,
                            exerciseId = exercise.id,
                            setNumber = nextSetNum,
                            weight = exerciseSets.lastOrNull()?.weight ?: 0.0,
                            reps = exerciseSets.lastOrNull()?.reps ?: 0
                        )
                    }) {
                        Text("+ Add Set")
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onSave(
                    session?.copy(
                        planId = selectedPlan?.id,
                        notes = notes
                    ) ?: WorkoutSession(
                        planId = selectedPlan?.id,
                        notes = notes,
                        startTime = Date()
                    ),
                    sets
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPlan != null
        ) {
            Text(if (session == null) "Log Workout" else "Update Workout")
        }
    }
}

@Composable
fun ExerciseForm(
    exercise: Exercise? = null,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var category by remember { mutableStateOf(exercise?.category ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = if (exercise == null) "Add Exercise" else "Edit Exercise",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (e.g. Strength, Cardio)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onSave(
                    exercise?.copy(name = name, category = category.takeIf { it.isNotBlank() })
                        ?: Exercise(name = name, category = category.takeIf { it.isNotBlank() })
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Text(if (exercise == null) "Save Exercise" else "Update Exercise")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanForm(
    plan: WorkoutPlan? = null,
    availableExercises: List<Exercise> = emptyList(),
    initialExercises: List<Exercise> = emptyList(),
    onSave: (WorkoutPlan, List<Exercise>) -> Unit
) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var description by remember { mutableStateOf(plan?.description ?: "") }
    var selectedExercises by remember { mutableStateOf(initialExercises) }
    var expanded by remember { mutableStateOf(false) }

    // Synchronize selectedExercises with initialExercises when they are loaded
    LaunchedEffect(initialExercises) {
        if (plan != null && initialExercises.isNotEmpty() && selectedExercises.isEmpty()) {
             selectedExercises = initialExercises
        }
    }

    // Synchronize selectedExercises with initialExercises when they are loaded
    LaunchedEffect(initialExercises) {
        if (plan != null && initialExercises.isNotEmpty()) {
             selectedExercises = initialExercises
        }
    }

    // Update state if initialExercises changes (e.g. when loaded from ViewModel)
    LaunchedEffect(initialExercises) {
        if (selectedExercises.isEmpty() && initialExercises.isNotEmpty()) {
            selectedExercises = initialExercises
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (plan == null) "Create Plan" else "Edit Plan",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Plan Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
        
        selectedExercises.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${index + 1}. ${exercise.name}")
                TextButton(onClick = {
                    selectedExercises = selectedExercises.toMutableList().apply { removeAt(index) }
                }) {
                    Text("Remove")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "Add Exercise",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableExercises.forEach { exercise ->
                    DropdownMenuItem(
                        text = { Text(exercise.name) },
                        onClick = {
                            selectedExercises = selectedExercises + exercise
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                onSave(
                    plan?.copy(name = name, description = description.takeIf { it.isNotBlank() })
                        ?: WorkoutPlan(name = name, description = description.takeIf { it.isNotBlank() }),
                    selectedExercises
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Text(if (plan == null) "Save Plan" else "Update Plan")
        }
    }
}
