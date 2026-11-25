package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VehicleRecord
import com.example.data.VerificationResult
import com.example.ui.RecentScanEntry
import com.example.ui.RegistrationPrefill
import com.example.ui.RegistryStats
import com.example.ui.components.CameraQrScannerCard
import com.example.ui.components.NotFoundResultCard
import com.example.ui.components.VerificationResultCard
import com.example.ui.theme.LocalGateStatusColors
import com.example.util.DateUtils
import com.example.util.PassStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerifyScreen(
  stats: RegistryStats,
  verificationResult: VerificationResult?,
  recentScans: List<RecentScanEntry>,
  onVerifyQuery: (String, Boolean) -> Unit,
  onClearResult: () -> Unit,
  onPhotoError: (String) -> Unit,
  onRenewRecord: (VehicleRecord) -> Unit,
  onViewPass: (VehicleRecord) -> Unit,
  onEditRecord: (VehicleRecord) -> Unit,
  onRegisterPrefilled: (RegistrationPrefill) -> Unit,
  onFilterExpiredClick: () -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  var manualInput by rememberSaveable { mutableStateOf("") }
  val keyboardController = LocalSoftwareKeyboardController.current

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().padding(contentPadding),
  ) {
    val isWide = maxWidth >= 760.dp

    if (isWide) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
          VerifyStatsStrip(stats = stats, onExpiredClick = onFilterExpiredClick)
          CameraQrScannerCard(
            onQrDecoded = { text, fromCamera ->
              manualInput = text
              onVerifyQuery(text, fromCamera)
            },
            onPhotoDecodeError = onPhotoError,
          )
          ManualLookupBox(
            query = manualInput,
            onQueryChange = { manualInput = it },
            onSubmit = {
              keyboardController?.hide()
              onVerifyQuery(manualInput, false)
            },
          )
        }
        Column(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
          VerificationResultArea(
            verificationResult = verificationResult,
            recentScans = recentScans,
            onVerifyRecent = { raw ->
              manualInput = raw
              onVerifyQuery(raw, false)
            },
            onClearResult = onClearResult,
            onRenewRecord = onRenewRecord,
            onViewPass = onViewPass,
            onEditRecord = onEditRecord,
            onRegisterPrefilled = onRegisterPrefilled,
          )
        }
      }
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier =
            Modifier
              .widthIn(max = 600.dp)
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          VerifyStatsStrip(stats = stats, onExpiredClick = onFilterExpiredClick)

          CameraQrScannerCard(
            onQrDecoded = { text, fromCamera ->
              manualInput = text
              onVerifyQuery(text, fromCamera)
            },
            onPhotoDecodeError = onPhotoError,
          )

          ManualLookupBox(
            query = manualInput,
            onQueryChange = { manualInput = it },
            onSubmit = {
              keyboardController?.hide()
              onVerifyQuery(manualInput, false)
            },
          )

          VerificationResultArea(
            verificationResult = verificationResult,
            recentScans = recentScans,
            onVerifyRecent = { raw ->
              manualInput = raw
              onVerifyQuery(raw, false)
            },
            onClearResult = onClearResult,
            onRenewRecord = onRenewRecord,
            onViewPass = onViewPass,
            onEditRecord = onEditRecord,
            onRegisterPrefilled = onRegisterPrefilled,
          )

          Spacer(modifier = Modifier.height(72.dp))
        }
      }
    }
  }
}

@Composable
private fun VerifyStatsStrip(
  stats: RegistryStats,
  onExpiredClick: () -> Unit,
) {
  val statusColors = LocalGateStatusColors.current
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
  ) {
    StatChipCard(
      label = "Offline DB",
      count = stats.total,
      bg = MaterialTheme.colorScheme.primaryContainer,
      fg = MaterialTheme.colorScheme.onPrimaryContainer,
      icon = Icons.Filled.VerifiedUser,
      onClick = null,
    )
    StatChipCard(
      label = "Approved",
      count = stats.valid,
      bg = statusColors.validBg,
      fg = statusColors.validFg,
      icon = Icons.Filled.CheckCircle,
      onClick = null,
    )
    StatChipCard(
      label = "Expiring",
      count = stats.expiringSoon,
      bg = statusColors.expiringBg,
      fg = statusColors.expiringFg,
      icon = Icons.Filled.Warning,
      onClick = null,
    )
    StatChipCard(
      label = "Expired",
      count = stats.expired,
      bg = statusColors.expiredBg,
      fg = statusColors.expiredFg,
      icon = Icons.Filled.Error,
      onClick = if (stats.expired > 0) onExpiredClick else null,
    )
  }
}

