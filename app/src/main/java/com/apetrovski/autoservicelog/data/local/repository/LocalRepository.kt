package com.apetrovski.autoservicelog.data.local.repository

import com.apetrovski.autoservicelog.data.local.dao.CarDao
import com.apetrovski.autoservicelog.data.local.dao.LocalUserDao
import com.apetrovski.autoservicelog.data.local.dao.WorksheetDao
import com.apetrovski.autoservicelog.data.local.dao.WorksheetEntryDao
import com.apetrovski.autoservicelog.data.local.entities.CarEntity
import com.apetrovski.autoservicelog.data.local.entities.LocalUserEntity
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntity
import com.apetrovski.autoservicelog.data.local.entities.WorksheetEntryEntity
import kotlinx.coroutines.flow.Flow

class LocalRepository(
    private val localUserDao: LocalUserDao,
    private val carDao: CarDao,
    private val worksheetDao: WorksheetDao,
    private val worksheetEntryDao: WorksheetEntryDao
) {
    fun observeUser(uid: String): Flow<LocalUserEntity?> {
        return localUserDao.observeUser(uid)
    }

    suspend fun getUser(uid: String): LocalUserEntity? {
        return localUserDao.getUser(uid)
    }

    suspend fun saveUser(user: LocalUserEntity) {
        localUserDao.saveUser(user)
    }

    suspend fun deleteUser(uid: String) {
        localUserDao.deleteUser(uid)
    }

    fun observeCarsForOwner(ownerId: String): Flow<List<CarEntity>> {
        return carDao.observeCarsForOwner(ownerId)
    }

    fun observeCar(carId: String): Flow<CarEntity?> {
        return carDao.observeCar(carId)
    }

    suspend fun findCarByLicensePlate(licensePlate: String): CarEntity? {
        return carDao.findByLicensePlate(licensePlate)
    }

    suspend fun saveCar(car: CarEntity) {
        carDao.saveCar(car)
    }

    suspend fun deleteCar(carId: String) {
        carDao.deleteCar(carId)
    }

    fun observeWorksheetsForCar(carId: String): Flow<List<WorksheetEntity>> {
        return worksheetDao.observeWorksheetsForCar(carId)
    }

    fun observeWorksheetsForOwner(ownerId: String): Flow<List<WorksheetEntity>> {
        return worksheetDao.observeWorksheetsForOwner(ownerId)
    }

    fun observeWorksheetsForMechanic(mechanicId: String): Flow<List<WorksheetEntity>> {
        return worksheetDao.observeWorksheetsForMechanic(mechanicId)
    }

    fun observeWorksheet(worksheetId: String): Flow<WorksheetEntity?> {
        return worksheetDao.observeWorksheet(worksheetId)
    }

    suspend fun saveWorksheet(worksheet: WorksheetEntity) {
        worksheetDao.saveWorksheet(worksheet)
    }

    suspend fun updateWorksheetStatus(worksheetId: String, status: String, finishedAt: Long?) {
        worksheetDao.updateStatus(worksheetId, status, finishedAt)
    }

    suspend fun deleteWorksheet(worksheetId: String) {
        worksheetEntryDao.deleteEntriesForWorksheet(worksheetId)
        worksheetDao.deleteWorksheet(worksheetId)
    }

    fun observeEntriesForWorksheet(worksheetId: String): Flow<List<WorksheetEntryEntity>> {
        return worksheetEntryDao.observeEntriesForWorksheet(worksheetId)
    }

    suspend fun saveWorksheetEntry(entry: WorksheetEntryEntity) {
        worksheetEntryDao.saveEntry(entry)
    }

    suspend fun deleteWorksheetEntry(entryId: String) {
        worksheetEntryDao.deleteEntry(entryId)
    }
}
