package com.apetrovski.autoservicelog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorksheetEntryDao {
    @Query("SELECT * FROM worksheet_entries WHERE worksheetId = :worksheetId ORDER BY createdAt ASC")
    fun observeEntriesForWorksheet(worksheetId: String): Flow<List<WorksheetEntryEntity>>

    @Upsert
    suspend fun saveEntry(entry: WorksheetEntryEntity)

    @Query("DELETE FROM worksheet_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: String)

    @Query("DELETE FROM worksheet_entries WHERE worksheetId = :worksheetId")
    suspend fun deleteEntriesForWorksheet(worksheetId: String)
}
