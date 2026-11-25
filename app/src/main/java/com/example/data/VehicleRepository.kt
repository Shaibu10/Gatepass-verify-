package com.example.data

import com.example.util.DateUtils
import com.example.util.ImportedDriverRow
import com.example.util.ParsedQrPayload
import com.example.util.PassStatus
import com.example.util.QrStickerParser
import kotlinx.coroutines.flow.Flow

sealed interface VerificationResult {
  data class Verified(
    val record: VehicleRecord,
    val status: PassStatus,
    val daysRemaining: Long,
    val parsedQr: ParsedQrPayload,
    val nameMismatchWarning: Boolean,
  ) : VerificationResult

  data class NotFound(
    val query: String,
    val parsedQr: ParsedQrPayload,
  ) : VerificationResult
}

data class BulkImportOutcome(
  val insertedCount: Int,
  val updatedCount: Int,
  val parseErrors: List<String>,
  val totalDataRows: Int,
)

class VehicleRepository(private val dao: VehicleDao) {

  val allRecords: Flow<List<VehicleRecord>> = dao.observeAllRecords()

  fun observeRecord(id: Long): Flow<VehicleRecord?> = dao.observeById(id)

  suspend fun verifyQrOrQuery(rawInput: String): VerificationResult {
    val parsed = QrStickerParser.parse(rawInput)
    val plateNorm = parsed.licensePlate?.let { QrStickerParser.compactPlate(it) }.orEmpty()
    val code = parsed.internalRegCode?.trim().orEmpty()

    val found =
      when {
        plateNorm.isNotEmpty() && code.isNotEmpty() -> {
          dao.findByInternalCode(code) ?: dao.findByCompactPlate(plateNorm)
        }
        code.isNotEmpty() -> dao.findByInternalCode(code)
        plateNorm.isNotEmpty() -> dao.findByCompactPlate(plateNorm)
        else -> null
      } ?: run {
        // Also allow direct lookup by driver license number if entered manually
        val licenseNorm = QrStickerParser.normalizeLicenseNumber(rawInput)
        if (licenseNorm.isNotEmpty()) dao.findByDriverLicense(licenseNorm) else null
      }

    if (found == null) {
      return VerificationResult.NotFound(
        query = rawInput.trim(),
        parsedQr = parsed,
      )
    }

    val status = found.status()
    val daysRemaining = found.daysRemaining()
    val nameMismatch =
      !parsed.driverName.isNullOrBlank() &&
        !found.driverName.trim().equals(parsed.driverName.trim(), ignoreCase = true)

    return VerificationResult.Verified(
      record = found,
      status = status,
      daysRemaining = daysRemaining,
      parsedQr = parsed,
      nameMismatchWarning = nameMismatch,
    )
  }

  suspend fun saveRecord(
    existingId: Long? = null,
    driverName: String,
    licensePlate: String,
    registrationDateIso: String,
    expirationDateIso: String = DateUtils.addOneYearIso(registrationDateIso),
    driverLicenseNumber: String,
    phoneNumber: String,
    stationLocation: String,
    internalRegCode: String,
    vehicleType: String,
  ): Long {
    val now = System.currentTimeMillis()
    val cleanPlate = QrStickerParser.normalizePlate(licensePlate)
    val cleanCode = internalRegCode.trim().uppercase()
    val existingByCode = dao.findByInternalCode(cleanCode)
    val targetId = existingId ?: existingByCode?.id ?: 0L
    val createdAt = existingByCode?.createdAtMillis ?: now

    val entity =
      VehicleRecord(
        id = targetId,
        driverName = driverName.trim(),
        licensePlate = cleanPlate,
        licensePlateNormalized = QrStickerParser.compactPlate(cleanPlate),
        registrationDateIso = registrationDateIso,
        expirationDateIso = expirationDateIso,
        driverLicenseNumber = driverLicenseNumber.trim().uppercase(),
        driverLicenseNormalized = QrStickerParser.normalizeLicenseNumber(driverLicenseNumber),
        phoneNumber = phoneNumber.trim(),
        stationLocation = stationLocation.trim(),
        internalRegCode = cleanCode,
        vehicleType = vehicleType.trim(),
        createdAtMillis = createdAt,
        updatedAtMillis = now,
      )
    return dao.insertOrReplace(entity)
  }

  suspend fun renewOneYear(record: VehicleRecord): VehicleRecord {
    val today = DateUtils.todayIso()
    val newExpiration = DateUtils.addOneYearIso(today)
    val updated =
      record.copy(
        registrationDateIso = today,
        expirationDateIso = newExpiration,
        updatedAtMillis = System.currentTimeMillis(),
      )
    dao.update(updated)
    return updated
  }

  suspend fun importRows(
    rows: List<ImportedDriverRow>,
    parseErrors: List<String>,
    totalDataRows: Int,
  ): BulkImportOutcome {
    var inserted = 0
    var updated = 0
    val now = System.currentTimeMillis()

    for (row in rows) {
      val cleanPlate = QrStickerParser.normalizePlate(row.licensePlate)
      val compactPlate = QrStickerParser.compactPlate(cleanPlate)
      val cleanCode = row.internalRegCode.trim().uppercase()

      val existing =
        dao.findByInternalCode(cleanCode) ?: dao.findByCompactPlate(compactPlate)

      val entity =
        VehicleRecord(
          id = existing?.id ?: 0L,
          driverName = row.driverName.trim(),
          licensePlate = cleanPlate,
          licensePlateNormalized = compactPlate,
          registrationDateIso = row.registrationDateIso,
          expirationDateIso = row.expirationDateIso,
          driverLicenseNumber = row.driverLicenseNumber.trim().uppercase(),
          driverLicenseNormalized = QrStickerParser.normalizeLicenseNumber(row.driverLicenseNumber),
          phoneNumber = row.phoneNumber.trim(),
          stationLocation = row.stationLocation.trim(),
          internalRegCode = existing?.internalRegCode ?: cleanCode,
          vehicleType = row.vehicleType.trim(),
          createdAtMillis = existing?.createdAtMillis ?: now,
          updatedAtMillis = now,
        )

      dao.insertOrReplace(entity)
      if (existing != null) updated++ else inserted++
    }

    return BulkImportOutcome(
      insertedCount = inserted,
      updatedCount = updated,
      parseErrors = parseErrors,
      totalDataRows = totalDataRows,
    )
  }

  suspend fun deleteById(id: Long) {
    dao.deleteById(id)
  }
}
