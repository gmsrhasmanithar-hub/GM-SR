package com.example

import com.example.model.Committee
import com.example.model.MonthUtils
import com.example.model.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitteeLogicUnitTest {

    @Test
    fun `committee formulas calculate dynamically for arbitrary sizes`() {
        val c25 = Committee(name = "Test 25", memberCount = 25L, monthlyInstallment = 10000L, totalMonths = 25L)
        assertEquals(250_000L, c25.monthlyCollectionTotal)
        assertEquals(6_250_000L, c25.totalCommitteeAmount)
        assertEquals(250_000L, c25.eachPayoutAmount)

        val c50 = Committee(name = "Test 50", memberCount = 50L, monthlyInstallment = 5000L, totalMonths = 50L)
        assertEquals(250_000L, c50.monthlyCollectionTotal)
        assertEquals(12_500_000L, c50.totalCommitteeAmount)
    }

    @Test
    fun `month arithmetic handles year rollover accurately`() {
        assertEquals("2027-01", MonthUtils.addMonths("2026-11", 2))
        assertEquals("2027-08", MonthUtils.addMonths("2026-11", 9))
        val list = MonthUtils.monthKeysForCommittee("2026-11", 5)
        assertEquals(listOf("2026-11", "2026-12", "2027-01", "2027-02", "2027-03"), list)
        assertTrue(MonthUtils.isValidMonthKey("2026-11"))
    }

    @Test
    fun `phone normalization formats Pakistani numbers for WhatsApp and SMS`() {
        assertEquals("923001234567", PhoneUtils.toWhatsAppDigits("0300-1234567"))
        assertEquals("923001234567", PhoneUtils.toWhatsAppDigits("+92 300 1234567"))
        assertEquals("+923001234567", PhoneUtils.toE164("03001234567"))
    }
}
