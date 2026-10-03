package com.supermanzer.manzertracker.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.Roaster
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BagDetail(
    bag: CoffeeBag,
    roaster: Roaster?,
    onEditBag: () -> Unit,
    onDeleteBag: () -> Unit
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
            Text(text = "Coffee Bag Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditBag) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Bag")
                }
                IconButton(onClick = onDeleteBag) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Bag", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Name", value = bag.name)
        DetailRow(label = "Roaster", value = roaster?.name ?: "Unknown")
        bag.origin?.let { DetailRow(label = "Origin", value = it) }
        bag.region?.let { DetailRow(label = "Region", value = it) }
        bag.variety?.let { DetailRow(label = "Variety", value = it) }
        bag.process?.let { DetailRow(label = "Process", value = it) }
        bag.roastDate?.let {
            DetailRow(
                label = "Roast Date",
                value = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).format(it)
            )
        }
    }
}
