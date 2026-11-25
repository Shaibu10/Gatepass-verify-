package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BulkImportOutcome
import com.example.data.GatePassDatabase
import com.example.data.VehicleRecord
import com.example.data.VehicleRepository
import com.example.data.VerificationResult
import com.example.util.CsvDriverImporter
import com.example.util.CsvParseResult
import com.example.util.DateUtils
import com.example.util.PassStatus
import com.example.util.QrStickerParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StatusFilter(val label: String) {
  ALL("All"),
  VALID("Valid"),
  EXPIRING_SOON("Expiring"),
  EXPIRED("Expired"),
}

enum class DateFilterMode(val label: String) {
  EXACT_DATE("Exact Date"),
  ON_OR_BEFORE("On or Before"),
}

enum class SortOrder(val label: String) {
  SOONEST_EXPIRY("Soonest Expiry"),
  RECENTLY_UPDATED("Recently Added"),
  PLATE_ASC("Plate A–Z"),
}

enum class SearchFieldFilter(val label: String) {
  ALL("All Fields"),
  LICENSE_NUMBER("License No."),
  PLATE("Plate"),
  DRIVER_NAME("Driver"),
}

data class RegistryStats(
  val total: Int = 0,
  val valid: Int = 0,
  val expiringSoon: Int = 0,
  val expired: Int = 0,
)

data class RecentScanEntry(
  val rawQuery: String,
  val label: String,
  val isFound: Boolean,
  val status: PassStatus?,
  val timestampMillis: Long = System.currentTimeMillis(),
)

data class RegistryFilterState(
  val searchQuery: String = "",
  val searchField: SearchFieldFilter = SearchFieldFilter.ALL,
  val statusFilter: StatusFilter = StatusFilter.ALL,
  val expirationDateFilterIso: String? = null,
  val dateFilterMode: DateFilterMode = DateFilterMode.EXACT_DATE,
  val sortOrder: SortOrder = SortOrder.SOONEST_EXPIRY,
)

data class RegistrationPrefill(
  val existingRecordId: Long? = null,
  val driverName: String = "",
  val licensePlate: String = "",
  val driverLicenseNumber: String = "",
  val phoneNumber: String = "",
  val stationLocation: String = "",
  val vehicleType: String = "Motorbike",
  val registrationDateIso: String = DateUtils.todayIso(),
  val expirationDateIso: String = DateUtils.todayPlusOneYearIso(),
  val internalRegCode: String = DateUtils.generateInternalRegCode("UMAT", DateUtils.todayIso()),
)

class GatePassViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: VehicleRepository by lazy {
    val db = GatePassDatabase.getInstance(application)
    VehicleRepository(db.vehicleDao())
  }

  val allRecords: StateFlow<List<VehicleRecord>> =
    repository.allRecords.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _filterState = MutableStateFlow(RegistryFilterState())
  val filterState: StateFlow<RegistryFilterState> = _filterState.asStateFlow()

  val registryStats: StateFlow<RegistryStats> =
    allRecords
      .combine(MutableStateFlow(Unit)) { records, _ ->
        var valid = 0
        var expiring = 0
        var expired = 0
        val today = DateUtils.todayIso()
        for (record in records) {
          when (record.status(today)) {
            PassStatus.VALID -> valid++
            PassStatus.EXPIRING_SOON -> expiring++
            PassStatus.EXPIRED -> expired++
          }
        }
        RegistryStats(
          total = records.size,
          valid = valid,
          expiringSoon = expiring,
          expired = expired,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RegistryStats(),
      )

  val filteredRecords: StateFlow<List<VehicleRecord>> =
    combine(allRecords, _filterState) { records, filter ->
      val today = DateUtils.todayIso()
      val query = filter.searchQuery.trim()
      val queryLower = query.lowercase()
      val queryCompact = QrStickerParser.compactPlate(query)

      records
        .filter { record ->
          val status = record.status(today)
          val matchesStatus =
            when (filter.statusFilter) {
              StatusFilter.ALL -> true
              StatusFilter.VALID -> status == PassStatus.VALID
              StatusFilter.EXPIRING_SOON -> status == PassStatus.EXPIRING_SOON
              StatusFilter.EXPIRED -> status == PassStatus.EXPIRED
            }

          val matchesExpDate =
            filter.expirationDateFilterIso?.let { targetIso ->
              when (filter.dateFilterMode) {
                DateFilterMode.EXACT_DATE -> record.expirationDateIso == targetIso
                DateFilterMode.ON_OR_BEFORE -> record.expirationDateIso <= targetIso
              }
            } ?: true

          val matchesQuery =
            if (query.isEmpty()) {
              true
            } else {
              when (filter.searchField) {
                SearchFieldFilter.ALL -> {
                  record.driverLicenseNumber.lowercase().contains(queryLower) ||
                    record.driverLicenseNormalized.contains(queryCompact) ||
                    record.licensePlate.lowercase().contains(queryLower) ||
                    record.licensePlateNormalized.contains(queryCompact) ||
                    record.driverName.lowercase().contains(queryLower) ||
                    record.internalRegCode.lowercase().contains(queryLower) ||
                    record.stationLocation.lowercase().contains(queryLower)
                }
                SearchFieldFilter.LICENSE_NUMBER -> {
                  record.driverLicenseNumber.lowercase().contains(queryLower) ||
                    record.driverLicenseNormalized.contains(queryCompact)
                }
                SearchFieldFilter.PLATE -> {
                  record.licensePlate.lowercase().contains(queryLower) ||
                    record.licensePlateNormalized.contains(queryCompact)
                }
                SearchFieldFilter.DRIVER_NAME -> {
                  record.driverName.lowercase().contains(queryLower)
                }
              }
            }

          matchesStatus && matchesExpDate && matchesQuery
        }
        .let { filtered ->
          when (filter.sortOrder) {
            SortOrder.SOONEST_EXPIRY -> filtered.sortedBy { it.expirationDateIso }
            SortOrder.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAtMillis }
            SortOrder.PLATE_ASC -> filtered.sortedBy { it.licensePlate }
          }
        }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
  val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

  private val _recentScans = MutableStateFlow<List<RecentScanEntry>>(emptyList())
  val recentScans: StateFlow<List<RecentScanEntry>> = _recentScans.asStateFlow()

  private val _pendingCsvParse = MutableStateFlow<CsvParseResult?>(null)
  val pendingCsvParse: StateFlow<CsvParseResult?> = _pendingCsvParse.asStateFlow()

  private val _lastImportOutcome = MutableStateFlow<BulkImportOutcome?>(null)
  val lastImportOutcome: StateFlow<BulkImportOutcome?> = _lastImportOutcome.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private var lastScannedRaw: String? = null
  private var lastScannedAtMillis: Long = 0L

  fun verifyScannedCode(rawCode: String, fromLiveCamera: Boolean = false) {
    val trimmed = rawCode.trim()
    if (trimmed.isEmpty()) return

    val now = System.currentTimeMillis()
    if (fromLiveCamera && trimmed == lastScannedRaw && (now - lastScannedAtMillis) < 2500L) {
      return
    }
    lastScannedRaw = trimmed
    lastScannedAtMillis = now

    viewModelScope.launch {
      val result = repository.verifyQrOrQuery(trimmed)
      _verificationResult.value = result
      val entry =
        when (result) {
          is VerificationResult.Verified ->
            RecentScanEntry(
              rawQuery = trimmed,
              label = "${result.record.licensePlate} • ${result.record.driverName}",
              isFound = true,
              status = result.status,
            )
          is VerificationResult.NotFound ->
            RecentScanEntry(
              rawQuery = trimmed,
              label = result.parsedQr.licensePlate ?: trimmed,
              isFound = false,
              status = null,
            )
        }
      _recentScans.update { current ->
        (listOf(entry) + current.filterNot { it.rawQuery.equals(trimmed, ignoreCase = true) })
          .take(6)
      }
    }
  }

  fun clearVerificationResult() {
    _verificationResult.value = null
    lastScannedRaw = null
  }

  fun saveDriverRecord(
    existingId: Long?,
    driverName: String,
    licensePlate: String,
    registrationDateIso: String,
    expirationDateIso: String,
    driverLicenseNumber: String,
    phoneNumber: String,
    stationLocation: String,
    internalRegCode: String,
    vehicleType: String,
    onSaved: (Long) -> Unit = {},
  ) {
    viewModelScope.launch {
      val id =
        repository.saveRecord(
          existingId = existingId,
          driverName = driverName,
          licensePlate = licensePlate,
          registrationDateIso = registrationDateIso,
          expirationDateIso = expirationDateIso,
          driverLicenseNumber = driverLicenseNumber,
          phoneNumber = phoneNumber,
          stationLocation = stationLocation,
          internalRegCode = internalRegCode,
          vehicleType = vehicleType,
        )
      // Refresh active verification result if it matches this plate or code
      val currentResult = _verificationResult.value
      if (currentResult != null) {
        val currentQuery =
          when (currentResult) {
            is VerificationResult.Verified -> currentResult.parsedQr.rawText
            is VerificationResult.NotFound -> currentResult.query
          }
        val refreshed = repository.verifyQrOrQuery(currentQuery)
        if (refreshed is VerificationResult.Verified) {
          _verificationResult.value = refreshed
        }
      }
      _snackbarMessage.value =
        if (existingId != null) "Driver registration updated" else "Driver registered successfully"
      onSaved(id)
    }
  }

  fun renewRecordOneYear(record: VehicleRecord) {
    viewModelScope.launch {
      val updated = repository.renewOneYear(record)
      val currentResult = _verificationResult.value
      if (currentResult is VerificationResult.Verified && currentResult.record.id == record.id) {
        _verificationResult.value =
          currentResult.copy(
            record = updated,
            status = updated.status(),
            daysRemaining = updated.daysRemaining(),
          )
      }
      _snackbarMessage.value =
        "Pass renewed until ${DateUtils.formatDisplayDate(updated.expirationDateIso)}"
    }
  }

  fun deleteRecord(record: VehicleRecord) {
    viewModelScope.launch {
      repository.deleteById(record.id)
      val currentResult = _verificationResult.value
      if (currentResult is VerificationResult.Verified && currentResult.record.id == record.id) {
        _verificationResult.value = null
      }
      _snackbarMessage.value = "Removed ${record.licensePlate} from local database"
    }
  }

  fun updateSearchQuery(query: String) {
    _filterState.update { it.copy(searchQuery = query) }
  }

  fun updateSearchField(field: SearchFieldFilter) {
    _filterState.update { it.copy(searchField = field) }
  }

  fun updateStatusFilter(status: StatusFilter) {
    _filterState.update { it.copy(statusFilter = status) }
  }

  fun updateExpirationDateFilter(isoDate: String?) {
    _filterState.update { it.copy(expirationDateFilterIso = isoDate) }
  }

  fun updateDateFilterMode(mode: DateFilterMode) {
    _filterState.update { it.copy(dateFilterMode = mode) }
  }

  fun updateSortOrder(sortOrder: SortOrder) {
    _filterState.update { it.copy(sortOrder = sortOrder) }
  }

  fun clearAllFilters() {
    _filterState.value = RegistryFilterState()
  }

  fun parseCsvFromUri(contentResolver: ContentResolver, uri: Uri) {
    viewModelScope.launch {
      val parseResult =
        withContext(Dispatchers.IO) {
          runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
              CsvDriverImporter.parseCsv(stream)
            } ?: CsvParseResult(emptyList(), listOf("Unable to open selected file."), 0)
          }.getOrElse { ex ->
            CsvParseResult(
              emptyList(),
              listOf("Failed to read CSV: ${ex.localizedMessage ?: "Unknown error"}"),
              0,
            )
          }
        }
      _pendingCsvParse.value = parseResult
      _lastImportOutcome.value = null
    }
  }

  fun confirmCsvImport() {
    val parsed = _pendingCsvParse.value ?: return
    if (parsed.validRows.isEmpty()) return
    viewModelScope.launch {
      val outcome =
        repository.importRows(
          rows = parsed.validRows,
          parseErrors = parsed.errors,
          totalDataRows = parsed.totalDataRows,
        )
      _pendingCsvParse.value = null
      _lastImportOutcome.value = outcome
      _snackbarMessage.value =
        "Imported ${outcome.insertedCount} new, ${outcome.updatedCount} updated"
    }
  }

  fun dismissCsvDialogs() {
    _pendingCsvParse.value = null
    _lastImportOutcome.value = null
  }

  fun writeSampleCsvTemplate(contentResolver: ContentResolver, uri: Uri) {
    viewModelScope.launch {
      val ok =
        withContext(Dispatchers.IO) {
          runCatching {
            contentResolver.openOutputStream(uri)?.use { out ->
              out.write(CsvDriverImporter.SAMPLE_CSV_CONTENT.toByteArray(Charsets.UTF_8))
              out.flush()
            }
            true
          }.getOrDefault(false)
        }
      _snackbarMessage.value =
        if (ok) "Sample CSV template saved" else "Could not write CSV template"
    }
  }

  fun showMessage(message: String) {
    _snackbarMessage.value = message
  }

  fun consumeSnackbar() {
    _snackbarMessage.value = null
  }
}
