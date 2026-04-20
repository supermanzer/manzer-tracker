package com.supermanzer.manzertracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "roasters")
data class Roaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String? = null
)

@Entity(
    tableName = "coffee_bags",
    foreignKeys = [
        ForeignKey(
            entity = Roaster::class,
            parentColumns = ["id"],
            childColumns = ["roasterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roasterId")]
)
data class CoffeeBag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roasterId: Long,
    val name: String,
    val origin: String? = null,
    val variety: String? = null,
    val process: String? = null,
    val roastDate: LocalDate? = null,
    val region: String? = null
)

@Entity(
    tableName = "coffee_brews",
    foreignKeys = [
        ForeignKey(
            entity = CoffeeBag::class,
            parentColumns = ["id"],
            childColumns = ["bagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bagId")]
)
data class CoffeeBrew(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bagId: Long,
    val brewDate: Instant = Instant.now(),
    val method: String,
    val grindSize: Int? = null,
    val waterTemp: Int? = null,
    val ratio: String? = null,
    val rating: Int? = null,
    val notes: String? = null
)
