package com.apetrovski.autoservicelog.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "worksheets",
    indices = [
        Index(value = ["carId"]),
        Index(value = ["ownerId"]),
        Index(value = ["mechanicId"])
    ]
)
data class WorksheetEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val ownerId: String,
    val mechanicId: String,
    val mechanicName: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val summary: String?
)
