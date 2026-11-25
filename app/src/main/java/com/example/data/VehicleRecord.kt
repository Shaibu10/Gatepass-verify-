package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.DateUtils
import com.example.util.PassStatus
import com.example.util.QrStickerParser

@Entity(
  tableName = "vehicle_records",
  indices = [
    Index(value = ["licensePlateNormalized"]),
    Index(value = ["driverLicenseNormalized"]),
    Index(value = ["internalRegCode"], unique = true),
  ],
)
data class VehicleRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val driverName: String,
  val licensePlate: String,
  val licensePlateNormalized: String,
  val registrationDateIso: String,
  val expirationDateIso: String,
  val driverLicenseNumber: String,
  val driverLicenseNormalized: String,
  val phoneNumber: String,
  val stationLocation: String,
  val internalRegCode: String,
  val vehicleType: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun status(referenceIso: String = DateUtils.todayIso()): PassStatus =
    DateUtils.evaluatePassStatus(expirationDateIso, referenceIso)

  fun daysRemaining(referenceIso: String = DateUtils.todayIso()): Long =
    DateUtils.daysUntilExpiration(expirationDateIso, referenceIso)

  fun stickerQrPayload(): String =
    QrStickerParser.formatStickerQrText(
      licensePlate = licensePlate,
      driverName = driverName,
      internalRegCode = internalRegCode,
    )
}
