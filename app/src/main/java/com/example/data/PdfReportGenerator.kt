package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.DashboardSummary
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PayoutStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfReportType {
    MEMBER_LIST,
    MONTHLY_PAYMENT,
    PAID_MEMBERS_REPORT,
    UNPAID_MEMBERS_REPORT,
    PAID_UNPAID_LIST,
    INDIVIDUAL_HISTORY,
    COLLECTION_REPORT,
    PAYOUT_REPORT,
    COMPLETE_SUMMARY
}

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595 // A4 width in PostScript points
    private const val PAGE_HEIGHT = 842 // A4 height
    private const val MARGIN = 36f

    fun generateReport(
        context: Context,
        reportType: PdfReportType,
        committee: Committee,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        selectedMonthKey: String,
        selectedMember: CommitteeMember?,
        summary: DashboardSummary
    ): Result<File> {
        val doc = PdfDocument()
        return try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "GM_HASMANI_${reportType.name}_$stamp.pdf"
            val outFile = File(dir, fileName)

            val writer = PdfPageWriter(doc, committee.name)
            writer.startNewPage(reportSubtitle(reportType, selectedMonthKey, selectedMember))

            when (reportType) {
                PdfReportType.MEMBER_LIST -> writeMemberList(writer, committee, members)
                PdfReportType.MONTHLY_PAYMENT -> writeMonthlyPaymentReport(writer, selectedMonthKey, installments)
                PdfReportType.PAID_MEMBERS_REPORT -> writePaidMembersReport(writer, selectedMonthKey, installments)
                PdfReportType.UNPAID_MEMBERS_REPORT -> writeUnpaidMembersReport(writer, selectedMonthKey, installments)
                PdfReportType.PAID_UNPAID_LIST -> writePaidUnpaidReport(writer, selectedMonthKey, installments)
                PdfReportType.INDIVIDUAL_HISTORY -> writeIndividualHistory(writer, selectedMember, installments)
                PdfReportType.COLLECTION_REPORT -> writeCollectionReport(writer, committee, installments, summary)
                PdfReportType.PAYOUT_REPORT -> writePayoutReport(writer, committee, members)
                PdfReportType.COMPLETE_SUMMARY -> writeCompleteSummary(writer, committee, members, installments, summary)
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
            val chooser = if (shareMode) Intent.createChooser(intent, "Share GM HASMANI PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else intent
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun reportSubtitle(type: PdfReportType, monthKey: String, member: CommitteeMember?): String = when (type) {
        PdfReportType.MEMBER_LIST -> "Complete Member List"
        PdfReportType.MONTHLY_PAYMENT -> "Monthly Collection Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.PAID_MEMBERS_REPORT -> "Paid Members Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.UNPAID_MEMBERS_REPORT -> "Unpaid Members Report • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.PAID_UNPAID_LIST -> "Paid & Unpaid List • ${MonthUtils.formatMonthDisplay(monthKey)}"
        PdfReportType.INDIVIDUAL_HISTORY -> "Member History • ${member?.name ?: "Member"}"
        PdfReportType.COLLECTION_REPORT -> "Committee Collection Report"
        PdfReportType.PAYOUT_REPORT -> "Payout History Report"
        PdfReportType.COMPLETE_SUMMARY -> "Complete Financial Summary"
    }

    private fun writeMemberList(writer: PdfPageWriter, committee: Committee, members: List<CommitteeMember>) {
        val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
        val archivedMembers = members.filter { it.memberStatusEnum == MemberStatus.ARCHIVED }.sortedBy { it.committeeNumber }
        writer.drawInfoBox(
            listOf(
                "Target Members: ${committee.memberCount} | Enrolled Active: ${activeMembers.size}",
                "Monthly Installment: ${FormatUtils.formatPkr(committee.monthlyInstallment)} | Payout: ${FormatUtils.formatPkr(committee.eachPayoutAmount)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 38f, "Member Name" to 190f, "Mobile Number" to 130f, "Payout Month" to 95f, "Status" to 70f))
        activeMembers.forEach { m ->
            writer.drawTableRow(
                listOf(
                    "#${m.committeeNumber}" to 38f,
                    m.name to 190f,
                    m.phone to 130f,
                    MonthUtils.formatMonthDisplay(m.payoutMonth) to 95f,
                    m.status to 70f
                ),
                statusColor = Color.rgb(11, 110, 79)
            )
        }
        if (archivedMembers.isNotEmpty()) {
            writer.drawSectionHeading("Archived Members (${archivedMembers.size})")
            archivedMembers.forEach { m ->
                writer.drawTableRow(
                    listOf(
                        "#${m.committeeNumber}" to 38f,
                        m.name to 190f,
                        m.phone to 130f,
                        MonthUtils.formatMonthDisplay(m.payoutMonth) to 95f,
                        m.status to 70f
                    ),
                    statusColor = Color.GRAY
                )
            }
        }
    }

    private fun writeMonthlyPaymentReport(writer: PdfPageWriter, monthKey: String, installments: List<InstallmentRecord>) {
        val monthItems = installments.filter { it.monthKey == monthKey }.sortedBy { it.committeeNumber }
        val received = monthItems.sumOf { it.paidAmount }
        val pending = monthItems.sumOf { it.remainingAmount }
        val paidCount = monthItems.count { it.statusEnum == InstallmentStatus.PAID }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Paid: $paidCount / ${monthItems.size}",
                "Received: ${FormatUtils.formatPkr(received)} | Pending: ${FormatUtils.formatPkr(pending)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 34f, "Member" to 155f, "Due" to 85f, "Paid" to 85f, "Date" to 84f, "Status" to 80f))
        monthItems.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 34f,
                    rec.memberName to 155f,
                    FormatUtils.formatCompactNumber(rec.dueAmount) to 85f,
                    FormatUtils.formatCompactNumber(rec.paidAmount) to 85f,
                    (rec.paymentDate ?: "-") to 84f,
                    rec.status to 80f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writePaidMembersReport(writer: PdfPageWriter, monthKey: String, installments: List<InstallmentRecord>) {
        val paid = installments
            .filter { it.monthKey == monthKey && it.statusEnum == InstallmentStatus.PAID }
            .sortedBy { it.committeeNumber }
        val totalPaidAmt = paid.sumOf { it.paidAmount }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Paid Members: ${paid.size}",
                "Total Collected from Paid Members: ${FormatUtils.formatPkr(totalPaidAmt)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 38f, "Member Name" to 190f, "Mobile" to 125f, "Amount Paid" to 95f, "Date" to 75f))
        paid.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 38f,
                    rec.memberName to 190f,
                    rec.memberPhone to 125f,
                    FormatUtils.formatPkr(rec.paidAmount) to 95f,
                    (rec.paymentDate ?: "-") to 75f
                ),
                statusColor = Color.rgb(22, 163, 74)
            )
        }
    }

    private fun writeUnpaidMembersReport(writer: PdfPageWriter, monthKey: String, installments: List<InstallmentRecord>) {
        val unpaidOrPartial = installments
            .filter { it.monthKey == monthKey && it.statusEnum != InstallmentStatus.PAID }
            .sortedBy { it.committeeNumber }
        val totalPendingAmt = unpaidOrPartial.sumOf { it.remainingAmount }
        writer.drawInfoBox(
            listOf(
                "Month: ${MonthUtils.formatMonthDisplay(monthKey)} | Unpaid / Partial Members: ${unpaidOrPartial.size}",
                "Total Pending Collection: ${FormatUtils.formatPkr(totalPendingAmt)}"
            )
        )
        writer.drawTableHeader(listOf("#" to 38f, "Member Name" to 170f, "Mobile" to 120f, "Remaining" to 105f, "Status" to 90f))
        unpaidOrPartial.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 38f,
                    rec.memberName to 170f,
                    rec.memberPhone to 120f,
                    FormatUtils.formatPkr(rec.remainingAmount) to 105f,
                    rec.status to 90f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writePaidUnpaidReport(writer: PdfPageWriter, monthKey: String, installments: List<InstallmentRecord>) {
        val monthItems = installments.filter { it.monthKey == monthKey }.sortedBy { it.committeeNumber }
        val paid = monthItems.filter { it.statusEnum == InstallmentStatus.PAID }
        val unpaidOrPartial = monthItems.filter { it.statusEnum != InstallmentStatus.PAID }

        writer.drawSectionHeading("PAID MEMBERS (${paid.size})")
        writer.drawTableHeader(listOf("#" to 38f, "Member Name" to 190f, "Mobile" to 125f, "Amount Paid" to 95f, "Date" to 75f))
        paid.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 38f,
                    rec.memberName to 190f,
                    rec.memberPhone to 125f,
                    FormatUtils.formatPkr(rec.paidAmount) to 95f,
                    (rec.paymentDate ?: "-") to 75f
                ),
                statusColor = Color.rgb(22, 163, 74)
            )
        }

        writer.drawSectionHeading("UNPAID / PARTIAL MEMBERS (${unpaidOrPartial.size})")
        writer.drawTableHeader(listOf("#" to 38f, "Member Name" to 170f, "Mobile" to 120f, "Remaining" to 105f, "Status" to 90f))
        unpaidOrPartial.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "#${rec.committeeNumber}" to 38f,
                    rec.memberName to 170f,
                    rec.memberPhone to 120f,
                    FormatUtils.formatPkr(rec.remainingAmount) to 105f,
                    rec.status to 90f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writeIndividualHistory(writer: PdfPageWriter, member: CommitteeMember?, installments: List<InstallmentRecord>) {
        if (member == null) {
            writer.drawInfoBox(listOf("No member selected."))
            return
        }
        val memberRows = installments.filter { it.memberId == member.id }.sortedBy { it.monthIndex }
        val totalPaid = memberRows.sumOf { it.paidAmount }
        val totalRemaining = memberRows.sumOf { it.remainingAmount }
        writer.drawInfoBox(
            listOf(
                "Member: ${member.name} (#${member.committeeNumber}) • Mobile: ${member.phone}",
                "Payout Month: ${MonthUtils.formatMonthDisplay(member.payoutMonth)} (${member.payoutStatus}) | Paid: ${FormatUtils.formatPkr(totalPaid)} | Due: ${FormatUtils.formatPkr(totalRemaining)}"
            )
        )
        writer.drawTableHeader(listOf("Month" to 110f, "Due" to 95f, "Paid" to 95f, "Date" to 95f, "Status" to 128f))
        memberRows.forEach { rec ->
            writer.drawTableRow(
                listOf(
                    "M${rec.monthIndex} ${MonthUtils.formatMonthDisplay(rec.monthKey)}" to 110f,
                    FormatUtils.formatPkr(rec.dueAmount) to 95f,
                    FormatUtils.formatPkr(rec.paidAmount) to 95f,
                    (rec.paymentDate ?: "-") to 95f,
                    rec.status to 128f
                ),
                statusColor = colorForInstallment(rec.statusEnum)
            )
        }
    }

    private fun writeCollectionReport(
        writer: PdfPageWriter,
        committee: Committee,
        installments: List<InstallmentRecord>,
        summary: DashboardSummary
    ) {
        writer.drawInfoBox(
            listOf(
                "Total Collected (All Months): ${FormatUtils.formatPkr(summary.totalCommitteeCollectedAllMonths)}",
                "Total Expected: ${FormatUtils.formatPkr(committee.totalCommitteeAmount)} | Monthly Target: ${FormatUtils.formatPkr(committee.monthlyCollectionTotal)}"
            )
        )
        writer.drawTableHeader(listOf("Month" to 130f, "Paid Count" to 90f, "Collected" to 115f, "Target" to 115f, "Balance" to 73f))
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
                    FormatUtils.formatPkr(collected) to 115f,
                    FormatUtils.formatPkr(target) to 115f,
                    FormatUtils.formatCompactNumber((target - collected).coerceAtLeast(0L)) to 73f
                ),
                statusColor = if (collected >= target && target > 0) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
            )
        }
    }

    private fun writePayoutReport(writer: PdfPageWriter, committee: Committee, members: List<CommitteeMember>) {
        val sorted = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
        val completed = sorted.count { it.payoutStatusEnum == PayoutStatus.PAID }
        writer.drawInfoBox(
            listOf(
                "Completed Payouts: $completed / ${sorted.size} | Payout Day: ${committee.payoutDay}th of month",
                "Each Member Payout: ${FormatUtils.formatPkr(committee.eachPayoutAmount)}"
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

    private fun writeCompleteSummary(
        writer: PdfPageWriter,
        committee: Committee,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        summary: DashboardSummary
    ) {
        writer.drawInfoBox(
            listOf(
                "Committee: ${committee.name} (${committee.status}) | ${committee.startMonth} to ${committee.endMonth}",
                "Members: ${summary.activeMemberCount}/${committee.memberCount} | Installment: ${FormatUtils.formatPkr(committee.monthlyInstallment)} | Collected: ${FormatUtils.formatPkr(summary.totalCommitteeCollectedAllMonths)}"
            )
        )
        writeCollectionReport(writer, committee, installments, summary)
        writer.drawSectionHeading("PAYOUT SCHEDULE")
        writePayoutReport(writer, committee, members)
    }

    private fun colorForInstallment(status: InstallmentStatus): Int = when (status) {
        InstallmentStatus.PAID -> Color.rgb(22, 163, 74)
        InstallmentStatus.UNPAID -> Color.rgb(220, 38, 38)
        InstallmentStatus.PARTIAL -> Color.rgb(217, 119, 6)
    }

    private class PdfPageWriter(
        private val document: PdfDocument,
        private val committeeName: String
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
            textSize = 10.5f
            isAntiAlias = true
        }
        private val headerBgPaint = Paint().apply {
            color = Color.rgb(11, 110, 79)
        }
        private val goldStripPaint = Paint().apply {
            color = Color.rgb(197, 155, 39)
        }
        private val infoBgPaint = Paint().apply {
            color = Color.rgb(240, 247, 244)
        }
        private val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(226, 238, 232)
        }
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

            // Brand header banner
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 66f, headerBgPaint)
            c.drawRect(0f, 66f, PAGE_WIDTH.toFloat(), 70f, goldStripPaint)
            c.drawText("GM HASMANI COMMITTEE MANAGER", MARGIN, 28f, brandPaint)
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
                val clipped = if (value.length > 28) value.take(26) + ".." else value
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
            c?.drawText("Generated by GM HASMANI Committee Manager • $timestamp", MARGIN, PAGE_HEIGHT - 16f, footerPaint)
            c?.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 40f, PAGE_HEIGHT - 16f, footerPaint)
            document.finishPage(page)
            currentPage = null
            canvas = null
        }

        fun finish() {
            finishCurrentPage()
        }
    }
}
