package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.RatingAverage
import java.util.Locale

private const val MAX_RATING = 5f

// Horizontal bar chart of average ratings on a fixed 0-5 scale. Built from layout rather than
// Canvas: every label and value is real text, so it scales with font size and reads in TalkBack.
@Composable
fun RatingBarList(
    items: List<RatingAverage>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // The count sits beside the average: one brew must not read like twenty.
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", item.average)} · " +
                            "${item.count} ${if (item.count == 1) "brew" else "brews"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.average.toFloat() / MAX_RATING).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                color = barColor,
                                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                            )
                    )
                }
            }
        }
    }
}
