package com.supermanzer.manzertracker.data

import java.time.Duration
import java.time.Instant

data class RatingPoint(val date: Instant, val rating: Int)

data class RatingAverage(val label: String, val average: Double, val count: Int)

data class BagOption(val bagId: Long, val label: String)

data class RoasterOption(val roasterId: Long, val name: String)

// One distinct recipe (temperature, grind, ratio) and how the brews that used it were rated.
data class BrewConfig(
    val waterTemp: Int?,
    val grindSize: Int?,
    val ratio: String?,
    val methods: List<String>,
    val average: Double,
    val count: Int
)

// Everything the Insights screen shows, derived from the three coffee tables.
data class BrewInsights(
    val brewCount: Int = 0,
    val recentBrewCount: Int = 0,
    val averageRating: Double? = null,
    val ratedBags: List<BagOption> = emptyList(),
    val selectedBag: BagOption? = null,
    val ratingTrend: List<RatingPoint> = emptyList(),
    val byRoaster: List<RatingAverage> = emptyList(),
    val byMethod: List<RatingAverage> = emptyList(),
    val topBags: List<RatingAverage> = emptyList(),
    val ratedRoasters: List<RoasterOption> = emptyList(),
    val selectedRoaster: RoasterOption? = null,
    val topConfigs: List<BrewConfig> = emptyList()
)

const val RECENT_DAYS = 30L
const val TOP_BAG_COUNT = 5
const val TOP_CONFIG_COUNT = 3
const val MIN_CONFIG_BREWS = 5

fun buildBrewInsights(
    brews: List<CoffeeBrew>,
    bags: List<CoffeeBag>,
    roasters: List<Roaster>,
    selectedBagId: Long?,
    selectedRoasterId: Long?,
    now: Instant
): BrewInsights {
    val bagsById = bags.associateBy { it.id }
    val roastersById = roasters.associateBy { it.id }
    val rated = brews.filter { it.rating != null }

    // Bags with at least one rated brew, most recently brewed first.
    val ratedBags = rated
        .sortedByDescending { it.brewDate }
        .mapNotNull { bagsById[it.bagId] }
        .distinctBy { it.id }
        .map { bag ->
            BagOption(bag.id, "${roastersById[bag.roasterId]?.name ?: "Unknown"} - ${bag.name}")
        }
    val selectedBag = ratedBags.find { it.bagId == selectedBagId } ?: ratedBags.firstOrNull()

    // Roasters with at least one rated brew, most recently brewed first.
    val ratedRoasters = rated
        .sortedByDescending { it.brewDate }
        .mapNotNull { brew -> roastersById[bagsById[brew.bagId]?.roasterId] }
        .distinctBy { it.id }
        .map { RoasterOption(it.id, it.name) }
    val selectedRoaster = ratedRoasters.find { it.roasterId == selectedRoasterId } ?: ratedRoasters.firstOrNull()

    return BrewInsights(
        brewCount = brews.size,
        recentBrewCount = brews.count { it.brewDate >= now.minus(Duration.ofDays(RECENT_DAYS)) },
        averageRating = rated.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average(),
        ratedBags = ratedBags,
        selectedBag = selectedBag,
        ratingTrend = selectedBag?.let { ratingsOverTime(brews, it.bagId) }.orEmpty(),
        byRoaster = averageRatingBy(
            brews,
            keyOf = { brew -> bagsById[brew.bagId]?.roasterId?.takeIf { it in roastersById } },
            labelOf = { roastersById.getValue(it).name }
        ),
        byMethod = averageRatingBy(
            brews,
            keyOf = { brew -> BrewMethod.fromLabel(brew.method)?.label ?: brew.method.trim().takeIf { it.isNotEmpty() } },
            labelOf = { it }
        ),
        topBags = averageRatingBy(
            brews,
            keyOf = { brew -> brew.bagId.takeIf { it in bagsById } },
            labelOf = { bagsById.getValue(it).name }
        ).take(TOP_BAG_COUNT),
        ratedRoasters = ratedRoasters,
        selectedRoaster = selectedRoaster,
        topConfigs = selectedRoaster?.let { roaster ->
            topBrewConfigs(brews.filter { bagsById[it.bagId]?.roasterId == roaster.roasterId })
        }.orEmpty()
    )
}

// Groups rated brews by recipe and returns the best-rated recipes, ties going to the one brewed more.
// A recipe needs minBrews rated brews to qualify, so one lucky cup cannot top the list.
fun topBrewConfigs(
    brews: List<CoffeeBrew>,
    limit: Int = TOP_CONFIG_COUNT,
    minBrews: Int = MIN_CONFIG_BREWS
): List<BrewConfig> =
    brews
        .filter { it.rating != null }
        .groupBy { Triple(it.waterTemp, it.grindSize, it.ratio?.trim()?.takeIf { r -> r.isNotEmpty() }) }
        .filterValues { it.size >= minBrews }
        .map { (recipe, group) ->
            BrewConfig(
                waterTemp = recipe.first,
                grindSize = recipe.second,
                ratio = recipe.third,
                methods = group.map { BrewMethod.fromLabel(it.method)?.label ?: it.method.trim() }.distinct(),
                average = group.mapNotNull { it.rating }.average(),
                count = group.size
            )
        }
        .sortedWith(compareByDescending<BrewConfig> { it.average }.thenByDescending { it.count })
        .take(limit)

fun ratingsOverTime(brews: List<CoffeeBrew>, bagId: Long): List<RatingPoint> =
    brews
        .filter { it.bagId == bagId }
        .mapNotNull { brew -> brew.rating?.let { RatingPoint(brew.brewDate, it) } }
        .sortedBy { it.date }

// Average rating per group, best first. Unrated brews and brews with no key are left out.
fun <K : Any> averageRatingBy(
    brews: List<CoffeeBrew>,
    keyOf: (CoffeeBrew) -> K?,
    labelOf: (K) -> String
): List<RatingAverage> =
    brews
        .mapNotNull { brew ->
            val key = keyOf(brew)
            val rating = brew.rating
            if (key != null && rating != null) key to rating else null
        }
        .groupBy({ it.first }, { it.second })
        .map { (key, ratings) -> RatingAverage(labelOf(key), ratings.average(), ratings.size) }
        .sortedWith(
            compareByDescending<RatingAverage> { it.average }
                .thenByDescending { it.count }
                .thenBy { it.label }
        )
