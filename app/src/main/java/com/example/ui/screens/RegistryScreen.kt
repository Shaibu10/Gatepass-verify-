package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VehicleRecord
import com.example.ui.DateFilterMode
import com.example.ui.RegistryFilterState
import com.example.ui.RegistryStats
import com.example.ui.SearchFieldFilter
import com.example.ui.SortOrder
import com.example.ui.StatusFilter
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalGateStatusColors
import com.example.util.DateUtils
import com.example.util.PassStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryScreen(
  records: List<VehicleRecord>,
  stats: RegistryStats,
  filterState: RegistryFilterState,
  onSearchQueryChange: (String) -> Unit,
  onSearchFieldChange: (SearchFieldFilter) -> Unit,
  onStatusFilterChange: (StatusFilter) -> Unit,
  onExpirationDateChange: (String?) -> Unit,
  onDateFilterModeChange: (DateFilterMode) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onClearFilters: () -> Unit,
  onRecordClick: (VehicleRecord) -> Unit,
  onAddDriverClick: () -> Unit,
  onImportCsvClick: () -> Unit,
  onExportTemplateClick: () -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  var showExpDatePicker by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }

  if (showExpDatePicker) {
    val initialMillis =
      filterState.expirationDateFilterIso?.let { DateUtils.isoToEpochMillisUtc(it) }
        ?: DateUtils.isoToEpochMillisUtc(DateUtils.todayPlusOneYearIso())
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showExpDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              onExpirationDateChange(DateUtils.epochMillisToIsoUtc(millis))
            }
            showExpDatePicker = false
          },
          modifier = Modifier.testTag("exp_filter_confirm"),
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExpDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize().padding(contentPadding),
  ) {
    Column(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
    ) {
      // Search & CSV Bulk Tools Header
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = onImportCsvClick,
            modifier = Modifier.weight(1f).testTag("import_csv_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.UploadFile,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bulk CSV Upload", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }

          OutlinedButton(
            onClick = onExportTemplateClick,
            modifier = Modifier.testTag("export_csv_template_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Download,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("CSV Template")
          }
        }

        // Search input
        OutlinedTextField(
          value = filterState.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = {
            Text(
              when (filterState.searchField) {
                SearchFieldFilter.ALL -> "Search license no., plate, driver, or code"
                SearchFieldFilter.LICENSE_NUMBER -> "Filter by driver license number..."
                SearchFieldFilter.PLATE -> "Filter by vehicle license plate..."
                SearchFieldFilter.DRIVER_NAME -> "Filter by driver name..."
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
          },
          trailingIcon = {
            if (filterState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("registry_search_input"),
        )

        // Search scope pills (explicit License No. filter!)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
          SearchFieldFilter.entries.forEach { field ->
            FilterChip(
              selected = filterState.searchField == field,
              onClick = { onSearchFieldChange(field) },
              label = { Text(field.label) },
              modifier = Modifier.testTag("search_scope_${field.name.lowercase()}"),
            )
          }
        }

        // Expiration Date Filter + Sort Row
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
          if (filterState.expirationDateFilterIso == null) {
            OutlinedButton(
              onClick = { showExpDatePicker = true },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("filter_expiration_date_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Filter Expiration Date")
            }
          } else {
            InputChip(
              selected = true,
              onClick = { showExpDatePicker = true },
              label = {
                Text(
                  "${filterState.dateFilterMode.label}: ${DateUtils.formatDisplayDate(filterState.expirationDateFilterIso)}"
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Filled.CalendarMonth,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = { onExpirationDateChange(null) },
                  modifier = Modifier.size(24.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear expiration filter",
                    modifier = Modifier.size(16.dp),
                  )
                }
              },
              modifier = Modifier.testTag("active_expiration_filter_chip"),
            )

            FilterChip(
              selected = filterState.dateFilterMode == DateFilterMode.ON_OR_BEFORE,
              onClick = {
                val next =
                  if (filterState.dateFilterMode == DateFilterMode.EXACT_DATE) {
                    DateFilterMode.ON_OR_BEFORE
                  } else {
                    DateFilterMode.EXACT_DATE
                  }
                onDateFilterModeChange(next)
              },
              label = {
                Text(
                  if (filterState.dateFilterMode == DateFilterMode.EXACT_DATE) {
                    "Switch to On/Before"
                  } else {
                    "On or Before"
                  }
                )
              },
            )
          }

          Box {
            OutlinedButton(
              onClick = { showSortMenu = true },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("sort_order_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(filterState.sortOrder.label)
            }
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false },
            ) {
              SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                  text = { Text(order.label) },
                  onClick = {
                    onSortOrderChange(order)
                    showSortMenu = false
                  },
                )
              }
            }
          }
        }

        // Status filter chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
          StatusFilter.entries.forEach { status ->
            val count =
              when (status) {
                StatusFilter.ALL -> stats.total
                StatusFilter.VALID -> stats.valid
                StatusFilter.EXPIRING_SOON -> stats.expiringSoon
                StatusFilter.EXPIRED -> stats.expired
              }
            FilterChip(
              selected = filterState.statusFilter == status,
              onClick = { onStatusFilterChange(status) },
              label = { Text("${status.label} ($count)") },
              modifier = Modifier.testTag("status_filter_${status.name.lowercase()}"),
            )
          }
        }
      }

      // Records List or Empty State
      if (records.isEmpty()) {
        val hasActiveFilters =
          filterState.searchQuery.isNotBlank() ||
            filterState.expirationDateFilterIso != null ||
            filterState.statusFilter != StatusFilter.ALL

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.DirectionsCar,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text =
                if (hasActiveFilters) "No matching vehicles found"
                else "No vehicles registered yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                if (hasActiveFilters) {
                  "Try clearing your license number or expiration date filters."
                } else {
                  "Register drivers individually or import a CSV spreadsheet for bulk offline verification."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (hasActiveFilters) {
              OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier.testTag("clear_all_filters_button"),
              ) {
                Text("Clear All Filters")
              }
            } else {
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = onAddDriverClick,
                  modifier = Modifier.testTag("empty_register_driver_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Register Driver")
                }
                OutlinedButton(onClick = onImportCsvClick) {
                  Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Import CSV")
                }
              }
            }
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize().testTag("registry_list"),
        ) {
          items(records, key = { it.id }) { record ->
            VehicleRegistryItemCard(
              record = record,
              onClick = { onRecordClick(record) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VehicleRegistryItemCard(
  record: VehicleRecord,
  onClick: () -> Unit,
) {
  val status = record.status()
  val statusColors = LocalGateStatusColors.current
  val accentColor =
    when (status) {
      PassStatus.VALID -> statusColors.validAccent
      PassStatus.EXPIRING_SOON -> statusColors.expiringAccent
      PassStatus.EXPIRED -> statusColors.expiredAccent
    }
  val containerColor =
    if (status == PassStatus.EXPIRED) {
      statusColors.expiredBg.copy(alpha = 0.45f)
    } else {
      MaterialTheme.colorScheme.surfaceContainerLow
    }

  Card(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border =
      BorderStroke(
        width = if (status == PassStatus.EXPIRED) 1.5.dp else 1.dp,
        color =
          if (status == PassStatus.EXPIRED) accentColor.copy(alpha = 0.7f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().testTag("vehicle_card_${record.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
      Box(
        modifier =
          Modifier
            .width(6.dp)
            .fillMaxHeight()
            .background(accentColor),
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f).padding(14.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = record.licensePlate,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          StatusBadgePill(status = status)
        }

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = record.driverName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = record.vehicleType,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.Badge,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = record.driverLicenseNumber,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = record.stationLocation,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = record.internalRegCode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Exp: ${DateUtils.formatDisplayDate(record.expirationDateIso)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color =
              if (status == PassStatus.EXPIRED) statusColors.expiredAccent
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
