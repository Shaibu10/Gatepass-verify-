package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

enum class PassStatus {
  VALID,
  EXPIRING_SOON,
  EXPIRED,
}

object DateUtils {
  private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")
  private const val MILLIS_PER_DAY = 86_400_000L

  fun todayIso(): String {
    val cal = Calendar.getInstance()
    return formatIso(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun formatIso(year: Int, monthOneBased: Int, day: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, monthOneBased, day)

  fun epochMillisToIsoUtc(millis: Long): String {
    val cal = GregorianCalendar(utcTimeZone).apply { timeInMillis = millis }
    return formatIso(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun isoToEpochMillisUtc(iso: String): Long? {
    val parts = iso.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    val cal =
      GregorianCalendar(utcTimeZone).apply {
        isLenient = false
        clear()
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return runCatching { cal.timeInMillis }.getOrNull()
  }

  fun addOneYearIso(registrationIso: String): String {
    val millis = isoToEpochMillisUtc(registrationIso) ?: return todayPlusOneYearIso()
    val cal = GregorianCalendar(utcTimeZone).apply {
      timeInMillis = millis
      add(Calendar.YEAR, 1)
    }
    return formatIso(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun todayPlusOneYearIso(): String = addOneYearIso(todayIso())

  fun daysUntilExpiration(expirationIso: String, referenceIso: String = todayIso()): Long {
    val expMillis = isoToEpochMillisUtc(expirationIso) ?: return -1L
    val refMillis = isoToEpochMillisUtc(referenceIso) ?: return -1L
    return (expMillis - refMillis) / MILLIS_PER_DAY
  }

  fun evaluatePassStatus(
    expirationIso: String,
    referenceIso: String = todayIso(),
  ): PassStatus {
    val days = daysUntilExpiration(expirationIso, referenceIso)
    return when {
      days < 0L -> PassStatus.EXPIRED
      days <= 30L -> PassStatus.EXPIRING_SOON
      else -> PassStatus.VALID
    }
  }

  fun formatDisplayDate(iso: String): String {
    val millis = isoToEpochMillisUtc(iso) ?: return iso
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = utcTimeZone }
    return sdf.format(Date(millis))
  }

  fun formatReadableDate(iso: String): String {
    val millis = isoToEpochMillisUtc(iso) ?: return iso
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US).apply { timeZone = utcTimeZone }
    return sdf.format(Date(millis))
  }

  fun parseFlexibleDateToIso(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    // Exact ISO yyyy-MM-dd
    if (Regex("""^\d{4}-\d{1,2}-\d{1,2}$""").matches(trimmed)) {
      val parts = trimmed.split("-")
      val y = parts[0].toInt()
      val m = parts[1].toInt()
      val d = parts[2].toInt()
      val candidate = formatIso(y, m, d)
      if (isoToEpochMillisUtc(candidate) != null) return candidate
    }

    // yyyy/MM/dd
    if (Regex("""^\d{4}/\d{1,2}/\d{1,2}$""").matches(trimmed)) {
      val parts = trimmed.split("/")
      val candidate = formatIso(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
      if (isoToEpochMillisUtc(candidate) != null) return candidate
    }

    // dd/MM/yyyy or dd-MM-yyyy (matches sticker format 15/11/2027)
    val dmyMatch = Regex("""^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$""").matchEntire(trimmed)
    if (dmyMatch != null) {
      val first = dmyMatch.groupValues[1].toInt()
      val second = dmyMatch.groupValues[2].toInt()
      val year = dmyMatch.groupValues[3].toInt()
      // Prefer dd/MM/yyyy unless second > 12 (then MM/dd/yyyy)
      val (day, month) = if (second > 12 && first <= 12) second to first else first to second
      val candidate = formatIso(year, month, day)
      if (isoToEpochMillisUtc(candidate) != null) return candidate
    }

    // Named month formats like "15 Nov 2027"
    val namedFormats = listOf("dd MMM yyyy", "d MMM yyyy", "MMM dd, yyyy")
    for (pattern in namedFormats) {
      val sdf =
        SimpleDateFormat(pattern, Locale.US).apply {
          isLenient = false
          timeZone = utcTimeZone
        }
      val parsed = runCatching { sdf.parse(trimmed) }.getOrNull()
      if (parsed != null) {
        return epochMillisToIsoUtc(parsed.time)
      }
    }

    return null
  }

  fun generateInternalRegCode(
    stationLocation: String = "",
    registrationDateIso: String = todayIso(),
  ): String {
    val year = registrationDateIso.take(4).takeIf { it.length == 4 } ?: todayIso().take(4)
    val stationPrefix =
      stationLocation
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9 ]"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .let { words ->
          when {
            words.isEmpty() -> "GATE"
            words.first().length in 2..5 -> words.first()
            else -> words.joinToString("") { it.take(1) }.take(4).ifEmpty { "GATE" }
          }
        }
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    return "$stationPrefix-$suffix-$year"
  }
}
