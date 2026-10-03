package com.supermanzer.manzertracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.supermanzer.manzertracker.ui.screens.CoffeeScreen
import com.supermanzer.manzertracker.ui.theme.BrewBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrewBuddyTheme {
                CoffeeScreen()
            }
        }
    }
}
