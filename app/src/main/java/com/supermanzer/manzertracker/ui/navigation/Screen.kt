package com.supermanzer.manzertracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Coffee : Screen("coffee", "Coffee", Icons.Default.Coffee)
    object Fitness : Screen("fitness", "Fitness", Icons.Default.FitnessCenter)
}

val items = listOf(
    Screen.Coffee,
    Screen.Fitness
)
