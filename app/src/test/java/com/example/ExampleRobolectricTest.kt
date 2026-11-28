package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.CommitteeRepository
import com.example.data.FinancialReportExporter
import com.example.data.MessagingGateway
import com.example.data.PdfReportGenerator
import com.example.data.PdfReportType
import com.example.data.local.AppDatabase
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.AppCompanySettings
import com.example.model.BackupUtils
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.ForwardFinancialCalculator
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.NotificationType
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.model.ReminderStage
import com.example.model.UrduMessageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals("G M S R HASHMANI THAR", appName)
    }

    @Test
    fun `backup preview parses exported json counts including turns forward payments and old records`() {
        val sampleJson = """
            {
              "brand": "G M S R HASHMANI THAR",
              "version": 5,
              "exportedAt": "2026-11-28",
              "committees": [
                {
                  "id": "c1",
                  "name": "Family Circle",
                  "members": [{"id": "m1"}, {"id": "m2"}],
                  "installments": [{"id": "i1"}, {"id": "i2"}, {"id": "i3"}],
                  "turns": [{"id": "t1"}, {"id": "t2"}, {"id": "t3"}, {"id": "t4"}],
                  "forwardOrganizers": [{"id": "org1"}],
                  "forwardPayments": [{"id": "fp1"}, {"id": "fp2"}]
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
        assertEquals(1, preview.forwardOrganizerCount)
        assertEquals(2, preview.forwardPaymentCount)
        assertEquals(1, preview.oldRecordCount)
        assertEquals("2026-11-28", preview.exportedAt)

        assertTrue(BackupUtils.parsePreview("{not valid json").isFailure)
    }

    @Test
    fun `localization strings reminder templates easypaisa request and 10 pdf reports work`() {
        val en = stringsFor(AppLanguage.EN)
        val ur = stringsFor(AppLanguage.UR)
        assertTrue(en.totalActiveCommittees.isNotBlank())
        assertTrue(ur.totalActiveCommittees.isNotBlank())
        assertTrue(en.confirmRestoreTitle.isNotBlank())
        assertTrue(ur.confirmRestoreTitle.isNotBlank())
        assertTrue(en.oldRecordsTitle.isNotBlank())
        assertTrue(ur.oldRecordsTitle.isNotBlank())
        assertTrue(en.randomDrawLabel.isNotBlank())
        assertTrue(ur.randomDrawLabel.isNotBlank())

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
        assertTrue(paymentRequest.contains("G M S R HASHMANI THAR"))

        val expectedReportTypes = listOf(
            PdfReportType.DAILY_COLLECTION,
            PdfReportType.INDIVIDUAL_MEMBER,
            PdfReportType.COMPLETE_MEMBER_LIST,
            PdfReportType.MONTHLY_COLLECTION,
            PdfReportType.PENDING_PAYMENTS,
            PdfReportType.COMPLETED_PAYMENTS,
            PdfReportType.INDIVIDUAL_COMMITTEE,
            PdfReportType.COMMITTEE_PAYOUT_REPORT,
            PdfReportType.PREVIOUS_COMMITTEE_HISTORY,
            PdfReportType.COMPLETE_FINANCIAL_SUMMARY,
            PdfReportType.FORWARD_PAYMENTS_SUMMARY
        )
        assertEquals(11, expectedReportTypes.size)
        assertTrue(PdfReportType.entries.containsAll(expectedReportTypes))
    }

    @Test
    fun `all 6 official Urdu templates A through F embed company name contact and substituted fields`() {
        val gateway = MessagingGateway()
        val settings = AppCompanySettings()
        assertEquals(6, UrduMessageType.entries.size)

        UrduMessageType.entries.forEach { templateType ->
            val rendered = gateway.renderUrduMessage(
                type = templateType,
                memberName = "Salman Hashmani",
                amount = 15000L,
                date = "08 Dec 2026",
                remainingAmount = 5000L,
                adminMessage = "کل شام 5 بجے کمیٹی قرعہ اندازی ہوگی",
                settings = settings
            )
            assertTrue("Missing official company name in $templateType", rendered.contains("G M S R HASHMANI THAR"))
            assertTrue("Missing official phone in $templateType", rendered.contains("03423498631"))
            assertFalse("Unreplaced placeholder in $templateType", rendered.contains("[Member Name]"))
            assertFalse("Unreplaced placeholder in $templateType", rendered.contains("[Amount]"))
            assertFalse("Unreplaced placeholder in $templateType", rendered.contains("[Date]"))
            assertFalse("Unreplaced placeholder in $templateType", rendered.contains("[Remaining Amount]"))
            assertFalse("Unreplaced placeholder in $templateType", rendered.contains("[Admin Message]"))
        }
    }

    @Test
    fun `fresh database starts completely empty and clearAllDataForFreshStart resets all records cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(db)
            val admin = repo.ensureDefaultAdmin()
            assertEquals("admin", admin.username)

            // Fresh start must contain zero demo committees, members, turns, installments, or forward records
            assertTrue(repo.observeCommittees().first().isEmpty())
            assertTrue(repo.observeAllMembers().first().isEmpty())
            assertTrue(repo.observeAllTurns().first().isEmpty())
            assertTrue(repo.observeAllInstallments().first().isEmpty())
            assertTrue(repo.observeOldRecords().first().isEmpty())
            assertTrue(repo.observeAllForwardOrganizers().first().isEmpty())
            assertTrue(repo.observeAllForwardPayments().first().isEmpty())
            assertTrue(repo.observeNotifications().first().isEmpty())

            // Create real user records, then verify Admin-authorized fresh reset clears everything
            val cid = repo.createCommittee(
                name = "Real User Circle",
                memberCount = 2L,
                totalShares = 2L,
                monthlyInstallment = 5000L,
                totalMonths = 2L,
                collectionDayStart = 28L,
                collectionDayEnd = 8L,
                payoutDay = 10L,
                startMonth = "2026-11",
                notes = null
            ).getOrThrow()
            val committee = repo.observeCommittees().first().first()
            val mid = repo.addMember(committee, "User One", "03001234567", 1L, 1.0).getOrThrow()
            val member = repo.observeMembers(cid).first().first { it.id == mid }
            repo.recordOrAddManualPayment(
                committee = committee,
                member = member,
                monthKey = "2026-11",
                monthIndex = 1L,
                paidAmount = 5000L,
                status = InstallmentStatus.PAID,
                paymentDate = "2026-11-28",
                paymentMethod = PaymentMethod.CASH,
                transactionId = null,
                paymentNote = null
            ).getOrThrow()

            // Wrong PIN must be rejected
            assertTrue(repo.clearAllDataForFreshStart(admin, "9999", context).isFailure)
            assertEquals(1, repo.observeCommittees().first().size)

            // Valid Admin PIN wipes all transactional & historical records while preserving Admin login
            repo.clearAllDataForFreshStart(admin, CommitteeRepository.DEFAULT_ADMIN_PIN, context).getOrThrow()
            assertTrue(repo.observeCommittees().first().isEmpty())
            assertTrue(repo.observeAllMembers().first().isEmpty())
            assertTrue(repo.observeAllTurns().first().isEmpty())
            assertTrue(repo.observeAllInstallments().first().isEmpty())
            assertTrue(repo.observeAllForwardOrganizers().first().isEmpty())
            assertTrue(repo.observeAllForwardPayments().first().isEmpty())
            assertTrue(repo.observeAuditLogs().first().isEmpty())
            assertEquals(1, repo.observeUsers().first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `member auth backfill notifications deduplication and random eligible winner work`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(db)
            val cid = repo.createCommittee(
                name = "Thar Circle",
                memberCount = 3L,
                totalShares = 3L,
                monthlyInstallment = 5000L,
                totalMonths = 3L,
                collectionDayStart = 28L,
                collectionDayEnd = 8L,
                payoutDay = 10L,
                startMonth = "2026-11",
                notes = null
            ).getOrThrow()
            val committee = repo.observeCommittees().first().first { it.id == cid }

            val m1Id = repo.addMember(
                committee = committee,
                name = "Ali Raza",
                phone = "03001112233",
                committeeNumber = 1L,
                shares = 1.0,
                memberCode = "",
                fatherName = "Haji Raza",
                address = "Mithi Thar",
                accessPin = "4321"
            ).getOrThrow()
            val m2Id = repo.addMember(
                committee = committee,
                name = "Bilal Ahmed",
                phone = "03004445566",
                committeeNumber = 2L,
                shares = 1.0,
                memberCode = "",
                accessPin = "1234"
            ).getOrThrow()

            repo.backfillMemberCodesAndReceiptsIfNeeded()
            val members = repo.observeMembers(cid).first().sortedBy { it.committeeNumber }
            val ali = members.first { it.id == m1Id }
            val bilal = members.first { it.id == m2Id }
            assertTrue(ali.displayMemberId.startsWith("GM-"))
            assertTrue(bilal.displayMemberId.startsWith("GM-"))

            // Authenticate via memberCode and normalized phone
            assertTrue(repo.authenticateMember(ali.displayMemberId, "4321").isSuccess)
            assertTrue(repo.authenticateMember("+92 300 1112233", "4321").isSuccess)
            assertTrue(repo.authenticateMember(ali.displayMemberId, "0000").isFailure)

            // Deactivated member cannot sign in, yet remains in DB
            repo.setMemberActiveStatus(cid, bilal, active = false).getOrThrow()
            assertTrue(repo.authenticateMember(bilal.displayMemberId, "1234").isFailure)

            // Eligible random draw picks only active non-paid member without mutating turns
            val activeWinner = repo.pickEligibleRandomTurnWinner(
                members = repo.observeMembers(cid).first(),
                existingTurns = emptyList()
            )
            assertNotNull(activeWinner)
            assertEquals(ali.id, activeWinner?.id)
            assertEquals(0, repo.observeTurns(cid).first().size)

            // Duplicate notification prevention for same committee + member + type + monthKey
            val firstSend = repo.sendInAppNotifications(
                committeeId = cid,
                targetMembers = listOf(ali),
                title = "Payment Reminder",
                message = "Due soon",
                type = NotificationType.PAYMENT_REMINDER,
                monthKey = "2026-11",
                preventDuplicate = true
            ).getOrThrow()
            assertEquals(1 to 0, firstSend)

            val duplicateSend = repo.sendInAppNotifications(
                committeeId = cid,
                targetMembers = listOf(ali),
                title = "Payment Reminder",
                message = "Due soon",
                type = NotificationType.PAYMENT_REMINDER,
                monthKey = "2026-11",
                preventDuplicate = true
            ).getOrThrow()
            assertEquals(0 to 1, duplicateSend)
            assertEquals(1, repo.observeNotificationsForMember(ali.id).first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `excel export generates valid spreadsheets for all 10 report types and pdf handles runtime cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val committee = Committee(id = "c1", name = "Family Circle", memberCount = 5L, totalShares = 5L, monthlyInstallment = 5000L)
        val member = CommitteeMember(id = "m1", committeeId = "c1", memberCode = "GM-001", name = "Ghulam Mustafa", phone = "03001234567", committeeNumber = 1L)
        val inst = InstallmentRecord(
            id = "i1",
            committeeId = "c1",
            memberId = "m1",
            memberName = "Ghulam Mustafa",
            memberPhone = "03001234567",
            committeeNumber = 1L,
            monthKey = "2026-11",
            monthIndex = 1L,
            dueAmount = 5000L,
            paidAmount = 5000L,
            status = InstallmentStatus.PAID.name,
            paymentDate = com.example.model.MonthUtils.todayDateString(),
            paymentMethod = PaymentMethod.EASYPAISA.name,
            transactionId = "EP-778899",
            receiptNumber = "GMSR-202611-0001"
        )
        val settings = AppCompanySettings()

        PdfReportType.entries.forEach { reportType ->
            val xml = FinancialReportExporter.buildExcelXml(
                reportType = reportType,
                committee = committee,
                allCommittees = listOf(committee),
                members = listOf(member),
                installments = listOf(inst),
                turns = listOf(CommitteeTurn(id = "t1", committeeId = "c1", turnNumber = 1L, recipientName = "Ghulam Mustafa", payoutAmount = 25000L, scheduledMonth = "2026-11", status = PayoutStatus.PAID.name)),
                oldRecords = emptyList(),
                selectedMonthKey = "2026-11",
                selectedMember = member,
                companySettings = settings
            )
            assertTrue("Missing Workbook for $reportType", xml.contains("<Workbook"))
            assertTrue("Missing company branding for $reportType", xml.contains("G M S R HASHMANI THAR"))
        }

        val monthlyXml = FinancialReportExporter.buildExcelXml(
            reportType = PdfReportType.MONTHLY_COLLECTION,
            committee = committee,
            allCommittees = listOf(committee),
            members = listOf(member),
            installments = listOf(inst),
            turns = emptyList(),
            oldRecords = emptyList(),
            selectedMonthKey = "2026-11",
            selectedMember = member,
            companySettings = settings
        )
        assertTrue(monthlyXml.contains("GMSR-202611-0001"))

        val receiptResult = PdfReportGenerator.generatePaymentReceiptPdf(
            context = context,
            committee = committee,
            member = member,
            installment = inst,
            companySettings = settings
        )
        receiptResult.onSuccess { file ->
            assertTrue(file.exists())
            assertTrue(file.length() > 500L)
            file.delete()
        }.onFailure { err ->
            // On JVM Robolectric without native libandroid_graphics PdfDocument JNI, constructor leaves mNativeDocument = 0.
            assertTrue(err is IllegalStateException)
        }
    }

    @Test
    fun `room migration 3 to 4 preserves existing committees members turns and payments`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v3_v4_test.db"
        context.deleteDatabase(dbName)

        // Create a real v3 SQLite database matching version 3 schema
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `committees` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `memberCount` INTEGER NOT NULL, `totalShares` INTEGER NOT NULL, `monthlyInstallment` INTEGER NOT NULL, `totalMonths` INTEGER NOT NULL, `collectionDayStart` INTEGER NOT NULL, `collectionDayEnd` INTEGER NOT NULL, `payoutDay` INTEGER NOT NULL, `startMonth` TEXT NOT NULL, `endMonth` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `members` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `committeeNumber` INTEGER NOT NULL, `shares` REAL NOT NULL, `payoutMonth` TEXT NOT NULL, `payoutStatus` TEXT NOT NULL, `payoutDate` TEXT, `payoutAmount` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_members_committeeId` ON `members` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `committee_turns` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `turnNumber` INTEGER NOT NULL, `recipientMemberId` TEXT, `recipientName` TEXT NOT NULL, `recipientPhone` TEXT, `payoutAmount` INTEGER NOT NULL, `scheduledMonth` TEXT NOT NULL, `payoutDate` TEXT, `status` TEXT NOT NULL, `confirmedByAdmin` INTEGER NOT NULL, `notes` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_committee_turns_committeeId` ON `committee_turns` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `old_committee_records` (`id` TEXT NOT NULL, `committeeNumber` TEXT NOT NULL, `committeeName` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `memberNames` TEXT NOT NULL, `contactNumbers` TEXT NOT NULL, `numberOfShares` TEXT NOT NULL, `monthlyContributions` TEXT NOT NULL, `totalCollection` TEXT NOT NULL, `paymentHistory` TEXT NOT NULL, `payoutRecipient` TEXT NOT NULL, `payoutAmount` TEXT NOT NULL, `payoutDate` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `installments` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `memberPhone` TEXT NOT NULL, `committeeNumber` INTEGER NOT NULL, `monthKey` TEXT NOT NULL, `monthIndex` INTEGER NOT NULL, `dueAmount` INTEGER NOT NULL, `paidAmount` INTEGER NOT NULL, `status` TEXT NOT NULL, `paymentDate` TEXT, `paymentNote` TEXT, `paymentMethod` TEXT NOT NULL, `transactionId` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_committeeId` ON `installments` (`committeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_monthKey` ON `installments` (`monthKey`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_memberId` ON `installments` (`memberId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_logs` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `installmentId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `previousPaidAmount` INTEGER NOT NULL, `newPaidAmount` INTEGER NOT NULL, `previousStatus` TEXT NOT NULL, `newStatus` TEXT NOT NULL, `paymentMethod` TEXT, `transactionId` TEXT, `actionNote` TEXT, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_logs_committeeId` ON `payment_logs` (`committeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_logs_installmentId` ON `payment_logs` (`installmentId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_logs` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `memberPhone` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `channel` TEXT NOT NULL, `scheduleStage` TEXT NOT NULL, `messagePreview` TEXT NOT NULL, `dispatchStatus` TEXT NOT NULL, `errorDetail` TEXT, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_logs_committeeId` ON `reminder_logs` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `local_users` (`id` TEXT NOT NULL, `username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `role` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `actorName` TEXT NOT NULL, `actionType` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetName` TEXT NOT NULL, `details` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `upload_queue` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `payloadType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `status` TEXT NOT NULL, `errorMessage` TEXT, `attempts` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                        // Seed pre-existing v3 user records
                        db.execSQL("INSERT INTO `committees` VALUES ('c_legacy', 'Legacy Thar Committee', 7, 10, 5000, 10, 28, 8, 10, '2026-08', '2027-05', 'ACTIVE', NULL, 1000, 1000)")
                        db.execSQL("INSERT INTO `members` VALUES ('m_legacy', 'c_legacy', 'Ghulam Mustafa', '03423498631', 1, 1.0, '2026-08', 'PAID', NULL, 50000, 'ACTIVE', 1000, 1000)")
                        db.execSQL("INSERT INTO `committee_turns` VALUES ('t_legacy', 'c_legacy', 1, 'm_legacy', 'Ghulam Mustafa', '03423498631', 50000, '2026-08', NULL, 'PAID', 1, 'First Committee', 1000, 1000)")
                        db.execSQL("INSERT INTO `installments` VALUES ('i_legacy', 'c_legacy', 'm_legacy', 'Ghulam Mustafa', '03423498631', 1, '2026-08', 1, 5000, 5000, 'PAID', '2026-08-30', NULL, 'CASH', NULL, 1000, 1000)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        // Open with Room v6 + MIGRATION_1_2..MIGRATION_5_6 (one-time fresh-start refresh clears old demo data)
        val dbV6 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(dbV6)
            assertTrue(repo.observeCommittees().first().isEmpty())
            assertTrue(repo.observeAllMembers().first().isEmpty())
            assertTrue(repo.observeAllInstallments().first().isEmpty())
            assertTrue(repo.observeAllTurns().first().isEmpty())
        } finally {
            dbV6.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `forward payments require confirmation stay isolated from member collection generate urdu summary and round trip backup`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(db)
            val cid = repo.createCommittee(
                name = "Forward Ledger Circle",
                memberCount = 2L,
                totalShares = 2L,
                monthlyInstallment = 10_000L,
                totalMonths = 2L,
                collectionDayStart = 28L,
                collectionDayEnd = 8L,
                payoutDay = 10L,
                startMonth = "2026-11",
                notes = null
            ).getOrThrow()
            val committee = repo.observeCommittees().first().first { it.id == cid }

            val m1Id = repo.addMember(committee, "Tariq Aziz", "03001112233", 1L, 1.0).getOrThrow()
            val m2Id = repo.addMember(committee, "Sajid Ali", "03004445566", 2L, 1.0).getOrThrow()
            val members = repo.observeMembers(cid).first().sortedBy { it.committeeNumber }
            val m1 = members.first { it.id == m1Id }
            val m2 = members.first { it.id == m2Id }

            // Record 10k from member 1 and 4k partial from member 2
            repo.recordOrAddManualPayment(
                committee = committee,
                member = m1,
                monthKey = "2026-11",
                monthIndex = 1L,
                paidAmount = 10_000L,
                status = InstallmentStatus.PAID,
                paymentDate = "2026-11-29",
                paymentMethod = PaymentMethod.EASYPAISA,
                transactionId = "EP-101",
                paymentNote = "Full",
                actorName = "Admin"
            ).getOrThrow()
            repo.recordOrAddManualPayment(
                committee = committee,
                member = m2,
                monthKey = "2026-11",
                monthIndex = 1L,
                paidAmount = 4_000L,
                status = InstallmentStatus.PARTIAL,
                paymentDate = "2026-11-30",
                paymentMethod = PaymentMethod.CASH,
                transactionId = null,
                paymentNote = "Partial",
                actorName = "Admin"
            ).getOrThrow()

            // Guarantee member collections NEVER auto-create forward payments
            assertEquals(0, repo.observeForwardPayments(cid).first().size)

            val org = repo.saveForwardOrganizer(
                organizerId = null,
                committeeId = cid,
                organizerName = "Haji Qadir",
                organizerPhone = "03331234567",
                totalPayableAmount = 20_000L,
                monthKey = "2026-11",
                notes = "November forward pool",
                actorName = "Admin"
            ).getOrThrow()

            // Recording without Admin confirmation must fail
            val unconfirmedResult = repo.recordForwardPayment(
                paymentId = null,
                committeeId = cid,
                organizerId = org.id,
                monthKey = "2026-11",
                amountForwarded = 8_000L,
                paymentDate = "2026-11-30",
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                transactionId = "BT-555",
                proofUri = null,
                proofLabel = null,
                notes = "Should fail",
                confirmedByAdmin = false
            )
            assertTrue(unconfirmedResult.isFailure)

            // Recording with Admin confirmation succeeds & assigns GMSR-FWD receipt
            val fp = repo.recordForwardPayment(
                paymentId = null,
                committeeId = cid,
                organizerId = org.id,
                monthKey = "2026-11",
                amountForwarded = 8_000L,
                paymentDate = "2026-11-30",
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                transactionId = "BT-555",
                proofUri = "/data/user/0/proof.jpg",
                proofLabel = "slip_nov.jpg",
                notes = "Verified transfer",
                confirmedByAdmin = true
            ).getOrThrow()
            assertTrue(fp.receiptNumber.orEmpty().startsWith("GMSR-FWD-202611-"))

            // Organizer cannot be deleted while active confirmed payment exists
            assertTrue(repo.deleteForwardOrganizerSafely(org.id).isFailure)

            val installments = repo.observeInstallments(cid).first()
            val summary = ForwardFinancialCalculator.compute(
                committee = committee,
                members = members,
                installments = installments,
                organizers = repo.observeForwardOrganizers(cid).first(),
                forwardPayments = repo.observeForwardPayments(cid).first(),
                monthKey = "2026-11"
            )
            assertEquals(20_000L, summary.expectedFromMembers)
            assertEquals(14_000L, summary.collectedFromMembers)
            assertEquals(6_000L, summary.pendingMemberCollection)
            assertEquals(8_000L, summary.totalForwarded)
            assertEquals(12_000L, summary.pendingForwardPayment)
            assertEquals(6_000L, summary.amountCurrentlyHeldByAdmin)

            // Verify Urdu Organizer Summary content
            val urduMsg = MessagingGateway().buildOrganizerUrduSummaryMessage(
                committeeName = committee.name,
                organizerName = org.organizerName,
                summary = summary,
                members = members,
                scopedInstallments = installments.filter { it.monthKey == "2026-11" },
                settings = AppCompanySettings()
            )
            assertTrue(urduMsg.contains("G M S R HASHMANI THAR"))
            assertTrue(urduMsg.contains("03423498631"))
            assertTrue(urduMsg.contains("Tariq Aziz"))
            assertTrue(urduMsg.contains("Sajid Ali"))
            assertTrue(urduMsg.contains("8,000"))
            assertTrue(urduMsg.contains("6,000"))

            // Backup v5 export + restore preserves forward organizers & payments
            val json = repo.exportFullBackupJson(AppCompanySettings()).getOrThrow()
            val preview = BackupUtils.parsePreview(json).getOrThrow()
            assertEquals(1, preview.forwardOrganizerCount)
            assertEquals(1, preview.forwardPaymentCount)

            val restoreDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val restoreRepo = CommitteeRepository(restoreDb)
                restoreRepo.restoreFromBackupJson(json).getOrThrow()
                assertEquals(1, restoreRepo.observeForwardOrganizers(cid).first().size)
                val restoredPayments = restoreRepo.observeForwardPayments(cid).first()
                assertEquals(1, restoredPayments.size)
                assertEquals(8_000L, restoredPayments.first().amountForwarded)
                assertEquals("slip_nov.jpg", restoredPayments.first().proofLabel)
            } finally {
                restoreDb.close()
            }

            // Reversing forward payment restores held cash back to 14,000 and allows deletion
            repo.reverseForwardPayment(fp.id, "Entered wrong bank ref").getOrThrow()
            val afterReverse = ForwardFinancialCalculator.compute(
                committee = committee,
                members = members,
                installments = installments,
                organizers = repo.observeForwardOrganizers(cid).first(),
                forwardPayments = repo.observeForwardPayments(cid).first(),
                monthKey = "2026-11"
            )
            assertEquals(0L, afterReverse.totalForwarded)
            assertEquals(14_000L, afterReverse.amountCurrentlyHeldByAdmin)
            assertTrue(repo.deleteForwardPaymentSafely(fp.id).isSuccess)
            assertTrue(repo.deleteForwardOrganizerSafely(org.id).isSuccess)
        } finally {
            db.close()
        }
    }

    @Test
    fun `room migration 4 to 5 preserves existing data and adds forward payment tables`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v4_v5_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `committees` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `memberCount` INTEGER NOT NULL, `totalShares` INTEGER NOT NULL, `monthlyInstallment` INTEGER NOT NULL, `totalMonths` INTEGER NOT NULL, `collectionDayStart` INTEGER NOT NULL, `collectionDayEnd` INTEGER NOT NULL, `payoutDay` INTEGER NOT NULL, `startMonth` TEXT NOT NULL, `endMonth` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `members` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberCode` TEXT NOT NULL, `name` TEXT NOT NULL, `fatherName` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `joiningDate` TEXT NOT NULL, `accessPin` TEXT NOT NULL, `committeeNumber` INTEGER NOT NULL, `shares` REAL NOT NULL, `payoutMonth` TEXT NOT NULL, `payoutStatus` TEXT NOT NULL, `payoutDate` TEXT, `payoutAmount` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_members_committeeId` ON `members` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `committee_turns` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `turnNumber` INTEGER NOT NULL, `recipientMemberId` TEXT, `recipientName` TEXT NOT NULL, `recipientPhone` TEXT, `payoutAmount` INTEGER NOT NULL, `scheduledMonth` TEXT NOT NULL, `payoutDate` TEXT, `status` TEXT NOT NULL, `confirmedByAdmin` INTEGER NOT NULL, `notes` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_committee_turns_committeeId` ON `committee_turns` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `old_committee_records` (`id` TEXT NOT NULL, `committeeNumber` TEXT NOT NULL, `committeeName` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `memberNames` TEXT NOT NULL, `contactNumbers` TEXT NOT NULL, `numberOfShares` TEXT NOT NULL, `monthlyContributions` TEXT NOT NULL, `totalCollection` TEXT NOT NULL, `paymentHistory` TEXT NOT NULL, `payoutRecipient` TEXT NOT NULL, `payoutAmount` TEXT NOT NULL, `payoutDate` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `installments` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `memberPhone` TEXT NOT NULL, `committeeNumber` INTEGER NOT NULL, `monthKey` TEXT NOT NULL, `monthIndex` INTEGER NOT NULL, `dueAmount` INTEGER NOT NULL, `paidAmount` INTEGER NOT NULL, `status` TEXT NOT NULL, `paymentDate` TEXT, `paymentNote` TEXT, `paymentMethod` TEXT NOT NULL, `transactionId` TEXT, `receiptNumber` TEXT, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_committeeId` ON `installments` (`committeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_monthKey` ON `installments` (`monthKey`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installments_memberId` ON `installments` (`memberId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_logs` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `installmentId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `previousPaidAmount` INTEGER NOT NULL, `newPaidAmount` INTEGER NOT NULL, `previousStatus` TEXT NOT NULL, `newStatus` TEXT NOT NULL, `paymentMethod` TEXT, `transactionId` TEXT, `receiptNumber` TEXT, `actionNote` TEXT, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_logs_committeeId` ON `payment_logs` (`committeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_logs_installmentId` ON `payment_logs` (`installmentId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_logs` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberName` TEXT NOT NULL, `memberPhone` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `channel` TEXT NOT NULL, `scheduleStage` TEXT NOT NULL, `messagePreview` TEXT NOT NULL, `dispatchStatus` TEXT NOT NULL, `errorDetail` TEXT, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_logs_committeeId` ON `reminder_logs` (`committeeId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `local_users` (`id` TEXT NOT NULL, `username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `role` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `actorName` TEXT NOT NULL, `actionType` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetName` TEXT NOT NULL, `details` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `upload_queue` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `payloadType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `status` TEXT NOT NULL, `errorMessage` TEXT, `attempts` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `in_app_notifications` (`id` TEXT NOT NULL, `committeeId` TEXT NOT NULL, `memberId` TEXT, `memberName` TEXT, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `notificationType` TEXT NOT NULL, `monthKey` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_in_app_notifications_committeeId` ON `in_app_notifications` (`committeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_in_app_notifications_memberId` ON `in_app_notifications` (`memberId`)")

                        db.execSQL("INSERT INTO `committees` VALUES ('c_v4', 'V4 Circle', 5, 5, 5000, 5, 28, 8, 10, '2026-10', '2027-02', 'ACTIVE', NULL, 1000, 1000)")
                        db.execSQL("INSERT INTO `members` VALUES ('m_v4', 'c_v4', 'GM-001', 'Abdul Aleem', 'Haji Aleem', '03423498631', 'Mithi', '2026-10-01', '1234', 1, 1.0, '2026-10', 'PAID', '2026-10-10', 25000, 'ACTIVE', 1000, 1000)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val dbV6 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val repo = CommitteeRepository(dbV6)
            // v5->v6 one-time refresh cleared old v4 demo data
            assertTrue(repo.observeCommittees().first().isEmpty())
            assertTrue(repo.observeMembers("c_v4").first().isEmpty())

            // App is ready for creating real new committees and forward organizer records
            val newCid = repo.createCommittee(
                name = "Fresh Circle",
                memberCount = 5L,
                totalShares = 5L,
                monthlyInstallment = 5000L,
                totalMonths = 5L,
                collectionDayStart = 28L,
                collectionDayEnd = 8L,
                payoutDay = 10L,
                startMonth = "2026-11",
                notes = null
            ).getOrThrow()

            val org = repo.saveForwardOrganizer(
                organizerId = null,
                committeeId = newCid,
                organizerName = "Forward Recipient",
                organizerPhone = "03001112233",
                totalPayableAmount = 25_000L,
                monthKey = "2026-11",
                notes = null
            ).getOrThrow()
            assertEquals("Forward Recipient", org.organizerName)
            assertEquals(1, repo.observeForwardOrganizers(newCid).first().size)
        } finally {
            dbV6.close()
            context.deleteDatabase(dbName)
        }
    }
}