@Composable
private fun StatChipCard(
  label: String,
  count: Int,
  bg: Color,
  fg: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: (() -> Unit)?,
) {
  Surface(
    color = bg,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, fg.copy(alpha = 0.2f)),
    onClick = onClick ?: {},
    enabled = onClick != null,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = fg,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$count $label",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = fg,
      )
    }
  }
}

@Composable
private fun ManualLookupBox(
  query: String,
  onQueryChange: (String) -> Unit,
  onSubmit: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(12.dp),
    ) {
      Text(
        text = "Manual QR / Plate / License Lookup",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = {
            Text(
              text = "e.g. GR 9206-19 or QR text",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.QrCode2, contentDescription = null)
          },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear input")
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
          modifier = Modifier.weight(1f).testTag("manual_qr_input"),
        )
        Button(
          onClick = onSubmit,
          enabled = query.isNotBlank(),
          modifier = Modifier.height(54.dp).testTag("verify_manual_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Verify",
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerificationResultArea(
  verificationResult: VerificationResult?,
  recentScans: List<RecentScanEntry>,
  onVerifyRecent: (String) -> Unit,
  onClearResult: () -> Unit,
  onRenewRecord: (VehicleRecord) -> Unit,
  onViewPass: (VehicleRecord) -> Unit,
  onEditRecord: (VehicleRecord) -> Unit,
  onRegisterPrefilled: (RegistrationPrefill) -> Unit,
) {
  when (verificationResult) {
    is VerificationResult.Verified -> {
      VerificationResultCard(
        result = verificationResult,
        onRenewClick = onRenewRecord,
        onViewPassClick = onViewPass,
        onEditClick = onEditRecord,
        onScanNext = onClearResult,
      )
    }
    is VerificationResult.NotFound -> {
      NotFoundResultCard(
        result = verificationResult,
        onRegisterPrefilled = {
          val parsed = verificationResult.parsedQr
          val today = DateUtils.todayIso()
          onRegisterPrefilled(
            RegistrationPrefill(
              driverName = parsed.driverName.orEmpty(),
              licensePlate = parsed.licensePlate.orEmpty(),
              internalRegCode =
                parsed.internalRegCode?.takeIf { it.isNotBlank() }
                  ?: DateUtils.generateInternalRegCode("UMAT", today),
              registrationDateIso = today,
              expirationDateIso = DateUtils.addOneYearIso(today),
            )
          )
        },
        onClear = onClearResult,
      )
    }
    null -> {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("ready_to_scan_placeholder"),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(20.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
          )
          Text(
            text = "Ready for Offline Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              "Point the camera at a vehicle QR sticker, pick a sticker photo, or enter a plate/QR string to verify against the local SQLite database.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (recentScans.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Recent Checks",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        recentScans.forEach { entry ->
          val tint =
            when (entry.status) {
              PassStatus.VALID -> LocalGateStatusColors.current.validFg
              PassStatus.EXPIRING_SOON -> LocalGateStatusColors.current.expiringFg
              PassStatus.EXPIRED, null -> LocalGateStatusColors.current.expiredFg
            }
          AssistChip(
            onClick = { onVerifyRecent(entry.rawQuery) },
            label = {
              Text(
                text = entry.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = {
              Icon(
                imageVector =
                  if (entry.status == PassStatus.VALID) Icons.Filled.CheckCircle
                  else if (entry.status == PassStatus.EXPIRING_SOON) Icons.Filled.Warning
                  else Icons.Filled.Error,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
              )
            },
          )
        }
      }
    }
  }
}
