package com.apetrovski.autoservicelog.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "worksheet_entries",
    indices = [
        Index(value = ["worksheetId"])
    ]
)
data class WorksheetEntryEntity(
    @PrimaryKey val id: String,
    val worksheetId: String,
    val description: String,
    val imageUrl: String?,
    val createdAt: Long
)
