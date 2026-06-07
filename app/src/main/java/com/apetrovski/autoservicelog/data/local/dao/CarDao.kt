package com.apetrovski.autoservicelog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.apetrovski.autoservicelog.data.local.entities.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun observeCarsForOwner(ownerId: String): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    fun observeCar(carId: String): Flow<CarEntity?>

    @Query("SELECT * FROM cars WHERE licensePlate = :licensePlate LIMIT 1")
    suspend fun findByLicensePlate(licensePlate: String): CarEntity?

    @Upsert
    suspend fun saveCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteCar(carId: String)
}
