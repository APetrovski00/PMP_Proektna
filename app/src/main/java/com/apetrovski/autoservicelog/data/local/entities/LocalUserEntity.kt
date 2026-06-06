package com.apetrovski.autoservicelog.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_users")
data class LocalUserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String?,
    val role: String,
    val photoUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)
