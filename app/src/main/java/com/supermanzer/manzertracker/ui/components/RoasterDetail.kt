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
import com.supermanzer.manzertracker.data.Roaster

@Composable
fun RoasterDetail(
    roaster: Roaster,
    onEditRoaster: () -> Unit,
    onDeleteRoaster: () -> Unit
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
            Text(text = "Roaster Details", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onEditRoaster) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Roaster")
                }
                IconButton(onClick = onDeleteRoaster) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Roaster", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Name", value = roaster.name)
        roaster.location?.let { DetailRow(label = "Location", value = it) }
    }
}
