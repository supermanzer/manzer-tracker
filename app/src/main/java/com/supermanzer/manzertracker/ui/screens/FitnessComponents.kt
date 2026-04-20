package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.Exercise
import com.supermanzer.manzertracker.data.WorkoutPlan
import com.supermanzer.manzertracker.data.WorkoutSession
import com.supermanzer.manzertracker.ui.viewmodels.FitnessViewModel
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutPlanItem(plan: WorkoutPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium
            )
            plan.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun WorkoutPlanDetail(
    plan: WorkoutPlan,
    exercises: List<Exercise>,
    onEditPlan: () -> Unit,
    onDeletePlan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Plan Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditPlan) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Plan")
                }
                IconButton(onClick = onDeletePlan) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Name", value = plan.name)
        plan.description?.let {
            DetailRow(label = "Description", value = it)
        }

        if (exercises.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            exercises.forEachIndexed { index, exercise ->
                Text(
                    text = "${index + 1}. ${exercise.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WorkoutSessionItem(session: WorkoutSession, plan: WorkoutPlan?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan?.name ?: "Quick Workout",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .withZone(ZoneId.systemDefault()).format(session.startTime),
                style = MaterialTheme.typography.labelSmall
            )
            session.endTime?.let {
                val minutes = Duration.between(session.startTime, it).toMinutes()
                Text(text = "Duration: $minutes mins", style = MaterialTheme.typography.bodySmall)
            }
            session.notes?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun WorkoutSessionDetail(
    session: WorkoutSession,
    plan: WorkoutPlan?,
    viewModel: FitnessViewModel,
    exercises: List<Exercise>,
    onEditSession: () -> Unit,
    onDeleteSession: () -> Unit
) {
    val sets by viewModel.getSetsForSession(session.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Workout Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditSession) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Session")
                }
                IconButton(onClick = onDeleteSession) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Workout Plan", value = plan?.name ?: "None")
        DetailRow(
            label = "Start Time",
            value = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .withZone(ZoneId.systemDefault()).format(session.startTime)
        )
        session.endTime?.let {
            DetailRow(
                label = "End Time",
                value = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .withZone(ZoneId.systemDefault()).format(it)
            )
        }
        
        session.notes?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodyLarge)
        }

        if (sets.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val setsByExercise = sets.groupBy { it.exerciseId }
            setsByExercise.forEach { (exerciseId, exerciseSets) ->
                val exercise = exercises.find { it.id == exerciseId }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = exercise?.name ?: "Unknown Exercise",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    exerciseSets.sortedBy { it.setNumber }.forEach { set ->
                        Text(
                            text = "Set ${set.setNumber}: ${set.weight}kg x ${set.reps}${set.rpe?.let { " (RPE $it)" } ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
            exercise.category?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun ExerciseDetail(
    exercise: Exercise,
    onEditExercise: () -> Unit,
    onDeleteExercise: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Exercise Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditExercise) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Exercise")
                }
                IconButton(onClick = onDeleteExercise) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Exercise", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Name", value = exercise.name)
        exercise.category?.let { DetailRow(label = "Category", value = it) }
    }
}
