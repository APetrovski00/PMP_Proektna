package com.apetrovski.autoservicelog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorksheetDao {
    @Query("SELECT * FROM worksheets WHERE carId = :carId ORDER BY startedAt DESC")
    fun observeWorksheetsForCar(carId: String): Flow<List<WorksheetEntity>>

    @Query("SELECT * FROM worksheets WHERE ownerId = :ownerId ORDER BY startedAt DESC")
    fun observeWorksheetsForOwner(ownerId: String): Flow<List<WorksheetEntity>>

    @Query("SELECT * FROM worksheets WHERE mechanicId = :mechanicId ORDER BY startedAt DESC")
    fun observeWorksheetsForMechanic(mechanicId: String): Flow<List<WorksheetEntity>>

    @Query("SELECT * FROM worksheets WHERE id = :worksheetId LIMIT 1")
    fun observeWorksheet(worksheetId: String): Flow<WorksheetEntity?>

    @Upsert
    suspend fun saveWorksheet(worksheet: WorksheetEntity)

    @Query("UPDATE worksheets SET status = :status, finishedAt = :finishedAt WHERE id = :worksheetId")
    suspend fun updateStatus(worksheetId: String, status: String, finishedAt: Long?)

    @Query("DELETE FROM worksheets WHERE id = :worksheetId")
    suspend fun deleteWorksheet(worksheetId: String)
}
