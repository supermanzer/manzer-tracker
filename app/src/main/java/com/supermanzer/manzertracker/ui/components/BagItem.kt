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
import com.supermanzer.manzertracker.data.Roaster
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BagItem(bag: CoffeeBag, roaster: Roaster?, onClick: () -> Unit) {
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
            Text(text = bag.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Roaster: ${roaster?.name ?: "Unknown"}")
            bag.origin?.let { Text(text = "Origin: $it", style = MaterialTheme.typography.bodyMedium) }
            bag.roastDate?.let {
                Text(
                    text = "Roast Date: ${DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).format(it)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
