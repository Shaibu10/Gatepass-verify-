package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
  @Query("SELECT * FROM vehicle_records ORDER BY updatedAtMillis DESC")
  fun observeAllRecords(): Flow<List<VehicleRecord>>

  @Query("SELECT * FROM vehicle_records WHERE id = :id")
  fun observeById(id: Long): Flow<VehicleRecord?>

  @Query("SELECT * FROM vehicle_records WHERE id = :id LIMIT 1")
  suspend fun findById(id: Long): VehicleRecord?

  @Query(
    """
    SELECT * FROM vehicle_records
    WHERE licensePlateNormalized = :compactPlate
       OR UPPER(internalRegCode) = UPPER(:internalCode)
    LIMIT 1
    """
  )
  suspend fun findByPlateOrCode(compactPlate: String, internalCode: String): VehicleRecord?

  @Query("SELECT * FROM vehicle_records WHERE licensePlateNormalized = :compactPlate LIMIT 1")
  suspend fun findByCompactPlate(compactPlate: String): VehicleRecord?

  @Query("SELECT * FROM vehicle_records WHERE UPPER(internalRegCode) = UPPER(:internalCode) LIMIT 1")
  suspend fun findByInternalCode(internalCode: String): VehicleRecord?

  @Query("SELECT * FROM vehicle_records WHERE driverLicenseNormalized = :normalizedLicense LIMIT 1")
  suspend fun findByDriverLicense(normalizedLicense: String): VehicleRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrReplace(record: VehicleRecord): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(records: List<VehicleRecord>)

  @Update
  suspend fun update(record: VehicleRecord)

  @Query("DELETE FROM vehicle_records WHERE id = :id")
  suspend fun deleteById(id: Long)
}
