package com.example

import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeUtils
import com.example.model.DuplicateConflictType
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `installment status computation handles unpaid partial and paid`() {
        assertEquals(InstallmentStatus.UNPAID, InstallmentStatus.computeStatus(0L, 10000L))
        assertEquals(InstallmentStatus.PARTIAL, InstallmentStatus.computeStatus(4500L, 10000L))
        assertEquals(InstallmentStatus.PAID, InstallmentStatus.computeStatus(10000L, 10000L))
        assertEquals(InstallmentStatus.PAID, InstallmentStatus.computeStatus(12000L, 10000L))
    }

    @Test
    fun `reminder filtering strictly excludes PAID members and preserves remaining partial due`() {
        val m1 = CommitteeMember(id = "m1", name = "Ali", phone = "03001111111", committeeNumber = 1L)
        val m2 = CommitteeMember(id = "m2", name = "Bilal", phone = "03002222222", committeeNumber = 2L)
        val m3 = CommitteeMember(id = "m3", name = "Zain", phone = "03003333333", committeeNumber = 3L)

        val installments = listOf(
            InstallmentRecord(id = "i1", memberId = "m1", dueAmount = 10000L, paidAmount = 10000L, status = InstallmentStatus.PAID.name),
            InstallmentRecord(id = "i2", memberId = "m2", dueAmount = 10000L, paidAmount = 4000L, status = InstallmentStatus.PARTIAL.name)
        )

        val result = CommitteeUtils.filterUnpaidForReminders(listOf(m1, m2, m3), installments, 10000L)
        assertEquals(listOf("m1"), result.excludedPaidMembers.map { it.id })
        assertEquals(listOf("m2" to 6000L, "m3" to 10000L), result.eligibleUnpaidOrPartial.map { it.first.id to it.second })
    }

    @Test
    fun `duplicate detection catches normalized phone and committee number conflicts`() {
        val m1 = CommitteeMember(id = "m1", name = "Tariq", phone = "0300-1234567", committeeNumber = 1L)
        val archived = CommitteeMember(
            id = "m2",
            name = "Old",
            phone = "03009999999",
            committeeNumber = 2L,
            status = MemberStatus.ARCHIVED.name
        )

        val phoneConflict = CommitteeUtils.findDuplicateMember(
            members = listOf(m1, archived),
            phone = "+92 300 1234567",
            committeeNumber = 3L
        )
        assertNotNull(phoneConflict)
        assertEquals(DuplicateConflictType.PHONE, phoneConflict?.type)

        val numConflict = CommitteeUtils.findDuplicateMember(
            members = listOf(m1, archived),
            phone = "03111234567",
            committeeNumber = 1L
        )
        assertNotNull(numConflict)
        assertEquals(DuplicateConflictType.COMMITTEE_NUMBER, numConflict?.type)

        assertNull(
            CommitteeUtils.findDuplicateMember(
                members = listOf(m1, archived),
                phone = "03009999999",
                committeeNumber = 2L,
                excludeMemberId = "m1"
            )
        )
    }

    @Test
    fun `password hashing and admin role authorization work deterministically`() {
        val hash1 = com.example.model.PasswordHasher.sha256("1234")
        val hash2 = com.example.model.PasswordHasher.sha256("1234")
        val hashDiff = com.example.model.PasswordHasher.sha256("5678")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertTrue(hash1 != hashDiff)

        val admin = com.example.model.LocalUser(
            id = "u1",
            username = "admin",
            displayName = "Admin",
            passwordHash = hash1,
            role = com.example.model.UserRole.ADMIN.name
        )
        val staff = com.example.model.LocalUser(
            id = "u2",
            username = "staff",
            displayName = "Staff",
            passwordHash = hashDiff,
            role = com.example.model.UserRole.STAFF.name
        )
        assertTrue(admin.isAdmin)
        assertTrue(!staff.isAdmin)
        assertEquals(com.example.model.UploadQueueStatus.FAILED, com.example.model.UploadQueueStatus.fromRaw("FAILED"))
        assertEquals(com.example.model.UploadQueueStatus.PENDING, com.example.model.UploadQueueStatus.fromRaw("unknown"))
    }
}

