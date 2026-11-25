package com.example

import com.example.util.CsvDriverImporter
import com.example.util.DateUtils
import com.example.util.PassStatus
import com.example.util.QrStickerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatePassLogicUnitTest {

  @Test
  fun `parse sticker QR text without closing parenthesis`() {
    val raw = "GR 9206-19(shaibu Sulemana,UMAT-SRID-2026"
    val parsed = QrStickerParser.parse(raw)
    assertTrue(parsed.isStructuredSticker)
    assertEquals("GR 9206-19", parsed.licensePlate)
    assertEquals("shaibu Sulemana", parsed.driverName)
    assertEquals("UMAT-SRID-2026", parsed.internalRegCode)
  }

  @Test
  fun `parse sticker QR text with closing parenthesis`() {
    val raw = "GR 9206-19(Shaibu Sulemana,UMAT-SRID-2026)"
    val parsed = QrStickerParser.parse(raw)
    assertTrue(parsed.isStructuredSticker)
    assertEquals("GR 9206-19", parsed.licensePlate)
    assertEquals("Shaibu Sulemana", parsed.driverName)
    assertEquals("UMAT-SRID-2026", parsed.internalRegCode)
  }

  @Test
  fun `expiration date is exactly one year from registration date`() {
    assertEquals("2027-11-15", DateUtils.addOneYearIso("2026-11-15"))
    assertEquals("2025-02-28", DateUtils.addOneYearIso("2024-02-29"))
  }

  @Test
  fun `pass status detects valid, expiring soon, and expired red alert`() {
    val ref = "2026-11-25"
    assertEquals(PassStatus.VALID, DateUtils.evaluatePassStatus("2027-11-25", ref))
    assertEquals(PassStatus.EXPIRING_SOON, DateUtils.evaluatePassStatus("2026-12-10", ref))
    assertEquals(PassStatus.EXPIRED, DateUtils.evaluatePassStatus("2026-11-24", ref))
  }

  @Test
  fun `csv importer parses sample rows and computes 1 year expiration when omitted`() {
    val parsed = CsvDriverImporter.parseCsvText(CsvDriverImporter.SAMPLE_CSV_CONTENT)
    assertEquals(2, parsed.validRows.size)
    assertTrue(parsed.errors.isEmpty())
    assertEquals("2027-11-15", parsed.validRows[0].expirationDateIso)
    assertEquals("2027-03-10", parsed.validRows[1].expirationDateIso)
  }
}
