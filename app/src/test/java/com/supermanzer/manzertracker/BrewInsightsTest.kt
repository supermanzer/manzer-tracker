package com.supermanzer.manzertracker

import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.Roaster
import com.supermanzer.manzertracker.data.buildBrewInsights
import com.supermanzer.manzertracker.data.ratingsOverTime
import com.supermanzer.manzertracker.data.topBrewConfigs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BrewInsightsTest {

    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private fun daysAgo(days: Long) = now.minus(Duration.ofDays(days))

    private val roasters = listOf(Roaster(1, "Onyx"), Roaster(2, "Verve"), Roaster(3, "Unbrewed"))
    private val bags = listOf(
        CoffeeBag(id = 10, roasterId = 1, name = "Geometry"),
        CoffeeBag(id = 11, roasterId = 1, name = "Monarch"),
        CoffeeBag(id = 20, roasterId = 2, name = "Streetlevel")
    )

    private fun brew(bagId: Long, daysAgo: Long, rating: Int?, method: String = "V60") =
        CoffeeBrew(bagId = bagId, brewDate = daysAgo(daysAgo), method = method, rating = rating)

    private val brews = listOf(
        brew(10, 40, 3),
        brew(10, 20, 5),
        brew(10, 10, null),
        brew(11, 5, 4, method = "french press"),
        brew(20, 2, 2, method = "Espresso")
    )

    private fun insights(selectedBagId: Long? = null, selectedRoasterId: Long? = null) =
        buildBrewInsights(brews, bags, roasters, selectedBagId, selectedRoasterId, now)

    @Test
    fun counts_include_unrated_brews_but_average_does_not() {
        val result = insights()
        assertEquals(5, result.brewCount)
        assertEquals(4, result.recentBrewCount)
        assertEquals(3.5, result.averageRating!!, 1e-9)
    }

    @Test
    fun trend_is_oldest_first_and_skips_unrated_brews() {
        assertEquals(listOf(3, 5), ratingsOverTime(brews, 10).map { it.rating })
    }

    @Test
    fun roaster_average_spans_its_bags_and_sorts_best_first() {
        val result = insights().byRoaster
        assertEquals(listOf("Onyx", "Verve"), result.map { it.label })
        assertEquals(4.0, result[0].average, 1e-9)
        assertEquals(3, result[0].count)
        assertEquals(1, result[1].count)
    }

    @Test
    fun top_bags_carry_ids_and_details_for_opening_them() {
        val result = insights()
        assertEquals(listOf(10L, 11L, 20L), result.topBags.map { it.id })
        assertEquals(setOf(10L, 11L, 20L), result.topBagDetails.keys)
        assertEquals("Geometry", result.topBagDetails.getValue(10).bag.name)
        assertEquals("Onyx", result.topBagDetails.getValue(10).roaster?.name)
        // Rows that are not a single record cannot be opened.
        assertEquals(listOf<Long?>(null, null), result.byRoaster.map { it.id })
    }

    @Test
    fun method_labels_are_normalised_to_the_known_list() {
        // V60 and French Press both average 4.0; the tie goes to the one with more brews.
        assertEquals(
            listOf("V60", "French Press", "Espresso"),
            insights().byMethod.map { it.label }
        )
    }

    @Test
    fun defaults_to_most_recently_rated_bag_and_honours_selection() {
        assertEquals(20L, insights().selectedBag?.bagId)
        assertEquals("Verve - Streetlevel", insights().selectedBag?.label)
        assertEquals(10L, insights(selectedBagId = 10).selectedBag?.bagId)
        assertEquals(listOf(3, 5), insights(selectedBagId = 10).ratingTrend.map { it.rating })
        // A stale selection (deleted bag) falls back to the default.
        assertEquals(20L, insights(selectedBagId = 999).selectedBag?.bagId)
    }

    @Test
    fun no_rated_brews_means_no_selection_and_no_average() {
        val result = buildBrewInsights(listOf(brew(10, 1, null)), bags, roasters, null, null, now)
        assertEquals(1, result.brewCount)
        assertNull(result.averageRating)
        assertNull(result.selectedBag)
        assertEquals(emptyList<Any>(), result.byRoaster)
        assertNull(result.selectedRoaster)
        assertEquals(emptyList<Any>(), result.topConfigs)
    }

    private fun recipe(temp: Int?, grind: Int?, ratio: String?, rating: Int?, method: String = "V60") =
        CoffeeBrew(bagId = 10, method = method, waterTemp = temp, grindSize = grind, ratio = ratio, rating = rating)

    private fun recipes(temp: Int, ratings: List<Int?>, ratio: String = "1:16", method: String = "V60") =
        ratings.map { recipe(temp, 15, ratio, it, method) }

    @Test
    fun top_configs_need_five_rated_brews_and_average_all_of_them() {
        val result = topBrewConfigs(
            recipes(200, listOf(5, 5, 5, 5)) +              // four brews: not enough
                recipes(201, listOf(5, 5, 5, 5, null)) +    // unrated brew does not count toward five
                recipes(205, listOf(5, 4, 4, 4, 3, 4))      // six brews: average of all six
        )
        assertEquals(listOf(205), result.map { it.waterTemp })
        assertEquals(6, result[0].count)
        assertEquals(4.0, result[0].average, 1e-9)
    }

    @Test
    fun top_configs_rank_by_average_then_count_and_keep_three() {
        val result = topBrewConfigs(
            recipes(200, listOf(4, 4, 4, 4, 4)) +
                recipes(201, listOf(4, 4, 4, 4, 4, 4)) +
                recipes(202, listOf(5, 5, 5, 5, 5)) +
                recipes(203, listOf(1, 1, 1, 1, 1))
        )
        // 201 and 200 both average 4.0; the one brewed more ranks higher. 203 is cut.
        assertEquals(listOf(202, 201, 200), result.map { it.waterTemp })
    }

    @Test
    fun top_configs_ignore_ratio_whitespace_and_list_each_method_once() {
        val result = topBrewConfigs(
            recipes(205, listOf(5, 4, 4)) + recipes(205, listOf(4, 3), ratio = " 1:16 ", method = "aeropress")
        )
        assertEquals(1, result.size)
        assertEquals(5, result[0].count)
        assertEquals(listOf("V60", "Aeropress"), result[0].methods)
    }

    @Test
    fun top_configs_follow_the_selected_roaster() {
        // Default is the most recently rated roaster (Verve); a stale id falls back to it.
        assertEquals(listOf("Verve", "Onyx"), insights().ratedRoasters.map { it.name })
        assertEquals(2L, insights().selectedRoaster?.roasterId)
        assertEquals(1L, insights(selectedRoasterId = 1).selectedRoaster?.roasterId)
        assertEquals(2L, insights(selectedRoasterId = 999).selectedRoaster?.roasterId)
        // The fixture has too few brews per recipe for anything to qualify.
        assertEquals(emptyList<Any>(), insights(selectedRoasterId = 1).topConfigs)

        // Onyx's recipes pool across both of its bags; Verve's brews are left out.
        val onyx = listOf(10L, 10L, 10L, 11L, 11L).map { brew(it, 1, 4) } + brew(20, 1, 1)
        val pooled = buildBrewInsights(onyx, bags, roasters, null, 1, now).topConfigs
        assertEquals(listOf(5), pooled.map { it.count })
        assertEquals(4.0, pooled[0].average, 1e-9)
    }
}
