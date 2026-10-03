package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DetailRow(label: String, value: String, onEdit: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit $label", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}
