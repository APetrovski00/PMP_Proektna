package com.apetrovski.autoservicelog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apetrovski.autoservicelog.data.local.entities.LocalUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<LocalUserEntity?>

    @Query("SELECT * FROM local_users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): LocalUserEntity?

    @Upsert
    suspend fun saveUser(user: LocalUserEntity)

    @Query("DELETE FROM local_users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}
