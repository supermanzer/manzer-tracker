package com.supermanzer.manzertracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    // Roaster
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoaster(roaster: Roaster): Long

    @Update
    suspend fun updateRoaster(roaster: Roaster)

    @Delete
    suspend fun deleteRoaster(roaster: Roaster)

    @Query("SELECT * FROM roasters ORDER BY name ASC")
    fun getAllRoasters(): Flow<List<Roaster>>

    // CoffeeBag
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffeeBag(bag: CoffeeBag): Long

    @Update
    suspend fun updateCoffeeBag(bag: CoffeeBag)

    @Delete
    suspend fun deleteCoffeeBag(bag: CoffeeBag)

    @Query("SELECT * FROM coffee_bags WHERE roasterId = :roasterId")
    fun getBagsForRoaster(roasterId: Long): Flow<List<CoffeeBag>>

    @Query("SELECT * FROM coffee_bags ORDER BY id DESC")
    fun getAllCoffeeBags(): Flow<List<CoffeeBag>>

    // CoffeeBrew
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrew(brew: CoffeeBrew): Long

    @Update
    suspend fun updateBrew(brew: CoffeeBrew)

    @Delete
    suspend fun deleteBrew(brew: CoffeeBrew)

    @Query("SELECT * FROM coffee_brews WHERE bagId = :bagId ORDER BY brewDate DESC")
    fun getBrewsForBag(bagId: Long): Flow<List<CoffeeBrew>>

    @Query("SELECT * FROM coffee_brews ORDER BY brewDate DESC")
    fun getAllBrews(): Flow<List<CoffeeBrew>>
}
