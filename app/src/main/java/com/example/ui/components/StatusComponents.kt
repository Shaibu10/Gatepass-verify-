package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VehicleRecord
import com.example.data.VerificationResult
import com.example.ui.theme.LocalGateStatusColors
import com.example.util.DateUtils
import com.example.util.PassStatus
import kotlin.math.abs

@Composable
fun StatusBadgePill(
  status: PassStatus,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalGateStatusColors.current
  val (bg, fg, icon, label) =
    when (status) {
      PassStatus.VALID ->
        Quad(statusColors.validBg, statusColors.validFg, Icons.Filled.CheckCircle, "APPROVED")
      PassStatus.EXPIRING_SOON ->
        Quad(statusColors.expiringBg, statusColors.expiringFg, Icons.Filled.Warning, "EXPIRING SOON")
      PassStatus.EXPIRED ->
        Quad(statusColors.expiredBg, statusColors.expiredFg, Icons.Filled.Error, "EXPIRED ALERT")
    }

  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, fg.copy(alpha = 0.3f)),
    modifier = modifier.testTag("status_badge_${status.name.lowercase()}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = fg,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = fg,
      )
    }
  }
}

@Composable
fun ExpiredRedAlertBanner(
  daysOverdue: Long,
  onRenewClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalGateStatusColors.current
  Surface(
    color = statusColors.expiredAccent,
    contentColor = Color.White,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier.fillMaxWidth().testTag("expired_red_alert_banner"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.2f)),
        ) {
          Icon(
            imageVector = Icons.Filled.ReportProblem,
            contentDescription = "Expired registration alert",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "REGISTRATION EXPIRED",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          val overdueText =
            when {
              daysOverdue <= 0L -> "Expired today — renewal required before entry"
              daysOverdue == 1L -> "1 day overdue — deny gate passage"
              else -> "$daysOverdue days overdue — deny gate passage"
            }
          Text(
            text = overdueText,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f),
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        onClick = onRenewClick,
        color = Color.White,
        contentColor = statusColors.expiredAccent,
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("alert_renew_button"),
      ) {
        Text(
          text = "Renew",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerificationResultCard(
  result: VerificationResult.Verified,
  onRenewClick: (VehicleRecord) -> Unit,
  onViewPassClick: (VehicleRecord) -> Unit,
  onEditClick: (VehicleRecord) -> Unit,
  onScanNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalGateStatusColors.current
  val record = result.record
  val (bannerBg, bannerFg, statusHeadline, statusSubline, statusIcon) =
    when (result.status) {
      PassStatus.VALID ->
        Quint(
          statusColors.validAccent,
          Color.White,
          "APPROVED FOR ENTRY",
          "${result.daysRemaining} days remaining • Valid pass",
          Icons.Filled.CheckCircle,
        )
      PassStatus.EXPIRING_SOON ->
        Quint(
          statusColors.expiringAccent,
          Color.White,
          "APPROVED • EXPIRING SOON",
          if (result.daysRemaining == 0L) "Expires today — remind driver to renew"
          else "Expires in ${result.daysRemaining} days",
          Icons.Filled.Warning,
        )
      PassStatus.EXPIRED ->
        Quint(
          statusColors.expiredAccent,
          Color.White,
          "EXPIRED — ENTRY DENIED",
          "Expired ${abs(result.daysRemaining)} days ago on ${DateUtils.formatDisplayDate(record.expirationDateIso)}",
          Icons.Filled.Error,
        )
    }

  AnimatedVisibility(
    visible = true,
    enter = fadeIn() + slideInVertically { it / 6 },
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(2.dp, bannerBg),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = modifier.fillMaxWidth().testTag("verification_result_card"),
    ) {
      Column {
        // Bold Color-Coded Approval / Alert Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .fillMaxWidth()
              .background(bannerBg)
              .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
          Icon(
            imageVector = statusIcon,
            contentDescription = statusHeadline,
            tint = bannerFg,
            modifier = Modifier.size(30.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = statusHeadline,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = bannerFg,
            )
            Text(
              text = statusSubline,
              style = MaterialTheme.typography.bodySmall,
              color = bannerFg.copy(alpha = 0.92f),
            )
          }
        }

        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          if (result.status == PassStatus.EXPIRED) {
            ExpiredRedAlertBanner(
              daysOverdue = abs(result.daysRemaining),
              onRenewClick = { onRenewClick(record) },
            )
          }

          if (result.nameMismatchWarning) {
            Surface(
              color = statusColors.expiringBg,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Warning,
                  contentDescription = "Name mismatch warning",
                  tint = statusColors.expiringFg,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "QR driver name '${result.parsedQr.driverName}' differs from database record '${record.driverName}'.",
                  style = MaterialTheme.typography.bodySmall,
                  color = statusColors.expiringFg,
                )
              }
            }
          }

          // Plate & Vehicle Type Row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = record.licensePlate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              shape = RoundedCornerShape(50),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector =
                    if (record.vehicleType.contains("moto", ignoreCase = true) ||
                      record.vehicleType.contains("bike", ignoreCase = true)
                    ) {
                      Icons.Filled.TwoWheeler
                    } else {
                      Icons.Filled.DirectionsCar
                    },
                  contentDescription = record.vehicleType,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = record.vehicleType,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Full Driver & Vehicle Metadata Grid
          DetailRow(
            icon = Icons.Filled.Person,
            label = "Driver Name",
            value = record.driverName,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            DetailRow(
              icon = Icons.Filled.Badge,
              label = "Driver License No.",
              value = record.driverLicenseNumber,
              monoValue = true,
              modifier = Modifier.weight(1f),
            )
            DetailRow(
              icon = Icons.Filled.Phone,
              label = "Phone Number",
              value = record.phoneNumber,
              monoValue = true,
              modifier = Modifier.weight(1f),
            )
          }
          DetailRow(
            icon = Icons.Filled.LocationOn,
            label = "Station Location",
            value = record.stationLocation,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            DetailRow(
              icon = Icons.Filled.CalendarMonth,
              label = "Registered",
              value = DateUtils.formatDisplayDate(record.registrationDateIso),
              monoValue = true,
              modifier = Modifier.weight(1f),
            )
            DetailRow(
              icon = Icons.Filled.CalendarMonth,
              label = "Expiration Date",
              value = DateUtils.formatDisplayDate(record.expirationDateIso),
              monoValue = true,
              valueColor =
                if (result.status == PassStatus.EXPIRED) statusColors.expiredAccent
                else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
          DetailRow(
            icon = Icons.Filled.QrCode2,
            label = "Internal Registration Code",
            value = record.internalRegCode,
            monoValue = true,
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { onViewPassClick(record) },
              modifier = Modifier.testTag("result_view_pass_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Digital Pass")
            }

            if (result.status != PassStatus.VALID) {
              Button(
                onClick = { onRenewClick(record) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                  ),
                modifier = Modifier.testTag("result_renew_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Renew +1 Yr")
              }
            }

            OutlinedButton(
              onClick = { onEditClick(record) },
              modifier = Modifier.testTag("result_edit_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Edit")
            }

            TextButton(
              onClick = onScanNext,
              modifier = Modifier.testTag("result_scan_next_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Clear")
            }
          }
        }
      }
    }
  }
}

