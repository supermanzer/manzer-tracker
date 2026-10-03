package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailSection(title: String, onEdit: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Section", modifier = Modifier.padding(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
