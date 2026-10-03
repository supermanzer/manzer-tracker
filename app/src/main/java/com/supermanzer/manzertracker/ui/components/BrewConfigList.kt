package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.BrewConfig
import java.util.Locale

// Ranked list of recipes. A list rather than a chart: the recipe itself is the thing to read.
@Composable
fun BrewConfigList(configs: List<BrewConfig>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        configs.forEachIndexed { index, config ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listOf(
                            config.waterTemp?.let { "$it°F" } ?: "No temp",
                            config.grindSize?.let { "Grind $it" } ?: "No grind",
                            config.ratio ?: "No ratio"
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${config.methods.joinToString(", ")} · " +
                            "${config.count} ${if (config.count == 1) "brew" else "brews"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", config.average)} / 5",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