@Composable
fun NotFoundResultCard(
  result: VerificationResult.NotFound,
  onRegisterPrefilled: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalGateStatusColors.current
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(2.dp, statusColors.expiredAccent),
    modifier = modifier.fillMaxWidth().testTag("not_found_result_card"),
  ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier
            .fillMaxWidth()
            .background(statusColors.expiredAccent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.ReportProblem,
          contentDescription = "Vehicle not registered",
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "UNREGISTERED VEHICLE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Text(
            text = "No matching pass found in local SQLite database",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f),
          )
        }
      }

      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(16.dp),
      ) {
        Text(
          text = "Scanned / Queried Input",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = result.query,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(10.dp),
          )
        }

        if (result.parsedQr.isStructuredSticker) {
          Text(
            text = "Decoded Sticker Details",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          result.parsedQr.licensePlate?.let {
            DetailRow(icon = Icons.Filled.DirectionsCar, label = "Plate Number", value = it)
          }
          result.parsedQr.driverName?.let {
            DetailRow(icon = Icons.Filled.Person, label = "Driver Name", value = it)
          }
          result.parsedQr.internalRegCode?.let {
            DetailRow(
              icon = Icons.Filled.QrCode2,
              label = "Internal Code",
              value = it,
              monoValue = true,
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Button(
            onClick = onRegisterPrefilled,
            modifier = Modifier.weight(1f).testTag("register_from_scan_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AddCircle,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Register Vehicle")
          }
          OutlinedButton(
            onClick = onClear,
            modifier = Modifier.testTag("dismiss_not_found_button"),
          ) {
            Text("Dismiss")
          }
        }
      }
    }
  }
}

@Composable
fun DetailRow(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  monoValue: Boolean = false,
  valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = modifier,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 2.dp).size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style =
          if (monoValue) MaterialTheme.typography.labelSmall.copy(
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.SemiBold,
          )
          else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = valueColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class Quint<A, B, C, D, E>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: E,
)
