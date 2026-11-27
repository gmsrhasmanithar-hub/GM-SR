package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CommitteeRepository
import com.example.data.MessagingGateway
import com.example.data.PdfReportType
import com.example.data.local.AppDatabase
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.AppCompanySettings
import com.example.model.BackupUtils
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.PayoutStatus
import com.example.model.ReminderStage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GM HASMANI COMMITTEE", appName)
    }

    @Test
    fun `backup preview parses exported json counts including turns and old records`() {
        val sampleJson = """
            {
              "brand": "GM HASMANI COMMITTEE",
              "version": 3,
              "exportedAt": "2026-11-27",
              "committees": [
                {
                  "id": "c1",
                  "name": "Family Circle",
                  "members": [{"id": "m1"}, {"id": "m2"}],
                  "installments": [{"id": "i1"}, {"id": "i2"}, {"id": "i3"}],
                  "turns": [{"id": "t1"}, {"id": "t2"}, {"id": "t3"}, {"id": "t4"}]
                }
              ],
              "oldCommitteeRecords": [
                {"id": "old1", "committeeName": "2024 Cycle"}
              ]
            }
        """.trimIndent()
        val preview = BackupUtils.parsePreview(sampleJson).getOrThrow()
        assertEquals(1, preview.committeeCount)
        assertEquals(2, preview.memberCount)
        assertEquals(3, preview.installmentCount)
        assertEquals(4, preview.turnCount)
        assertEquals(1, preview.oldRecordCount)
        assertEquals("2026-11-27", preview.exportedAt)

        assertTrue(BackupUtils.parsePreview("{not valid json").isFailure)
    }

    @Test
    fun `localization strings reminder templates easypaisa request and 9 pdf reports work`() {
        val en = stringsFor(AppLanguage.EN)
        val ur = stringsFor(AppLanguage.UR)
        assertTrue(en.totalActiveCommittees.isNotBlank())
        assertTrue(ur.totalActiveCommittees.isNotBlank())
        assertTrue(en.confirmRestoreTitle.isNotBlank())
        assertTrue(ur.confirmRestoreTitle.isNotBlank())
        assertTrue(en.oldRecordsTitle.isNotBlank())
        assertTrue(ur.oldRecordsTitle.isNotBlank())

        val gateway = MessagingGateway()
        val committee = Committee(name = "Hasmani Circle", memberCount = 7L, totalShares = 10L, monthlyInstallment = 5000L)
        val member = CommitteeMember(name = "Tariq", phone = "03001234567", committeeNumber = 1L, shares = 2.0)
        val settings = AppCompanySettings()
        val renderedUr = gateway.renderMessage(
            template = ReminderStage.FIRST_28.defaultTemplateUr,
            member = member,
            committee = committee,
            monthKey = "2026-11",
            dueAmount = 10000L,
            settings = settings
        )
        assertTrue(renderedUr.contains("Tariq"))
        assertTrue(renderedUr.contains("Hasmani Circle"))
        assertTrue(renderedUr.contains("PKR 10,000"))

        val paymentRequest = gateway.buildPaymentRequestMessage(
            memberName = "Tariq",
            amount = 10000L,
            dueDate = "28 November 2026 to 8 December 2026",
            committeeName = "GM Hasmani Committee",
            settings = settings
        )
        assertTrue(paymentRequest.contains("03423498631"))
        assertTrue(paymentRequest.contains("Tariq"))
        assertTrue(paymentRequest.contains("28 November 2026 to 8 December 2026"))
        assertTrue(paymentRequest.contains("GM HASMANI COMMITTEE"))

        val expectedReportTypes = listOf(
            PdfReportType.INDIVIDUAL_MEMBER,
            PdfReportType.COMPLETE_MEMBER_LIST,
            PdfReportType.MONTHLY_COLLECTION,
            PdfReportType.PENDING_PAYMENTS,
            PdfReportType.COMPLETED_PAYMENTS,
            PdfReportType.INDIVIDUAL_COMMITTEE,
            PdfReportType.COMMITTEE_PAYOUT_REPORT,
            PdfReportType.PREVIOUS_COMMITTEE_HISTORY,
            PdfReportType.COMPLETE_FINANCIAL_SUMMARY
        )
        assertEquals(9, expectedReportTypes.size)
        assertTrue(PdfReportType.entries.containsAll(expectedReportTypes))
    }

    @Test
    fun `known committee history seeds only when empty and preserves unknown dates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(db)
            val seededId = repo.ensureKnownInitialCommitteeIfEmpty()
            assertTrue(!seededId.isNullOrBlank())

            val committees = repo.observeCommittees().first()
            assertEquals(1, committees.size)
            assertEquals(10L, committees.first().effectiveTotalShares)
            assertEquals(50_000L, committees.first().monthlyCollectionTotal)

            val turns = repo.observeTurns(seededId!!).first().sortedBy { it.turnNumber }
            assertEquals(4, turns.size)
            assertEquals("Ghulam Mustafa", turns[0].recipientName)
            assertEquals(50_000L, turns[0].payoutAmount)
            assertEquals(PayoutStatus.PAID.name, turns[0].status)
            assertNull(turns[0].payoutDate)

            assertEquals("Abdul Aleem", turns[1].recipientName)
            assertEquals(PayoutStatus.PAID.name, turns[1].status)
            assertNull(turns[1].payoutDate)

            assertEquals("Abdul Aleem", turns[2].recipientName)
            assertEquals(PayoutStatus.PAID.name, turns[2].status)
            assertNull(turns[2].payoutDate)

            assertEquals("Upcoming", turns[3].recipientName)
            assertEquals(PayoutStatus.PENDING.name, turns[3].status)
            assertEquals("2026-11", turns[3].scheduledMonth)

            // Calling seed again must be a strict no-op (non-destructive)
            val secondSeed = repo.ensureKnownInitialCommitteeIfEmpty()
            assertNull(secondSeed)
            assertEquals(1, repo.observeCommittees().first().size)
        } finally {
            db.close()
        }
    }
}
