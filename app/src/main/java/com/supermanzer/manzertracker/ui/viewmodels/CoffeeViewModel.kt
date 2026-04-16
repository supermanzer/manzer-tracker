package com.supermanzer.manzertracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.CoffeeDao
import com.supermanzer.manzertracker.data.Roaster
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class CoffeeViewModel(private val coffeeDao: CoffeeDao) : ViewModel() {

    val allRoasters: StateFlow<List<Roaster>> = coffeeDao.getAllRoasters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBags: StateFlow<List<CoffeeBag>> = coffeeDao.getAllCoffeeBags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBrews: StateFlow<List<CoffeeBrew>> = coffeeDao.getAllBrews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRoaster(name: String, location: String? = null) {
        viewModelScope.launch {
            coffeeDao.insertRoaster(Roaster(name = name, location = location))
        }
    }

    fun updateRoaster(roaster: Roaster) {
        viewModelScope.launch {
            coffeeDao.updateRoaster(roaster)
        }
    }

    fun deleteRoaster(roaster: Roaster) {
        viewModelScope.launch {
            coffeeDao.deleteRoaster(roaster)
        }
    }

    fun addCoffeeBag(
        roasterId: Long,
        name: String,
        origin: String? = null,
        variety: String? = null,
        process: String? = null,
        roastDate: Date? = null,
        region: String? = null
    ) {
        viewModelScope.launch {
            coffeeDao.insertCoffeeBag(
                CoffeeBag(
                    roasterId = roasterId,
                    name = name,
                    origin = origin,
                    variety = variety,
                    process = process,
                    roastDate = roastDate,
                    region = region
                )
            )
        }
    }

    fun updateCoffeeBag(bag: CoffeeBag) {
        viewModelScope.launch {
            coffeeDao.updateCoffeeBag(bag)
        }
    }

    fun deleteCoffeeBag(bag: CoffeeBag) {
        viewModelScope.launch {
            coffeeDao.deleteCoffeeBag(bag)
        }
    }

    fun addBrew(brew: CoffeeBrew) {
        viewModelScope.launch {
            coffeeDao.insertBrew(brew)
        }
    }

    fun updateBrew(brew: CoffeeBrew) {
        viewModelScope.launch {
            coffeeDao.updateBrew(brew)
        }
    }

    fun deleteBrew(brew: CoffeeBrew) {
        viewModelScope.launch {
            coffeeDao.deleteBrew(brew)
        }
    }
}

class CoffeeViewModelFactory(private val coffeeDao: CoffeeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoffeeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoffeeViewModel(coffeeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
