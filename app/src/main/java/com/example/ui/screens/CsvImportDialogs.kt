package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BulkImportOutcome
import com.example.data.VehicleRecord
import com.example.ui.components.ExpiredRedAlertBanner
import com.example.ui.components.PassStickerCard
import com.example.ui.theme.LocalGateStatusColors
import com.example.util.CsvParseResult
import com.example.util.DateUtils
import com.example.util.PassStatus
import kotlin.math.abs

@Composable
fun CsvGuideDialog(
  onPickCsvClick: () -> Unit,
  onSaveTemplateClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.UploadFile,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(text = "Bulk CSV Driver Upload", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
      ) {
        Text(
          text =
            "Select a .csv file to register or update multiple drivers in the offline SQLite database.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "Expected CSV Columns:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text =
              "driver_name, license_plate, driver_license_number, phone_number, station_location, vehicle_type, registration_date, expiration_date, internal_reg_code",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(10.dp),
          )
        }
        Text(
          text =
            "If expiration_date is left blank, it is automatically set to 1 year after registration_date. If internal_reg_code is blank, one is generated automatically.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onPickCsvClick,
        modifier = Modifier.testTag("pick_csv_file_confirm"),
      ) {
        Text("Choose CSV File")
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onSaveTemplateClick) {
          Text("Save Template")
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    },
  )
}

@Composable
fun CsvPreviewImportDialog(
  parseResult: CsvParseResult,
  onConfirmImport: () -> Unit,
  onDismiss: () -> Unit,
) {
  val statusColors = LocalGateStatusColors.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = "CSV Import Preview", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
      ) {
        Text(
          text =
            "Ready to import ${parseResult.validRows.size} valid record(s) from ${parseResult.totalDataRows} row(s).",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )

        if (parseResult.errors.isNotEmpty()) {
          Surface(
            color = statusColors.expiringBg,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(10.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Warning,
                  contentDescription = null,
                  tint = statusColors.expiringFg,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${parseResult.errors.size} skipped row(s)",
                  style = MaterialTheme.typography.labelLarge,
                  color = statusColors.expiringFg,
                )
              }
              parseResult.errors.take(5).forEach { err ->
                Text(
                  text = "• $err",
                  style = MaterialTheme.typography.bodySmall,
                  color = statusColors.expiringFg,
                )
              }
            }
          }
        }

        parseResult.validRows.take(5).forEach { row ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "${row.licensePlate} — ${row.driverName}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "${row.vehicleType} • ${row.driverLicenseNumber} • Exp ${DateUtils.formatDisplayDate(row.expirationDateIso)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        if (parseResult.validRows.size > 5) {
          Text(
            text = "+ ${parseResult.validRows.size - 5} more rows",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmImport,
        enabled = parseResult.validRows.isNotEmpty(),
        modifier = Modifier.testTag("confirm_csv_import_button"),
      ) {
        Text("Import ${parseResult.validRows.size} Records")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun CsvOutcomeDialog(
  outcome: BulkImportOutcome,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = LocalGateStatusColors.current.validAccent,
      )
    },
    title = { Text("Bulk Import Complete", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("New drivers added: ${outcome.insertedCount}")
        Text("Existing drivers updated: ${outcome.updatedCount}")
        if (outcome.parseErrors.isNotEmpty()) {
          Text(
            text = "Skipped rows: ${outcome.parseErrors.size}",
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Done") }
    },
  )
}

@Composable
fun VehicleDetailDialog(
  record: VehicleRecord,
  onDismiss: () -> Unit,
  onRenew: (VehicleRecord) -> Unit,
  onEdit: (VehicleRecord) -> Unit,
  onDelete: (VehicleRecord) -> Unit,
) {
  var confirmDelete by remember { mutableStateOf(false) }
  val status = record.status()
  val days = record.daysRemaining()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = "Vehicle Registration Pass", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
      ) {
        if (status == PassStatus.EXPIRED) {
          ExpiredRedAlertBanner(
            daysOverdue = abs(days),
            onRenewClick = { onRenew(record) },
          )
        }

        PassStickerCard(record = record)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          InfoLine("Driver", record.driverName)
          InfoLine("License Plate", record.licensePlate)
          InfoLine("Driver License", record.driverLicenseNumber)
          InfoLine("Phone", record.phoneNumber)
          InfoLine("Station", record.stationLocation)
          InfoLine("Vehicle Type", record.vehicleType)
          InfoLine("Registered", DateUtils.formatDisplayDate(record.registrationDateIso))
          InfoLine("Expires", DateUtils.formatDisplayDate(record.expirationDateIso))
          InfoLine("Internal Code", record.internalRegCode)
        }

        if (confirmDelete) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(10.dp),
            ) {
              Text(
                text = "Delete ${record.licensePlate} from local SQLite database?",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    onDelete(record)
                    onDismiss()
                  },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                  Text("Delete")
                }
                TextButton(onClick = { confirmDelete = false }) {
                  Text("Keep")
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { onEdit(record) }) {
          Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Edit")
        }
        Button(onClick = { onRenew(record) }) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Renew")
        }
      }
    },
    dismissButton = {
      Row {
        TextButton(
          onClick = { confirmDelete = true },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
          Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete")
        }
        TextButton(onClick = onDismiss) { Text("Close") }
      }
    },
  )
}

@Composable
private fun InfoLine(label: String, value: String) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
