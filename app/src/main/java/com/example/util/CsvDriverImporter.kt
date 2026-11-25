package com.example.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

data class ImportedDriverRow(
  val driverName: String,
  val licensePlate: String,
  val driverLicenseNumber: String,
  val phoneNumber: String,
  val stationLocation: String,
  val vehicleType: String,
  val registrationDateIso: String,
  val expirationDateIso: String,
  val internalRegCode: String,
)

data class CsvParseResult(
  val validRows: List<ImportedDriverRow>,
  val errors: List<String>,
  val totalDataRows: Int,
)

object CsvDriverImporter {

  const val SAMPLE_CSV_CONTENT =
    "driver_name,license_plate,driver_license_number,phone_number,station_location,vehicle_type,registration_date,expiration_date,internal_reg_code\n" +
      "Shaibu Sulemana,GR 9206-19,DL-GH-884210,+233244001122,UMaT Essikado Campus,Motorbike,2026-11-15,2027-11-15,UMAT-SRID-2026\n" +
      "Abena Mensah,WR 4512-23,DL-GH-551904,+233208112233,Main Security Gate,Sedan,2026-03-10,,UMAT-MAIN-2026\n"

  fun parseCsv(inputStream: InputStream): CsvParseResult {
    val lines =
      BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
        reader.readLines()
      }
    return parseCsvLines(lines)
  }

  fun parseCsvText(csvText: String): CsvParseResult =
    parseCsvLines(csvText.lines())

  fun parseCsvLines(rawLines: List<String>): CsvParseResult {
    val nonEmptyLines = rawLines.map { it.trimEnd('\r') }.filter { it.isNotBlank() }
    if (nonEmptyLines.isEmpty()) {
      return CsvParseResult(emptyList(), listOf("The CSV file is empty."), 0)
    }

    val firstRow = parseCsvRow(nonEmptyLines.first())
    val headerMap = detectHeaderMap(firstRow)
    val startIndex = if (headerMap != null) 1 else 0
    val resolvedMap = headerMap ?: defaultPositionalMap()

    val validRows = mutableListOf<ImportedDriverRow>()
    val errors = mutableListOf<String>()
    var dataRowCount = 0

    for (i in startIndex until nonEmptyLines.size) {
      val lineNumber = i + 1
      val cells = parseCsvRow(nonEmptyLines[i])
      if (cells.all { it.isBlank() }) continue
      dataRowCount++

      fun cell(field: CsvField): String =
        resolvedMap[field]?.let { idx -> cells.getOrNull(idx)?.trim().orEmpty() }.orEmpty()

      val driverName = cell(CsvField.DRIVER_NAME)
      val licensePlate = cell(CsvField.LICENSE_PLATE)
      val driverLicense = cell(CsvField.DRIVER_LICENSE)
      val phone = cell(CsvField.PHONE)
      val station = cell(CsvField.STATION)
      val vehicleType = cell(CsvField.VEHICLE_TYPE).ifBlank { "Sedan" }
      val rawRegDate = cell(CsvField.REG_DATE)
      val rawExpDate = cell(CsvField.EXP_DATE)
      val rawInternalCode = cell(CsvField.INTERNAL_CODE)

      if (driverName.isBlank() || licensePlate.isBlank()) {
        errors.add("Row $lineNumber: Missing driver name or vehicle license plate.")
        continue
      }

      val regIso =
        if (rawRegDate.isBlank()) {
          DateUtils.todayIso()
        } else {
          DateUtils.parseFlexibleDateToIso(rawRegDate)
            ?: run {
              errors.add("Row $lineNumber: Unrecognized registration date '$rawRegDate'.")
              continue
            }
        }

      val expIso =
        if (rawExpDate.isBlank()) {
          DateUtils.addOneYearIso(regIso)
        } else {
          DateUtils.parseFlexibleDateToIso(rawExpDate)
            ?: run {
              errors.add("Row $lineNumber: Unrecognized expiration date '$rawExpDate'.")
              continue
            }
        }

      val finalStation = station.ifBlank { "Main Gate Station" }
      val finalCode =
        rawInternalCode.ifBlank {
          DateUtils.generateInternalRegCode(finalStation, regIso)
        }

      validRows.add(
        ImportedDriverRow(
          driverName = driverName,
          licensePlate = licensePlate,
          driverLicenseNumber = driverLicense.ifBlank { "N/A" },
          phoneNumber = phone.ifBlank { "N/A" },
          stationLocation = finalStation,
          vehicleType = vehicleType,
          registrationDateIso = regIso,
          expirationDateIso = expIso,
          internalRegCode = finalCode,
        )
      )
    }

    return CsvParseResult(
      validRows = validRows,
      errors = errors,
      totalDataRows = dataRowCount,
    )
  }

  private enum class CsvField {
    DRIVER_NAME,
    LICENSE_PLATE,
    DRIVER_LICENSE,
    PHONE,
    STATION,
    VEHICLE_TYPE,
    REG_DATE,
    EXP_DATE,
    INTERNAL_CODE,
  }

  private fun defaultPositionalMap(): Map<CsvField, Int> =
    mapOf(
      CsvField.DRIVER_NAME to 0,
      CsvField.LICENSE_PLATE to 1,
      CsvField.DRIVER_LICENSE to 2,
      CsvField.PHONE to 3,
      CsvField.STATION to 4,
      CsvField.VEHICLE_TYPE to 5,
      CsvField.REG_DATE to 6,
      CsvField.EXP_DATE to 7,
      CsvField.INTERNAL_CODE to 8,
    )

  private fun detectHeaderMap(columns: List<String>): Map<CsvField, Int>? {
    val map = mutableMapOf<CsvField, Int>()
    columns.forEachIndexed { index, rawCol ->
      val key = rawCol.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
      when (key) {
        "drivername", "driver", "name", "fullname" -> map[CsvField.DRIVER_NAME] = index
        "licenseplate", "plate", "platenumber", "vehicleplate", "vehicleregistration", "regnumber", "registrationnumber" ->
          map[CsvField.LICENSE_PLATE] = index
        "driverlicensenumber", "driverlicense", "licensenumber", "dlnumber", "license" ->
          map[CsvField.DRIVER_LICENSE] = index
        "phonenumber", "phone", "mobile", "contact", "tel" ->
          map[CsvField.PHONE] = index
        "stationlocation", "station", "location", "campus", "gate" ->
          map[CsvField.STATION] = index
        "vehicletype", "type", "vehicle", "category" ->
          map[CsvField.VEHICLE_TYPE] = index
        "registrationdate", "regdate", "issueddate", "dateofregistration" ->
          map[CsvField.REG_DATE] = index
        "expirationdate", "expdate", "expirydate", "validuntil" ->
          map[CsvField.EXP_DATE] = index
        "internalregcode", "internalregistrationcode", "internalcode", "regcode", "code", "passcode" ->
          map[CsvField.INTERNAL_CODE] = index
      }
    }
    return if (map.containsKey(CsvField.DRIVER_NAME) || map.containsKey(CsvField.LICENSE_PLATE)) {
      map
    } else {
      null
    }
  }

  private fun parseCsvRow(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      when {
        c == '"' -> {
          if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
            current.append('"')
            i++
          } else {
            inQuotes = !inQuotes
          }
        }
        c == ',' && !inQuotes -> {
          result.add(current.toString())
          current.clear()
        }
        else -> current.append(c)
      }
      i++
    }
    result.add(current.toString())
    return result
  }
}
