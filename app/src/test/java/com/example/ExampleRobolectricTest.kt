package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MessagingGateway
import com.example.data.PdfReportType
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.BackupUtils
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.ReminderStage
import org.junit.Assert.assertEquals
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
        assertEquals("GM Hasmani Committee Manager", appName)
    }

    @Test
    fun `backup preview parses exported json counts and rejects invalid json`() {
        val sampleJson = """
            {
              "brand": "GM HASMANI COMMITTEE MANAGER",
              "version": 1,
              "exportedAt": "2026-11-26",
              "committees": [
                {
                  "id": "c1",
                  "name": "Family Circle",
                  "members": [{"id": "m1"}, {"id": "m2"}],
                  "installments": [{"id": "i1"}, {"id": "i2"}, {"id": "i3"}]
                }
              ]
            }
        """.trimIndent()
        val preview = BackupUtils.parsePreview(sampleJson).getOrThrow()
        assertEquals(1, preview.committeeCount)
        assertEquals(2, preview.memberCount)
        assertEquals(3, preview.installmentCount)
        assertEquals("2026-11-26", preview.exportedAt)

        assertTrue(BackupUtils.parsePreview("{not valid json").isFailure)
    }

    @Test
    fun `localization strings and reminder templates work for both English and Urdu`() {
        val en = stringsFor(AppLanguage.EN)
        val ur = stringsFor(AppLanguage.UR)
        assertTrue(en.totalActiveCommittees.isNotBlank())
        assertTrue(ur.totalActiveCommittees.isNotBlank())
        assertTrue(en.confirmRestoreTitle.isNotBlank())
        assertTrue(ur.confirmRestoreTitle.isNotBlank())

        val gateway = MessagingGateway()
        val committee = Committee(name = "Hasmani Circle", memberCount = 10L, monthlyInstallment = 10000L)
        val member = CommitteeMember(name = "Tariq", phone = "03001234567", committeeNumber = 1L)
        val renderedUr = gateway.renderMessage(
            template = ReminderStage.FIRST_28.defaultTemplateUr,
            member = member,
            committee = committee,
            monthKey = "2026-11",
            dueAmount = 10000L
        )
        assertTrue(renderedUr.contains("Tariq"))
        assertTrue(renderedUr.contains("Hasmani Circle"))
        assertTrue(renderedUr.contains("PKR 10,000"))

        // Verify all 7+ report types are defined
        val expectedReportTypes = listOf(
            PdfReportType.COLLECTION_REPORT,
            PdfReportType.MEMBER_LIST,
            PdfReportType.MONTHLY_PAYMENT,
            PdfReportType.PAID_MEMBERS_REPORT,
            PdfReportType.UNPAID_MEMBERS_REPORT,
            PdfReportType.PAYOUT_REPORT,
            PdfReportType.COMPLETE_SUMMARY,
            PdfReportType.INDIVIDUAL_HISTORY
        )
        assertTrue(PdfReportType.entries.containsAll(expectedReportTypes))
    }
}
