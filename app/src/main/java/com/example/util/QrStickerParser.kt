package com.example.util

import java.util.Locale

data class ParsedQrPayload(
  val rawText: String,
  val licensePlate: String?,
  val driverName: String?,
  val internalRegCode: String?,
  val isStructuredSticker: Boolean,
)

object QrStickerParser {

  fun normalizePlate(plate: String): String =
    plate.trim().uppercase(Locale.US).replace(Regex("""\s+"""), " ")

  fun compactPlate(plate: String): String =
    plate.trim().uppercase(Locale.US).replace(Regex("""[\s-]+"""), "")

  fun normalizeLicenseNumber(license: String): String =
    license.trim().uppercase(Locale.US).replace(Regex("""\s+"""), "")

  /**
   * Parses QR sticker strings matching:
   * - "GR 9206-19(shaibu Sulemana,UMAT-SRID-2026"
   * - "GR 9206-19(shaibu Sulemana,UMAT-SRID-2026)"
   * - "GR 9206-19 (Shaibu Sulemana, UMAT-SRID-2026)"
   * Also supports fallback plain plate ("GR 9206-19") or internal code ("UMAT-SRID-2026").
   */
  fun parse(rawInput: String): ParsedQrPayload {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      return ParsedQrPayload(
        rawText = rawInput,
        licensePlate = null,
        driverName = null,
        internalRegCode = null,
        isStructuredSticker = false,
      )
    }

    val openParenIdx = trimmed.indexOf('(')
    if (openParenIdx > 0) {
      val platePart = trimmed.substring(0, openParenIdx).trim()
      val afterParen =
        trimmed
          .substring(openParenIdx + 1)
          .trim()
          .removeSuffix(")")
          .trim()

      val commaIdx = afterParen.lastIndexOf(',')
      return if (commaIdx >= 0) {
        val driverPart = afterParen.substring(0, commaIdx).trim().ifEmpty { null }
        val codePart = afterParen.substring(commaIdx + 1).trim().ifEmpty { null }
        ParsedQrPayload(
          rawText = trimmed,
          licensePlate = platePart.ifEmpty { null },
          driverName = driverPart,
          internalRegCode = codePart,
          isStructuredSticker = true,
        )
      } else {
        ParsedQrPayload(
          rawText = trimmed,
          licensePlate = platePart.ifEmpty { null },
          driverName = afterParen.ifEmpty { null },
          internalRegCode = null,
          isStructuredSticker = true,
        )
      }
    }

    // Fallback: pipe or semicolon separated "PLATE|NAME|CODE"
    if (trimmed.contains('|')) {
      val parts = trimmed.split('|').map { it.trim() }
      if (parts.size >= 2) {
        return ParsedQrPayload(
          rawText = trimmed,
          licensePlate = parts.getOrNull(0)?.ifEmpty { null },
          driverName = parts.getOrNull(1)?.ifEmpty { null },
          internalRegCode = parts.getOrNull(2)?.ifEmpty { null },
          isStructuredSticker = true,
        )
      }
    }

    // Plain plate or internal code
    return ParsedQrPayload(
      rawText = trimmed,
      licensePlate = trimmed,
      driverName = null,
      internalRegCode = trimmed,
      isStructuredSticker = false,
    )
  }

  fun formatStickerQrText(
    licensePlate: String,
    driverName: String,
    internalRegCode: String,
  ): String = "${licensePlate.trim()}(${driverName.trim()},${internalRegCode.trim()})"
}
