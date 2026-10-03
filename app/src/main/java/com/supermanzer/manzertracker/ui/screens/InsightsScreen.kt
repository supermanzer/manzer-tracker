package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supermanzer.manzertracker.BrewBuddyApplication
import com.supermanzer.manzertracker.data.BrewInsights
import com.supermanzer.manzertracker.data.MIN_CONFIG_BREWS
import com.supermanzer.manzertracker.data.RECENT_DAYS
import com.supermanzer.manzertracker.ui.components.BrewConfigList
import com.supermanzer.manzertracker.ui.components.ChartCard
import com.supermanzer.manzertracker.ui.components.DropdownField
import com.supermanzer.manzertracker.ui.components.RatingBarList
import com.supermanzer.manzertracker.ui.components.RatingLineChart
import com.supermanzer.manzertracker.ui.components.StatTile
import com.supermanzer.manzertracker.ui.theme.chartMarkColor
import com.supermanzer.manzertracker.ui.theme.coffeeGradient
import com.supermanzer.manzertracker.ui.viewmodels.InsightsViewModel
import com.supermanzer.manzertracker.ui.viewmodels.InsightsViewModelFactory
import java.util.Locale

@Composable
fun InsightsScreen() {
    val context = LocalContext.current
    val database = (context.applicationContext as BrewBuddyApplication).database
    val viewModel: InsightsViewModel = viewModel(
        factory = InsightsViewModelFactory(database.coffeeDao())
    )
    val insights by viewModel.insights.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coffeeGradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Insights", style = MaterialTheme.typography.headlineMedium)
            // Nothing is drawn until the first database read arrives.
            insights?.let {
                InsightsContent(
                    insights = it,
                    onBagSelected = viewModel::selectBag,
                    onRoasterSelected = viewModel::selectRoaster
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(
    insights: BrewInsights,
    onBagSelected: (Long) -> Unit,
    onRoasterSelected: (Long) -> Unit
) {
    val markColor = chartMarkColor()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            label = "Brews",
            value = insights.brewCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Avg rating",
            value = insights.averageRating?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "–",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Last $RECENT_DAYS days",
            value = insights.recentBrewCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }

    val selectedBag = insights.selectedBag
    if (selectedBag == null) {
        ChartCard(title = "No ratings yet") {
            Text(
                text = "Log a brew and give it a rating, and trends will appear here.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val trend = insights.ratingTrend
    val trendAverage = String.format(Locale.getDefault(), "%.1f", trend.map { it.rating }.average())
    val trendSummary = "${trend.size} rated ${if (trend.size == 1) "brew" else "brews"} · average $trendAverage"
    ChartCard(title = "Rating over time", subtitle = trendSummary) {
        DropdownField(
            label = "Coffee bag",
            options = insights.ratedBags,
            selected = selectedBag,
            onSelected = { onBagSelected(it.bagId) },
            optionLabel = { it.label }
        )
        Spacer(modifier = Modifier.height(12.dp))
        RatingLineChart(
            points = trend,
            lineColor = markColor,
            description = "Ratings over time for ${selectedBag.label}: $trendSummary, " +
                "latest ${trend.last().rating} out of 5.",
            modifier = Modifier.fillMaxWidth()
        )
    }

    ChartCard(title = "Average rating by roaster", subtitle = "Out of 5, across all bags") {
        RatingBarList(items = insights.byRoaster, barColor = markColor)
    }

    insights.selectedRoaster?.let { selectedRoaster ->
        ChartCard(
            title = "Best recipes by roaster",
            subtitle = "By average rating, recipes with $MIN_CONFIG_BREWS or more rated brews"
        ) {
            DropdownField(
                label = "Roaster",
                options = insights.ratedRoasters,
                selected = selectedRoaster,
                onSelected = { onRoasterSelected(it.roasterId) },
                optionLabel = { it.name }
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (insights.topConfigs.isEmpty()) {
                Text(
                    text = "No recipe for this roaster has $MIN_CONFIG_BREWS rated brews yet. " +
                        "Repeat a recipe to see how it holds up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                BrewConfigList(configs = insights.topConfigs)
            }
        }
    }

    ChartCard(title = "Average rating by brew method", subtitle = "Out of 5") {
        RatingBarList(items = insights.byMethod, barColor = markColor)
    }

    ChartCard(title = "Top-rated bags", subtitle = "Out of 5, best ${insights.topBags.size}") {
        RatingBarList(items = insights.topBags, barColor = markColor)
    }
}
