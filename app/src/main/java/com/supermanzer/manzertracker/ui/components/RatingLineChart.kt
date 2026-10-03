package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.RatingPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private const val MIN_RATING = 1
private const val MAX_RATING = 5
private const val MAX_POINTS_WITH_MARKERS = 40

// Ratings over time for one series. The y-axis is fixed at 1-5 so charts for different bags are
// comparable; the x-axis is real time, so gaps between brews show as gaps.
@Composable
fun RatingLineChart(
    points: List<RatingPoint>,
    lineColor: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val axisDate = remember {
        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }
    val axisTime = remember {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }
    val readoutDate = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }

    Column(modifier = modifier) {
        Text(
            text = selectedIndex?.let { points.getOrNull(it) }
                ?.let { "${readoutDate.format(it.date)} · ${it.rating} / $MAX_RATING" }
                ?: "Tap the chart to inspect a brew",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = description }
                .pointerInput(points) {
                    detectTapGestures { tap ->
                        val xs = pointXs(points, plotLeft(), size.width - plotRight())
                        selectedIndex = xs.indices.minByOrNull { abs(xs[it] - tap.x) }
                    }
                }
        ) {
            val left = plotLeft()
            val right = size.width - plotRight()
            val top = 8.dp.toPx()
            val bottom = size.height - 24.dp.toPx()

            fun yFor(rating: Int) =
                bottom - (rating - MIN_RATING).toFloat() / (MAX_RATING - MIN_RATING) * (bottom - top)

            // Hairline gridlines with a tick label at every whole rating.
            for (rating in MIN_RATING..MAX_RATING) {
                val y = yFor(rating)
                drawLine(gridColor, Offset(left - 8.dp.toPx(), y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(rating.toString(), axisStyle)
                drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
            }

            if (points.isEmpty()) return@Canvas
            val xs = pointXs(points, left, right)
            val offsets = points.mapIndexed { i, point -> Offset(xs[i], yFor(point.rating)) }

            // First and last dates label the time axis; a lone point gets its date centred.
            // Brews that all fall on one day are labelled by time, or both ends would read the same.
            val oneDay = xs.first() != xs.last() &&
                axisDate.format(points.first().date) == axisDate.format(points.last().date)
            val axisFormat = if (oneDay) axisTime else axisDate
            val firstLabel = textMeasurer.measure(axisFormat.format(points.first().date), axisStyle)
            val lastLabel = textMeasurer.measure(axisFormat.format(points.last().date), axisStyle)
            val labelY = bottom + 8.dp.toPx()
            if (xs.first() == xs.last()) {
                drawText(firstLabel, topLeft = Offset(xs.first() - firstLabel.size.width / 2f, labelY))
            } else {
                drawText(firstLabel, topLeft = Offset(left, labelY))
                drawText(lastLabel, topLeft = Offset(size.width - lastLabel.size.width, labelY))
            }

            selectedIndex?.let { offsets.getOrNull(it) }?.let { selected ->
                drawLine(gridColor, Offset(selected.x, top), Offset(selected.x, bottom), strokeWidth = 1.dp.toPx())
            }

            if (offsets.size > 1) {
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Each marker sits on a ring of the card colour so it stays distinct where it crosses the line.
            offsets.forEachIndexed { i, offset ->
                val isSelected = i == selectedIndex
                if (isSelected || points.size <= MAX_POINTS_WITH_MARKERS) {
                    val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(surfaceColor, radius + 2.dp.toPx(), offset)
                    drawCircle(lineColor, radius, offset)
                }
            }
        }
    }
}

// Room on the left for the rating tick labels, and on the right so the last marker is not clipped.
private fun androidx.compose.ui.unit.Density.plotLeft() = 28.dp.toPx()
private fun androidx.compose.ui.unit.Density.plotRight() = 12.dp.toPx()

// Maps each point's timestamp onto the horizontal plot range.
private fun pointXs(points: List<RatingPoint>, left: Float, right: Float): List<Float> {
    if (points.isEmpty()) return emptyList()
    val start = points.first().date.toEpochMilli()
    val span = points.last().date.toEpochMilli() - start
    return points.map { point ->
        if (span == 0L) (left + right) / 2f
        else left + (point.date.toEpochMilli() - start).toFloat() / span * (right - left)
    }
}
