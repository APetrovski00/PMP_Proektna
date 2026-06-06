package com.apetrovski.autoservicelog.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["licensePlate"])
    ]
)
data class CarEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String,
    val manufacturer: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val vin: String?,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long
)
