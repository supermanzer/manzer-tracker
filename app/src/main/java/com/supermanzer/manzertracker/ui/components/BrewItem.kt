package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.Roaster
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BrewItem(brew: CoffeeBrew, bag: CoffeeBag?, roaster: Roaster?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${bag?.name ?: "Unknown Bag"} (${roaster?.name ?: "Unknown Roaster"})",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Method: ${brew.method}")
            Text(text = "Brew Ratio: ${brew.ratio}, Water Temp: ${brew.waterTemp}, Grind Size: ${brew.grindSize}", style = MaterialTheme.typography.bodyMedium)
            brew.rating?.let {
                Text(text = "Rating: $it/5", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault()).format(brew.brewDate),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
