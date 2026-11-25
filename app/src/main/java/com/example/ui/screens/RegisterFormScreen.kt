package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.RegistrationPrefill
import com.example.util.DateUtils

private val VEHICLE_TYPE_PRESETS =
  listOf("Motorbike", "Sedan", "SUV", "Pickup", "Van", "Bus", "Truck")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterFormScreen(
  prefill: RegistrationPrefill,
  onNavigateBack: () -> Unit,
  onSave: (
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
  ) -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  var driverName by rememberSaveable { mutableStateOf(prefill.driverName) }
  var licensePlate by rememberSaveable { mutableStateOf(prefill.licensePlate) }
  var driverLicenseNumber by rememberSaveable { mutableStateOf(prefill.driverLicenseNumber) }
  var phoneNumber by rememberSaveable { mutableStateOf(prefill.phoneNumber) }
  var stationLocation by rememberSaveable { mutableStateOf(prefill.stationLocation) }
  var vehicleType by rememberSaveable { mutableStateOf(prefill.vehicleType.ifBlank { "Motorbike" }) }
  var registrationDateIso by rememberSaveable { mutableStateOf(prefill.registrationDateIso) }
  var internalRegCode by rememberSaveable { mutableStateOf(prefill.internalRegCode) }

  // Expiration is always 1 year from registration date (unless editing an existing explicit date and registration date wasn't changed)
  val computedExpirationIso =
    if (prefill.existingRecordId != null && registrationDateIso == prefill.registrationDateIso) {
      prefill.expirationDateIso
    } else {
      DateUtils.addOneYearIso(registrationDateIso)
    }

  var showValidationError by rememberSaveable { mutableStateOf(false) }
  var showRegDatePicker by rememberSaveable { mutableStateOf(false) }

  if (showRegDatePicker) {
    val initialMillis = DateUtils.isoToEpochMillisUtc(registrationDateIso)
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showRegDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              registrationDateIso = DateUtils.epochMillisToIsoUtc(millis)
            }
            showRegDatePicker = false
          },
          modifier = Modifier.testTag("reg_date_confirm_button"),
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRegDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  val isEdit = prefill.existingRecordId != null

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isEdit) "Edit Vehicle Pass" else "Register Driver & Vehicle",
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("form_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier =
          Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Text(
          text = "Driver Information",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
          value = driverName,
          onValueChange = { driverName = it },
          label = { Text("Driver Name *") },
          placeholder = { Text("e.g. Shaibu Sulemana") },
          leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
          isError = showValidationError && driverName.isBlank(),
          supportingText =
            if (showValidationError && driverName.isBlank()) {
              { Text("Driver name is required") }
            } else null,
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Next,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_driver_name"),
        )

        OutlinedTextField(
          value = driverLicenseNumber,
          onValueChange = { driverLicenseNumber = it },
          label = { Text("Driver License Number *") },
          placeholder = { Text("e.g. DL-GH-884210") },
          leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
          isError = showValidationError && driverLicenseNumber.isBlank(),
          supportingText =
            if (showValidationError && driverLicenseNumber.isBlank()) {
              { Text("Driver license number is required") }
            } else null,
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Characters,
              imeAction = ImeAction.Next,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_driver_license"),
        )

        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it },
          label = { Text("Phone Number *") },
          placeholder = { Text("e.g. +233 24 400 1122") },
          leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
          isError = showValidationError && phoneNumber.isBlank(),
          supportingText =
            if (showValidationError && phoneNumber.isBlank()) {
              { Text("Phone number is required") }
            } else null,
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Phone,
              imeAction = ImeAction.Next,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_phone_number"),
        )

        Text(
          text = "Vehicle & Station",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
          value = licensePlate,
          onValueChange = { licensePlate = it },
          label = { Text("Vehicle License Plate *") },
          placeholder = { Text("e.g. GR 9206-19") },
          leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
          isError = showValidationError && licensePlate.isBlank(),
          supportingText =
            if (showValidationError && licensePlate.isBlank()) {
              { Text("Vehicle license plate is required") }
            } else null,
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Characters,
              imeAction = ImeAction.Next,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_license_plate"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Vehicle Type *",
            style = MaterialTheme.typography.labelLarge,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            VEHICLE_TYPE_PRESETS.forEach { preset ->
              FilterChip(
                selected = vehicleType.equals(preset, ignoreCase = true),
                onClick = { vehicleType = preset },
                label = { Text(preset) },
                leadingIcon =
                  if (vehicleType.equals(preset, ignoreCase = true)) {
                    {
                      Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  } else null,
                modifier = Modifier.testTag("vehicle_type_${preset.lowercase()}"),
              )
            }
          }
          OutlinedTextField(
            value = vehicleType,
            onValueChange = { vehicleType = it },
            label = { Text("Custom / Selected Vehicle Type") },
            singleLine = true,
            isError = showValidationError && vehicleType.isBlank(),
            modifier = Modifier.fillMaxWidth().testTag("input_vehicle_type"),
          )
        }

        OutlinedTextField(
          value = stationLocation,
          onValueChange = { stationLocation = it },
          label = { Text("Station Location *") },
          placeholder = { Text("e.g. UMaT Essikado Campus, Sekondi-Takoradi") },
          leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
          isError = showValidationError && stationLocation.isBlank(),
          supportingText =
            if (showValidationError && stationLocation.isBlank()) {
              { Text("Station location is required") }
            } else null,
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Next,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_station_location"),
        )

        Text(
          text = "Registration & 1-Year Validity",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = { showRegDatePicker = true },
            modifier = Modifier.weight(1f).testTag("pick_reg_date_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.CalendarMonth,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reg: ${DateUtils.formatDisplayDate(registrationDateIso)}")
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().testTag("computed_expiration_card"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Column {
              Text(
                text = "Expiration Date (1 Year Validity)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = DateUtils.formatReadableDate(computedExpirationIso),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Text(
              text = DateUtils.formatDisplayDate(computedExpirationIso),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        OutlinedTextField(
          value = internalRegCode,
          onValueChange = { internalRegCode = it },
          label = { Text("Internal Registration Code *") },
          placeholder = { Text("e.g. UMAT-SRID-2026") },
          leadingIcon = { Icon(Icons.Filled.QrCode2, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = {
                internalRegCode =
                  DateUtils.generateInternalRegCode(stationLocation, registrationDateIso)
              },
              modifier = Modifier.testTag("generate_internal_code_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Generate internal code",
              )
            }
          },
          isError = showValidationError && internalRegCode.isBlank(),
          supportingText = {
            Text("Included in vehicle QR sticker string: PLATE(DRIVER,CODE)")
          },
          singleLine = true,
          keyboardOptions =
            KeyboardOptions(
              capitalization = KeyboardCapitalization.Characters,
              imeAction = ImeAction.Done,
            ),
          modifier = Modifier.fillMaxWidth().testTag("input_internal_code"),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = {
            val valid =
              driverName.isNotBlank() &&
                licensePlate.isNotBlank() &&
                driverLicenseNumber.isNotBlank() &&
                phoneNumber.isNotBlank() &&
                stationLocation.isNotBlank() &&
                vehicleType.isNotBlank() &&
                internalRegCode.isNotBlank()
            if (!valid) {
              showValidationError = true
            } else {
              onSave(
                prefill.existingRecordId,
                driverName,
                licensePlate,
                registrationDateIso,
                computedExpirationIso,
                driverLicenseNumber,
                phoneNumber,
                stationLocation,
                internalRegCode,
                vehicleType,
              )
            }
          },
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_registration_button"),
        ) {
          Text(
            text = if (isEdit) "Save Changes" else "Save Driver Registration",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
