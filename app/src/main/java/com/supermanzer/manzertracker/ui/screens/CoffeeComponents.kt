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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.Roaster
import java.text.SimpleDateFormat
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
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(brew.brewDate),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun BrewDetail(
    brew: CoffeeBrew, 
    bag: CoffeeBag?, 
    roaster: Roaster?,
    onEditBrew: () -> Unit,
    onEditBag: () -> Unit,
    onEditRoaster: () -> Unit,
    onDeleteBrew: () -> Unit
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
            Text(text = "Brew Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditBrew) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Brew")
                }
                IconButton(onClick = onDeleteBrew) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Brew", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailSection(
            title = "Coffee Information",
            onEdit = onEditBag
        ) {
            DetailRow(label = "Roaster", value = roaster?.name ?: "Unknown", onEdit = onEditRoaster)
            roaster?.location?.let { DetailRow(label = "Roaster Location", value = it) }
            DetailRow(label = "Coffee Name", value = bag?.name ?: "Unknown")
            bag?.origin?.let { DetailRow(label = "Origin", value = it) }
            bag?.region?.let { DetailRow(label = "Region", value = it) }
            bag?.variety?.let { DetailRow(label = "Variety", value = it) }
            bag?.process?.let { DetailRow(label = "Process", value = it) }
            bag?.roastDate?.let {
                DetailRow(
                    label = "Roast Date",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        DetailSection(title = "Brew Parameters") {
            DetailRow(
                label = "Date",
                value = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(brew.brewDate)
            )
            DetailRow(label = "Method", value = brew.method)
            brew.ratio?.let { DetailRow(label = "Ratio", value = it) }
            brew.waterTemp?.let { DetailRow(label = "Water Temp", value = "${it}°F") }
            brew.grindSize?.let { DetailRow(label = "Grind Size", value = it.toString()) }
            brew.rating?.let { DetailRow(label = "Rating", value = "$it / 5") }
        }

        brew.notes?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            DetailSection(title = "Notes") {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
