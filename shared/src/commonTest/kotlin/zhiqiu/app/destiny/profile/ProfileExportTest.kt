package zhiqiu.app.destiny.profile

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileExportTest {
    private fun sampleProfile() = Profile(
        id = "abc123",
        name = "测试命盘",
        gender = "男",
        birthdayType = "公历",
        birthday = "1990-01-01",
        timeIndex = 12,
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun plainRoundTrip() {
        val profiles = listOf(sampleProfile())
        val prefs = listOf(zhiqiu.app.destiny.db.ReaderPrefEntity("k1", "v1"))
        val json = exportAllJson(profiles, prefs)
        val parsed = importAllFromJson(json)
        assertEquals(profiles, parsed.profiles)
        assertEquals(prefs, parsed.readerPrefs)
    }

    @Test
    fun legacyRawArrayStillImports() {
        val raw = Json.encodeToString(listOf(sampleProfile()))
        val parsed = importAllFromJson(raw)
        assertEquals(listOf(sampleProfile()), parsed.profiles)
        assertTrue(parsed.readerPrefs.isEmpty())
    }

    @Test
    fun encryptedRoundTripAndWrongPasswordFails() {
        val profiles = listOf(sampleProfile())
        val plain = exportAllJson(profiles, emptyList())
        val enc = exportEncryptedJson(plain, "password123")
        assertTrue(isEncryptedBackup(enc))
        val parsed = importAllFromJson(enc, "password123")
        assertEquals(profiles, parsed.profiles)
        assertFailsWith<IllegalArgumentException> { importAllFromJson(enc, "wrong") }
    }

    @Test
    fun plaintextIsNotDetectedAsEncrypted() {
        assertFalse(isEncryptedBackup(exportAllJson(listOf(sampleProfile()), emptyList())))
    }
}
