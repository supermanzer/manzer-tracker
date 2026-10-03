package com.supermanzer.manzertracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supermanzer.manzertracker.data.BrewInsights
import com.supermanzer.manzertracker.data.CoffeeDao
import com.supermanzer.manzertracker.data.buildBrewInsights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class InsightsViewModel(coffeeDao: CoffeeDao) : ViewModel() {

    private val selectedBagId = MutableStateFlow<Long?>(null)
    private val selectedRoasterId = MutableStateFlow<Long?>(null)

    // null until the first database read arrives, so the screen can tell "loading" from "no data".
    val insights: StateFlow<BrewInsights?> = combine(
        coffeeDao.getAllBrews(),
        coffeeDao.getAllCoffeeBags(),
        coffeeDao.getAllRoasters(),
        selectedBagId,
        selectedRoasterId
    ) { brews, bags, roasters, bagId, roasterId ->
        buildBrewInsights(brews, bags, roasters, bagId, roasterId, Instant.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectBag(bagId: Long) {
        selectedBagId.value = bagId
    }

    fun selectRoaster(roasterId: Long) {
        selectedRoasterId.value = roasterId
    }
}

class InsightsViewModelFactory(private val coffeeDao: CoffeeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(coffeeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
