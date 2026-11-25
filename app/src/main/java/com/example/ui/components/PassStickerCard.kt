package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VehicleRecord
import com.example.ui.theme.LocalGateStatusColors
import com.example.util.DateUtils
import com.example.util.PassStatus
import com.example.util.QrBitmapGenerator

@Composable
fun PassStickerCard(
  record: VehicleRecord,
  modifier: Modifier = Modifier,
) {
  val status = record.status()
  val statusColors = LocalGateStatusColors.current
  val borderColor =
    when (status) {
      PassStatus.VALID -> statusColors.validAccent
      PassStatus.EXPIRING_SOON -> statusColors.expiringAccent
      PassStatus.EXPIRED -> statusColors.expiredAccent
    }
  val qrText = remember(record.licensePlate, record.driverName, record.internalRegCode) {
    record.stickerQrPayload()
  }
  val qrBitmap = remember(qrText) { QrBitmapGenerator.generateQrBitmap(qrText, 420) }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(2.5.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column {
      // Top Crest Header Bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = "Official station pass",
            tint = Color(0xFFF5C344),
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = record.stationLocation,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadgePill(status = status)
      }

      // Sticker Core: QR on Left, Plate & Driver on Right
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Surface(
          color = Color.White,
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.size(116.dp),
        ) {
          if (qrBitmap != null) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "Vehicle verification QR code for ${record.licensePlate}",
              contentScale = ContentScale.Fit,
              modifier = Modifier.padding(6.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = record.licensePlate,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
          }
          Text(
            text = record.driverName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "License: ${record.driverLicenseNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = record.internalRegCode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Gold Pass Type & Expiration Strip (inspired by attached sticker)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
          Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5C344))
            .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Text(
          text = "${record.vehicleType} Pass",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF2D1E00),
        )
        Surface(
          color =
            if (status == PassStatus.EXPIRED) statusColors.expiredAccent else Color.White,
          contentColor =
            if (status == PassStatus.EXPIRED) Color.White else Color(0xFF151D17),
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = "Exp Date: ${DateUtils.formatDisplayDate(record.expirationDateIso)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // Encoded QR String preview
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
      ) {
        Text(
          text = "QR: $qrText",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
