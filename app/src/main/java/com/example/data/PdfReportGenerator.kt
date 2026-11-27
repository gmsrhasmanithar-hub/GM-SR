package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.CommitteeTurn
import com.example.model.DashboardSummary
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfReportType {
    INDIVIDUAL_MEMBER,
    COMPLETE_MEMBER_LIST,
    MONTHLY_COLLECTION,
    PENDING_PAYMENTS,
    COMPLETED_PAYMENTS,
    INDIVIDUAL_COMMITTEE,
    COMMITTEE_PAYOUT_REPORT,
    PREVIOUS_COMMITTEE_HISTORY,
    COMPLETE_FINANCIAL_SUMMARY
}

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595 // A4 width in PostScript points
    private const val PAGE_HEIGHT = 842 // A4 height
    private const val MARGIN = 36f

    fun generateReport(
        context: Context,
        reportType: PdfReportType,
        committee: Committee?,
        allCommittees: List<Committee> = emptyList(),
        members: List<CommitteeMember> = emptyList(),
        installments: List<InstallmentRecord> = emptyList(),
        turns: List<CommitteeTurn> = emptyList(),
        oldRecords: List<OldCommitteeRecord> = emptyList(),
        selectedMonthKey: String = MonthUtils.currentMonthKey(),
        selectedMember: CommitteeMember? = null,
        summary: DashboardSummary? = null,
        companySettings: AppCompanySettings = AppCompanySettings()
    ): Result<File> {
        val doc = PdfDocument()
        return try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "GM_HASMANI_${reportType.name}_$stamp.pdf"
            val outFile = File(dir, fileName)

            val headerCommitteeLabel = when (reportType) {
                PdfReportType.PREVIOUS_COMMITTEE_HISTORY -> "Historical Archive"
                PdfReportType.COMPLETE_FINANCIAL_SUMMARY -> "All Committees"
                else -> committee?.name ?: companySettings.companyName
            }
            val writer = PdfPageWriter(doc, headerCommitteeLabel, companySettings)
            writer.startNewPage(reportSubtitle(reportType, selectedMonthKey, selectedMember))

            val cur = companySettings.currency

            when (reportType) {
                PdfReportType.INDIVIDUAL_MEMBER ->
                    writeIndividualMemberReport(writer, committee, selectedMember, installments, turns, cur)
                PdfReportType.COMPLETE_MEMBER_LIST ->
                    writeCompleteMemberList(writer, committee, members, cur)
                PdfReportType.MONTHLY_COLLECTION ->
                    writeMonthlyCollectionReport(writer, selectedMonthKey, installments, cur)
                PdfReportType.PENDING_PAYMENTS ->
                    writePendingPaymentsReport(writer, selectedMonthKey, installments, cur)
                PdfReportType.COMPLETED_PAYMENTS ->
                    writeCompletedPaymentsReport(writer, selectedMonthKey, installments, cur)
                PdfReportType.INDIVIDUAL_COMMITTEE ->
                    writeIndividualCommitteeReport(writer, committee, members, installments, turns, summary, cur)
                PdfReportType.COMMITTEE_PAYOUT_REPORT ->
                    writeCommitteePayoutReport(writer, committee, members, turns, cur)
                PdfReportType.PREVIOUS_COMMITTEE_HISTORY ->
                    writePreviousCommitteeHistory(writer, oldRecords)
                PdfReportType.COMPLETE_FINANCIAL_SUMMARY ->
                    writeCompleteFinancialSummary(writer, committee, allCommittees, members, installments, turns, oldRecords, summary, cur)
            }

            writer.finish()
            FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { doc.close() }
        }
    }

    fun openOrSharePdf(context: Context, file: File, shareMode: Boolean): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = if (shareMode) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GM HASMANI Committee Report - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            val chooser = if (shareMode) {
                Intent.createChooser(intent, "Share GM HASMANI PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                intent
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun savePdfToUri(context: Context, sourceFile: File, targetUri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("Unable to open destination file."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device."))

            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(pdfFile.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    if (destination == null) {
                        callback?.onWriteFailed("Missing print destination.")
                        return
                    }
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.localizedMessage ?: "Print write error")
                    }
                }
            }
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun reportSubtitle(type: PdfReportType, monthKey: String, member: CommitteeMember?): String = when (type) {
        PdfReportType.INDIVIDUAL_MEMBER -> "Individual Member Report • ${member?.name ?: "Member"}"
        PdfReportType.COMPLETE_MEMBER_LIST -> "Complete Member List"
        PdfReportType.MONTHLY_COLLECTION -> "Monthly Collection Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.PENDING_PAYMENTS -> "Pending Payments Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.COMPLETED_PAYMENTS -> "Completed Payments Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.INDIVIDUAL_COMMITTEE -> "Individual Committee Report"
        PdfReportType.COMMITTEE_PAYOUT_REPORT -> "Committee Payout Turns Report"
        PdfReportType.PREVIOUS_COMMITTEE_HISTORY -> "Previous Committee History"
        PdfReportType.COMPLETE_FINANCIAL_SUMMARY -> "Complete Financial Summary"
    }

    private fun writeIndividualMemberReport(
        writer: PdfPageWriter,
        committee: Committee?,
        member: CommitteeMember?,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        cur: String
    ) {
        if (member == null || committee == null) {
            writer.drawInfoBox(listOf("No member selected for report."))
            return
        }
        val memberRows = installments.filter { it.memberId == member.id }.sortedBy { it.monthIndex }
        val totalPaid = memberRows.sumOf { it.paidAmount }
        val totalRemaining = memberRows.sumOf { it.remainingAmount }
        val monthlyContribution = member.monthlyDue(committee.monthlyInstallment)
        val memberTurns = turns.filter {
            it.recipientMemberId == member.id || it.recipientName.equals(member.name, ignoreCase = true)
        }

        writer.drawInfoBox(
            listOf(
                "Member: ${member.name} (#${member.committeeNumber}) • Mobile: ${member.phone}",
                "Shares: ${FormatUtils.formatShares(member.effectiveShares)} • Monthly Contribution: ${FormatUtils.formatMoney(monthlyContribution, cur)}",
                "Total Paid: ${FormatUtils.formatMoney(totalPaid, cur)} | Pending: ${FormatUtils.formatMoney(totalRemaining, cur)}"
            )
        )

        writer.drawSectionHeading("INSTALLMENT PAYMENT HISTORY")
        writer.drawTableHeader(listOf("Month" to 105f, "Due" to 85f, "Paid" to 85f, "Method" to 85f, "Date" to 80f, "Status" to 83f))
        memberRows.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "M${rec.monthIndex} ${MonthUtils.formatMonthDisplay(rec.monthKey)}" to 105f,
                    FormatUtils.formatCompactNumber(rec.dueAmount) to 85f,
                    FormatUtils.formatCompactNumber(rec.paidAmount) to 85f,
                    PaymentMethod.fromRaw(rec.paymentMethod).label to 85f,
                    (rec.paymentDate ?: "-") to 80f,
                    rec.status to 83f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }

        if (memberTurns.isNotEmpty()) {
            writer.drawSectionHeading("COMMITTEE PAYOUT TURNS (${memberTurns.size})")
            writer.drawTableHeader(listOf("Turn" to 55f, "Month" to 120f, "Payout Amount" to 130f, "Payout Date" to 118f, "Status" to 100f))
            memberTurns.forEach { t ->
                writer.drawTableRow(
                    listOf(
                        "Turn #${t.turnNumber}" to 55f,
                        MonthUtils.formatMonthDisplay(t.scheduledMonth) to 120f,
                        FormatUtils.formatMoney(t.payoutAmount, cur) to 130f,
                        (t.payoutDate?.takeIf { it.isNotBlank() } ?: "Unknown") to 118f,
                        t.status to 100f
                    ),
                    statusColor = if (t.statusEnum == PayoutStatus.PAID) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
                )
            }
        }
    }

    private fun writeCompleteMemberList(
        writer: PdfPageWriter,
        committee: Committee?,
        members: List<CommitteeMember>,
        cur: String
    ) {
        if (committee == null) {
            writer.drawInfoBox(listOf("No committee selected."))
            return
        }
        val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
        val archivedMembers = members.filter { it.memberStatusEnum == MemberStatus.ARCHIVED }.sortedBy { it.committeeNumber }
        val sumShares = activeMembers.sumOf { it.effectiveShares }
        writer.drawInfoBox(
            listOf(
                "Committee: ${committee.name} • Active Members: ${activeMembers.size} (Target ${committee.memberCount})",
                "Configured Shares: ${committee.effectiveTotalShares} | Assigned Member Shares: ${FormatUtils.formatShares(sumShares)} | Per Share: ${FormatUtils.formatMoney(committee.monthlyInstallment, cur)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 34f, "Member Name" to 155f, "Mobile Number" to 115f, "Shares" to 55f, "Monthly" to 95f, "Status" to 69f))
        activeMembers.forEach { m ->
            writer.drawTableRow(
                listOf(
                    "#${m.committeeNumber}" to 34f,
                    m.name to 155f,
                    m.phone to 115f,
                    FormatUtils.formatShares(m.effectiveShares) to 55f,
                    FormatUtils.formatMoney(m.monthlyDue(committee.monthlyInstallment), cur) to 95f,
                    m.status to 69f
                ),
                statusColor = Color.rgb(11, 110, 79)
            )
        }
        if (archivedMembers.isNotEmpty()) {
            writer.drawSectionHeading("Archived Members (${archivedMembers.size})")
            archivedMembers.forEach { m ->
                writer.drawTableRow(
                    listOf(
                        "#${m.committeeNumber}" to 34f,
                        m.name to 155f,
                        m.phone to 115f,
                        FormatUtils.formatShares(m.effectiveShares) to 55f,
                        FormatUtils.formatMoney(m.monthlyDue(committee.monthlyInstallment), cur) to 95f,
                        m.status to 69f
                    ),
                    statusColor = Color.GRAY
                )
            }
        }
    }

    private fun writeMonthlyCollectionReport(
        writer: PdfPageWriter,
        monthKey: String,
        installments: List<InstallmentRecord>,
        cur: String
    ) {
        val monthItems = installments.filter { it.monthKey == monthKey }.sortedBy { it.committeeNumber }
        val received = monthItems.sumOf { it.paidAmount }
        val pending = monthItems.sumOf { it.remainingAmount }
        val paidCount = monthItems.count { it.statusEnum == InstallmentStatus.PAID }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Paid: $paidCount / ${monthItems.size}",
                "Total Received: ${FormatUtils.formatMoney(received, cur)} | Total Pending: ${FormatUtils.formatMoney(pending, cur)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 32f, "Member" to 135f, "Due" to 75f, "Paid" to 75f, "Method" to 78f, "Date" to 68f, "Status" to 60f))
        monthItems.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 32f,
                    rec.memberName to 135f,
                    FormatUtils.formatCompactNumber(rec.dueAmount) to 75f,
                    FormatUtils.formatCompactNumber(rec.paidAmount) to 75f,
                    PaymentMethod.fromRaw(rec.paymentMethod).label to 78f,
                    (rec.paymentDate ?: "-") to 68f,
                    rec.status to 60f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writePendingPaymentsReport(
        writer: PdfPageWriter,
        monthKey: String,
        installments: List<InstallmentRecord>,
        cur: String
    ) {
        val unpaidOrPartial = installments
            .filter { it.monthKey == monthKey && it.statusEnum != InstallmentStatus.PAID }
            .sortedBy { it.committeeNumber }
        val totalPendingAmt = unpaidOrPartial.sumOf { it.remainingAmount }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Unpaid / Partial Entries: ${unpaidOrPartial.size}",
                "Total Pending Collection: ${FormatUtils.formatMoney(totalPendingAmt, cur)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 36f, "Member Name" to 170f, "Mobile" to 118f, "Remaining" to 108f, "Status" to 91f))
        unpaidOrPartial.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 36f,
                    rec.memberName to 170f,
                    rec.memberPhone to 118f,
                    FormatUtils.formatMoney(rec.remainingAmount, cur) to 108f,
                    rec.status to 91f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writeCompletedPaymentsReport(
        writer: PdfPageWriter,
        monthKey: String,
        installments: List<InstallmentRecord>,
        cur: String
    ) {
        val paid = installments
            .filter { it.monthKey == monthKey && it.statusEnum == InstallmentStatus.PAID }
            .sortedBy { it.committeeNumber }
        val totalPaidAmt = paid.sumOf { it.paidAmount }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Completed Payments: ${paid.size}",
                "Total Completed Collection: ${FormatUtils.formatMoney(totalPaidAmt, cur)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 34f, "Member Name" to 145f, "Amount Paid" to 92f, "Method" to 85f, "Tx ID" to 92f, "Date" to 75f))
        paid.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 34f,
                    rec.memberName to 145f,
                    FormatUtils.formatMoney(rec.paidAmount, cur) to 92f,
                    PaymentMethod.fromRaw(rec.paymentMethod).label to 85f,
                    (rec.transactionId?.takeIf { it.isNotBlank() } ?: "-") to 92f,
                    (rec.paymentDate ?: "-") to 75f
                ),
                statusColor = Color.rgb(22, 163, 74)
            )
        }
    }

    private fun writeIndividualCommitteeReport(
        writer: PdfPageWriter,
        committee: Committee?,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        summary: DashboardSummary?,
        cur: String
    ) {
        if (committee == null) {
            writer.drawInfoBox(listOf("No committee selected."))
            return
        }
        val totalCollected = installments.sumOf { it.paidAmount }
        val activeCount = members.count { it.memberStatusEnum == MemberStatus.ACTIVE }
        writer.drawInfoBox(
            listOf(
                "Committee: ${committee.name} (${committee.status}) • ${committee.startMonth} → ${committee.endMonth}",
                "Members: $activeCount/${committee.memberCount} | Shares: ${committee.effectiveTotalShares} @ ${FormatUtils.formatMoney(committee.monthlyInstallment, cur)}",
                "Monthly Pool: ${FormatUtils.formatMoney(committee.monthlyCollectionTotal, cur)} | Total Collected: ${FormatUtils.formatMoney(totalCollected, cur)}"
            )
        )
        writer.drawSectionHeading("COLLECTION BY MONTH")
        writer.drawTableHeader(listOf("Month" to 130f, "Paid Count" to 90f, "Collected" to 115f, "Target" to 115f, "Pending" to 73f))
        val grouped = installments.groupBy { it.monthKey }
        MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt()).forEachIndexed { idx, key ->
            val rows = grouped[key].orEmpty()
            val collected = rows.sumOf { it.paidAmount }
            val target = if (rows.isNotEmpty()) rows.sumOf { it.dueAmount } else committee.monthlyCollectionTotal
            val paidCount = rows.count { it.statusEnum == InstallmentStatus.PAID }
            writer.drawTableRow(
                listOf(
                    "M${idx + 1} ${MonthUtils.formatMonthDisplay(key)}" to 130f,
                    "$paidCount / ${rows.size}" to 90f,
                    FormatUtils.formatMoney(collected, cur) to 115f,
                    FormatUtils.formatMoney(target, cur) to 115f,
                    FormatUtils.formatCompactNumber((target - collected).coerceAtLeast(0L)) to 73f
                ),
                statusColor = if (collected >= target && target > 0) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
            )
        }
        writer.drawSectionHeading("COMMITTEE PAYOUT TURNS")
        writeCommitteePayoutReport(writer, committee, members, turns, cur)
    }

    private fun writeCommitteePayoutReport(
        writer: PdfPageWriter,
        committee: Committee?,
        members: List<CommitteeMember>,
        turns: List<CommitteeTurn>,
        cur: String
    ) {
        if (committee == null) {
            writer.drawInfoBox(listOf("No committee selected."))
            return
        }
        if (turns.isNotEmpty()) {
            val sortedTurns = turns.sortedBy { it.turnNumber }
            val completed = sortedTurns.count { it.statusEnum == PayoutStatus.PAID }
            val paidSum = sortedTurns.filter { it.statusEnum == PayoutStatus.PAID }.sumOf { it.payoutAmount }
            writer.drawInfoBox(
                listOf(
                    "Payout Turns Completed: $completed / ${sortedTurns.size} | Collection Window: ${committee.collectionDayStart}th – ${committee.collectionDayEnd}th",
                    "Total Payouts Distributed: ${FormatUtils.formatMoney(paidSum, cur)}"
                )
            )
            writer.drawTableHeader(listOf("Turn" to 46f, "Recipient" to 165f, "Schedule" to 105f, "Amount" to 92f, "Date" to 60f, "Status" to 55f))
            sortedTurns.forEach { t ->
                writer.drawTableRow(
                    listOf(
                        "#${t.turnNumber}" to 46f,
                        t.recipientName to 165f,
                        MonthUtils.formatMonthDisplay(t.scheduledMonth) to 105f,
                        FormatUtils.formatCompactNumber(t.payoutAmount) to 92f,
                        (t.payoutDate?.takeIf { it.isNotBlank() } ?: "Unknown") to 60f,
                        t.status to 55f
                    ),
                    statusColor = if (t.statusEnum == PayoutStatus.PAID) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
                )
            }
        } else {
            val sorted = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
            val completed = sorted.count { it.payoutStatusEnum == PayoutStatus.PAID }
            writer.drawInfoBox(
                listOf(
                    "Completed Payouts: $completed / ${sorted.size} | Payout Day: ${committee.payoutDay}th",
                    "Pool Amount: ${FormatUtils.formatMoney(committee.eachPayoutAmount, cur)}"
                )
            )
            writer.drawTableHeader(listOf("#" to 36f, "Member Name" to 165f, "Payout Month" to 100f, "Amount" to 100f, "Date" to 65f, "Status" to 57f))
            sorted.forEach { m ->
                val amt = if (m.payoutAmount > 0) m.payoutAmount else committee.eachPayoutAmount
                writer.drawTableRow(
                    listOf(
                        "#${m.committeeNumber}" to 36f,
                        m.name to 165f,
                        MonthUtils.formatMonthDisplay(m.payoutMonth) to 100f,
                        FormatUtils.formatCompactNumber(amt) to 100f,
                        (m.payoutDate ?: "-") to 65f,
                        m.payoutStatus to 57f
                    ),
                    statusColor = if (m.payoutStatusEnum == PayoutStatus.PAID) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
                )
            }
        }
    }

    private fun writePreviousCommitteeHistory(
        writer: PdfPageWriter,
        oldRecords: List<OldCommitteeRecord>
    ) {
        writer.drawInfoBox(
            listOf(
                "Total Historical / Previous Committee Records: ${oldRecords.size}",
                "Note: Blank or 'Unknown' indicates historical details not recorded earlier."
            )
        )
        if (oldRecords.isEmpty()) {
            writer.drawInfoBox(listOf("No previous committee history records added yet."))
            return
        }
        writer.drawTableHeader(listOf("No." to 55f, "Committee" to 135f, "Recipient" to 115f, "Payout" to 85f, "Date" to 68f, "Status" to 65f))
        oldRecords.forEach { r ->
            writer.drawTableRow(
                listOf(
                    r.committeeNumber.ifBlank { "-" } to 55f,
                    r.committeeName.ifBlank { "-" } to 135f,
                    r.payoutRecipient.ifBlank { "Unknown" } to 115f,
                    r.payoutAmount.ifBlank { "-" } to 85f,
                    r.payoutDate.ifBlank { "Unknown" } to 68f,
                    r.status.ifBlank { "COMPLETED" } to 65f
                ),
                statusColor = Color.rgb(11, 110, 79)
            )
        }
    }

    private fun writeCompleteFinancialSummary(
        writer: PdfPageWriter,
        activeCommittee: Committee?,
        allCommittees: List<Committee>,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        oldRecords: List<OldCommitteeRecord>,
        summary: DashboardSummary?,
        cur: String
    ) {
        val activeCount = allCommittees.count { it.statusEnum == CommitteeStatus.ACTIVE }
        val completedCount = allCommittees.count { it.statusEnum == CommitteeStatus.COMPLETED }
        val totalReceived = summary?.totalCommitteeCollectedAllMonths ?: installments.sumOf { it.paidAmount }
        val totalPendingCurrent = summary?.currentMonthPending ?: 0L
        val totalPayoutsDistributed = turns.filter { it.statusEnum == PayoutStatus.PAID }.sumOf { it.payoutAmount }

        writer.drawInfoBox(
            listOf(
                "Active Committees: $activeCount | Completed Committees: $completedCount | Old History Entries: ${oldRecords.size}",
                "Active Committee: ${activeCommittee?.name ?: "-"} | Total Collected: ${FormatUtils.formatMoney(totalReceived, cur)}",
                "Current Month Pending: ${FormatUtils.formatMoney(totalPendingCurrent, cur)} | Total Payouts Paid: ${FormatUtils.formatMoney(totalPayoutsDistributed, cur)}"
            )
        )
        if (allCommittees.isNotEmpty()) {
            writer.drawSectionHeading("ALL COMMITTEES OVERVIEW")
            writer.drawTableHeader(listOf("Committee Name" to 180f, "Shares" to 60f, "Monthly Pool" to 115f, "Period" to 105f, "Status" to 63f))
            allCommittees.forEach { c ->
                writer.drawTableRow(
                    listOf(
                        c.name to 180f,
                        c.effectiveTotalShares.toString() to 60f,
                        FormatUtils.formatMoney(c.monthlyCollectionTotal, cur) to 115f,
                        "${c.startMonth}..${c.endMonth}" to 105f,
                        c.status to 63f
                    ),
                    statusColor = if (c.statusEnum == CommitteeStatus.ACTIVE) Color.rgb(22, 163, 74) else Color.rgb(11, 110, 79)
                )
            }
        }
        if (activeCommittee != null) {
            writeIndividualCommitteeReport(writer, activeCommittee, members, installments, turns, summary, cur)
        }
    }

    private fun colorForInstallment(status: InstallmentStatus): Int = when (status) {
        InstallmentStatus.PAID -> Color.rgb(22, 163, 74)
        InstallmentStatus.UNPAID -> Color.rgb(220, 38, 38)
        InstallmentStatus.PARTIAL -> Color.rgb(217, 119, 6)
    }

    private class PdfPageWriter(
        private val document: PdfDocument,
        private val committeeName: String,
        private val companySettings: AppCompanySettings
    ) {
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN
        private var pageNumber = 0
        private var currentSubtitle = ""

        private val brandPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val subtitlePaint = Paint().apply {
            color = Color.rgb(235, 248, 242)
            textSize = 10f
            isAntiAlias = true
        }
        private val headerBgPaint = Paint().apply { color = Color.rgb(11, 110, 79) }
        private val goldStripPaint = Paint().apply { color = Color.rgb(197, 155, 39) }
        private val infoBgPaint = Paint().apply { color = Color.rgb(240, 247, 244) }
        private val tableHeaderBgPaint = Paint().apply { color = Color.rgb(226, 238, 232) }
        private val rowDividerPaint = Paint().apply {
            color = Color.rgb(215, 226, 220)
            strokeWidth = 0.8f
        }
        private val textBoldPaint = Paint().apply {
            color = Color.rgb(16, 32, 26)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val textPaint = Paint().apply {
            color = Color.rgb(32, 50, 42)
            textSize = 9f
            isAntiAlias = true
        }
        private val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }

        fun startNewPage(subtitle: String) {
            finishCurrentPage()
            currentSubtitle = subtitle
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            currentPage = page
            val c = page.canvas
            canvas = c

            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 66f, headerBgPaint)
            c.drawRect(0f, 66f, PAGE_WIDTH.toFloat(), 70f, goldStripPaint)
            c.drawText(companySettings.companyName.uppercase(Locale.US), MARGIN, 28f, brandPaint)
            c.drawText("$committeeName  •  $subtitle", MARGIN, 48f, subtitlePaint)
            y = 88f
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - 42f) {
                startNewPage(currentSubtitle)
            }
        }

        fun drawInfoBox(lines: List<String>) {
            val boxHeight = 16f + lines.size * 15f
            ensureSpace(boxHeight + 10f)
            val c = canvas ?: return
            c.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, infoBgPaint)
            var ty = y + 17f
            lines.forEach { line ->
                c.drawText(line, MARGIN + 10f, ty, textBoldPaint)
                ty += 15f
            }
            y += boxHeight + 12f
        }

        fun drawSectionHeading(title: String) {
            ensureSpace(26f)
            y += 6f
            canvas?.drawText(title, MARGIN, y + 12f, textBoldPaint)
            y += 20f
        }

        fun drawTableHeader(columns: List<Pair<String, Float>>) {
            ensureSpace(24f)
            val c = canvas ?: return
            c.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, tableHeaderBgPaint)
            var x = MARGIN + 6f
            columns.forEach { (label, width) ->
                c.drawText(label, x, y + 13.5f, textBoldPaint)
                x += width
            }
            y += 22f
        }

        fun drawTableRow(columns: List<Pair<String, Float>>, statusColor: Int? = null) {
            ensureSpace(20f)
            val c = canvas ?: return
            var x = MARGIN + 6f
            columns.forEachIndexed { index, (value, width) ->
                val isLast = index == columns.lastIndex
                val paint = if (isLast && statusColor != null) {
                    Paint(textBoldPaint).apply { color = statusColor }
                } else {
                    textPaint
                }
                val clipped = if (value.length > 26) value.take(24) + ".." else value
                c.drawText(clipped, x, y + 13f, paint)
                x += width
            }
            c.drawLine(MARGIN, y + 18f, PAGE_WIDTH - MARGIN, y + 18f, rowDividerPaint)
            y += 19f
        }

        private fun finishCurrentPage() {
            val page = currentPage ?: return
            val c = canvas
            val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date())
            val note = companySettings.pdfFooterNote.ifBlank { companySettings.companyName }
            c?.drawText("$note • $timestamp", MARGIN, PAGE_HEIGHT - 16f, footerPaint)
            c?.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 42f, PAGE_HEIGHT - 16f, footerPaint)
            document.finishPage(page)
            currentPage = null
            canvas = null
        }

        fun finish() {
            finishCurrentPage()
        }
    }
}
