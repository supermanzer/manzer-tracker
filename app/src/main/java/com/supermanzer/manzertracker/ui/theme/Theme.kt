package com.supermanzer.manzertracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val CoffeeDarkColorScheme = darkColorScheme(
    primary = CoffeeDarkPrimary,
    secondary = CoffeeDarkSecondary,
    background = CoffeeDarkBackground,
    surface = CoffeeDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val CoffeeLightColorScheme = lightColorScheme(
    primary = CoffeeLightPrimary,
    secondary = CoffeeLightSecondary,
    background = CoffeeLightBackground,
    surface = CoffeeLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val FitnessDarkColorScheme = darkColorScheme(
    primary = Color.White, // High contrast for dark mode
    secondary = FitnessDark1,
    tertiary = FitnessDark3,
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.Black
)

private val FitnessLightColorScheme = lightColorScheme(
    primary = FitnessLightPrimary,
    secondary = FitnessLightSecondary,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onPrimary = Color.White
)

@Composable
fun ManzerTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFitness: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isFitness) {
        if (darkTheme) FitnessDarkColorScheme else FitnessLightColorScheme
    } else {
        if (darkTheme) CoffeeDarkColorScheme else CoffeeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
