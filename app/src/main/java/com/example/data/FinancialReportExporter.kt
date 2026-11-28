package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.FormatUtils
import com.example.model.ForwardFinancialCalculator
import com.example.model.ForwardOrganizer
import com.example.model.ForwardPaymentRecord
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.PayoutStatus
import java.io.File

/**
 * Generates clean Excel (.xls SpreadsheetML XML) files for all financial reports.
 * Opens directly in Microsoft Excel, Google Sheets, and WPS Office on Android without extra dependencies.
 */
object FinancialReportExporter {

    fun createExcelFileInCache(
        context: Context,
        reportType: PdfReportType,
        committee: Committee?,
        allCommittees: List<Committee>,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        oldRecords: List<OldCommitteeRecord>,
        selectedMonthKey: String,
        selectedMember: CommitteeMember?,
        companySettings: AppCompanySettings,
        forwardOrganizers: List<ForwardOrganizer> = emptyList(),
        forwardPayments: List<ForwardPaymentRecord> = emptyList()
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = reportType.name.lowercase()
        val file = File(reportsDir, "GMSR_${safeName}_${System.currentTimeMillis()}.xls")
        val xml = buildExcelXml(
            reportType = reportType,
            committee = committee,
            allCommittees = allCommittees,
            members = members,
            installments = installments,
            turns = turns,
            oldRecords = oldRecords,
            selectedMonthKey = selectedMonthKey,
            selectedMember = selectedMember,
            companySettings = companySettings,
            forwardOrganizers = forwardOrganizers,
            forwardPayments = forwardPayments
        )
        file.writeText(xml, Charsets.UTF_8)
        return file
    }

    fun writeExcelToUri(
        context: Context,
        uri: Uri,
        reportType: PdfReportType,
        committee: Committee?,
        allCommittees: List<Committee>,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        oldRecords: List<OldCommitteeRecord>,
        selectedMonthKey: String,
        selectedMember: CommitteeMember?,
        companySettings: AppCompanySettings,
        forwardOrganizers: List<ForwardOrganizer> = emptyList(),
        forwardPayments: List<ForwardPaymentRecord> = emptyList()
    ): Boolean {
        val xml = buildExcelXml(
            reportType = reportType,
            committee = committee,
            allCommittees = allCommittees,
            members = members,
            installments = installments,
            turns = turns,
            oldRecords = oldRecords,
            selectedMonthKey = selectedMonthKey,
            selectedMember = selectedMember,
            companySettings = companySettings,
            forwardOrganizers = forwardOrganizers,
            forwardPayments = forwardPayments
        )
        return context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(xml.toByteArray(Charsets.UTF_8))
            true
        } ?: false
    }

    fun buildExcelXml(
        reportType: PdfReportType,
        committee: Committee?,
        allCommittees: List<Committee>,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        oldRecords: List<OldCommitteeRecord>,
        selectedMonthKey: String,
        selectedMember: CommitteeMember?,
        companySettings: AppCompanySettings,
        forwardOrganizers: List<ForwardOrganizer> = emptyList(),
        forwardPayments: List<ForwardPaymentRecord> = emptyList()
    ): String {
        val tableData = buildReportRows(
            reportType = reportType,
            committee = committee,
            allCommittees = allCommittees,
            members = members,
            installments = installments,
            turns = turns,
            oldRecords = oldRecords,
            selectedMonthKey = selectedMonthKey,
            selectedMember = selectedMember,
            companySettings = companySettings,
            forwardOrganizers = forwardOrganizers,
            forwardPayments = forwardPayments
        )

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<?mso-application progid=\"Excel.Sheet\"?>")
        sb.appendLine("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"")
        sb.appendLine(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">")
        sb.appendLine(" <Styles>")
        sb.appendLine("  <Style ss:ID=\"Title\"><Font ss:Bold=\"1\" ss:Size=\"14\" ss:Color=\"#0E4B44\"/></Style>")
        sb.appendLine("  <Style ss:ID=\"Header\"><Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#0E4B44\" ss:Pattern=\"Solid\"/></Style>")
        sb.appendLine(" </Styles>")
        sb.appendLine(" <Worksheet ss:Name=\"${xmlEsc(tableData.sheetName.take(30))}\">")
        sb.appendLine("  <Table>")
        sb.appendLine("   <Row><Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">${xmlEsc(companySettings.companyName)} — ${xmlEsc(tableData.title)}</Data></Cell></Row>")
        sb.appendLine("   <Row><Cell><Data ss:Type=\"String\">Contact: ${xmlEsc(companySettings.adminContactNumber.ifBlank { "03423498631" })} | Generated: ${xmlEsc(MonthUtils.todayDateString())} | Currency: ${xmlEsc(companySettings.currency)}</Data></Cell></Row>")
        sb.appendLine("   <Row></Row>")

        // Header Row
        sb.appendLine("   <Row>")
        tableData.headers.forEach { h ->
            sb.appendLine("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">${xmlEsc(h)}</Data></Cell>")
        }
        sb.appendLine("   </Row>")

        // Data Rows
        tableData.rows.forEach { row ->
            sb.appendLine("   <Row>")
            row.forEach { cell ->
                sb.appendLine("    <Cell><Data ss:Type=\"String\">${xmlEsc(cell)}</Data></Cell>")
            }
            sb.appendLine("   </Row>")
        }

        sb.appendLine("  </Table>")
        sb.appendLine(" </Worksheet>")
        sb.appendLine("</Workbook>")
        return sb.toString()
    }

    private data class ExcelTable(
        val sheetName: String,
        val title: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    private fun buildReportRows(
        reportType: PdfReportType,
        committee: Committee?,
        allCommittees: List<Committee>,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        oldRecords: List<OldCommitteeRecord>,
        selectedMonthKey: String,
        selectedMember: CommitteeMember?,
        companySettings: AppCompanySettings,
        forwardOrganizers: List<ForwardOrganizer> = emptyList(),
        forwardPayments: List<ForwardPaymentRecord> = emptyList()
    ): ExcelTable {
        val currency = companySettings.currency
        val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
        val startDay = committee?.collectionDayStart ?: 28L
        val endDay = committee?.collectionDayEnd ?: 8L

        return when (reportType) {
            PdfReportType.DAILY_COLLECTION -> {
                val today = MonthUtils.todayDateString()
                val todayRows = installments.filter {
                    it.paymentDate == today && it.paidAmount > 0L
                }.sortedBy { it.committeeNumber }
                ExcelTable(
                    sheetName = "Daily Collection",
                    title = "Daily Collection ($today)",
                    headers = listOf("Receipt #", "Member", "Month", "Amount Paid", "Method", "Reference", "Date"),
                    rows = todayRows.map {
                        listOf(
                            it.receiptNumber ?: "-",
                            it.memberName,
                            MonthUtils.formatMonthDisplay(it.monthKey),
                            FormatUtils.formatMoney(it.paidAmount, currency),
                            it.paymentMethodEnum.label,
                            it.transactionId ?: "-",
                            it.paymentDate ?: today
                        )
                    }
                )
            }

            PdfReportType.INDIVIDUAL_MEMBER -> {
                val target = selectedMember ?: activeMembers.firstOrNull()
                val mInst = if (target != null) installments.filter { it.memberId == target.id }.sortedBy { it.monthIndex } else emptyList()
                ExcelTable(
                    sheetName = "Member Statement",
                    title = "Member Statement - ${target?.name ?: "Member"} (${target?.displayMemberId ?: "-"})",
                    headers = listOf("Month #", "Month", "Receipt #", "Due Amount", "Paid Amount", "Remaining", "Status", "Method", "Date"),
                    rows = mInst.map {
                        listOf(
                            "M${it.monthIndex}",
                            MonthUtils.formatMonthDisplay(it.monthKey),
                            it.receiptNumber ?: "-",
                            FormatUtils.formatMoney(it.dueAmount, currency),
                            FormatUtils.formatMoney(it.paidAmount, currency),
                            FormatUtils.formatMoney(it.remainingAmount, currency),
                            it.effectiveStatus(startDay, endDay).name,
                            it.paymentMethodEnum.label,
                            it.paymentDate ?: "-"
                        )
                    }
                )
            }

            PdfReportType.COMPLETE_MEMBER_LIST -> {
                ExcelTable(
                    sheetName = "Member List",
                    title = "Complete Member List (${committee?.name ?: "All"})",
                    headers = listOf("Member ID", "No.", "Full Name", "Father Name", "Mobile", "Address", "Shares", "Monthly Due", "Joining Date", "Status"),
                    rows = members.sortedBy { it.committeeNumber }.map { m ->
                        listOf(
                            m.displayMemberId,
                            "#${m.committeeNumber}",
                            m.name,
                            m.fatherName.ifBlank { "-" },
                            m.phone,
                            m.address.ifBlank { "-" },
                            FormatUtils.formatShares(m.shares),
                            FormatUtils.formatMoney(m.monthlyDue(committee?.monthlyInstallment ?: 5000L), currency),
                            m.joiningDate.ifBlank { "-" },
                            m.status
                        )
                    }
                )
            }

            PdfReportType.MONTHLY_COLLECTION -> {
                val monthInst = installments.filter { it.monthKey == selectedMonthKey }.sortedBy { it.committeeNumber }
                ExcelTable(
                    sheetName = "Monthly Collection",
                    title = "Monthly Collection (${MonthUtils.formatMonthDisplay(selectedMonthKey)})",
                    headers = listOf("Receipt #", "No.", "Member", "Mobile", "Due Amount", "Paid Amount", "Remaining", "Status", "Method", "Date"),
                    rows = monthInst.map {
                        listOf(
                            it.receiptNumber ?: "-",
                            "#${it.committeeNumber}",
                            it.memberName,
                            it.memberPhone,
                            FormatUtils.formatMoney(it.dueAmount, currency),
                            FormatUtils.formatMoney(it.paidAmount, currency),
                            FormatUtils.formatMoney(it.remainingAmount, currency),
                            it.effectiveStatus(startDay, endDay).name,
                            it.paymentMethodEnum.label,
                            it.paymentDate ?: "-"
                        )
                    }
                )
            }

            PdfReportType.PENDING_PAYMENTS -> {
                val pending = installments.filter {
                    it.monthKey == selectedMonthKey && it.statusEnum != InstallmentStatus.PAID
                }.sortedBy { it.committeeNumber }
                ExcelTable(
                    sheetName = "Pending Payments",
                    title = "Pending & Overdue Payments (${MonthUtils.formatMonthDisplay(selectedMonthKey)})",
                    headers = listOf("No.", "Member", "Mobile", "Due", "Paid", "Remaining", "Status"),
                    rows = pending.map {
                        listOf(
                            "#${it.committeeNumber}",
                            it.memberName,
                            it.memberPhone,
                            FormatUtils.formatMoney(it.dueAmount, currency),
                            FormatUtils.formatMoney(it.paidAmount, currency),
                            FormatUtils.formatMoney(it.remainingAmount, currency),
                            it.effectiveStatus(startDay, endDay).name
                        )
                    }
                )
            }

            PdfReportType.COMPLETED_PAYMENTS -> {
                val completed = installments.filter {
                    it.paidAmount > 0L
                }.sortedWith(compareBy({ it.monthIndex }, { it.committeeNumber }))
                ExcelTable(
                    sheetName = "Completed Payments",
                    title = "Completed Payments Report",
                    headers = listOf("Receipt #", "Month", "Member", "Paid Amount", "Method", "Tx Ref", "Payment Date", "Status"),
                    rows = completed.map {
                        listOf(
                            it.receiptNumber ?: "-",
                            MonthUtils.formatMonthDisplay(it.monthKey),
                            it.memberName,
                            FormatUtils.formatMoney(it.paidAmount, currency),
                            it.paymentMethodEnum.label,
                            it.transactionId ?: "-",
                            it.paymentDate ?: "-",
                            it.status
                        )
                    }
                )
            }

            PdfReportType.INDIVIDUAL_COMMITTEE -> {
                val monthGroups = installments.groupBy { it.monthIndex to it.monthKey }.toSortedMap(compareBy { it.first })
                ExcelTable(
                    sheetName = "Committee Summary",
                    title = "Individual Committee Report - ${committee?.name ?: "Committee"}",
                    headers = listOf("Month #", "Month", "Members", "Collected", "Pending", "Paid Count"),
                    rows = monthGroups.map { (key, list) ->
                        val collected = list.sumOf { it.paidAmount }
                        val due = list.sumOf { it.dueAmount }
                        val paidCount = list.count { it.statusEnum == InstallmentStatus.PAID }
                        listOf(
                            "M${key.first}",
                            MonthUtils.formatMonthDisplay(key.second),
                            list.size.toString(),
                            FormatUtils.formatMoney(collected, currency),
                            FormatUtils.formatMoney((due - collected).coerceAtLeast(0L), currency),
                            "$paidCount / ${list.size}"
                        )
                    }
                )
            }

            PdfReportType.COMMITTEE_PAYOUT_REPORT -> {
                ExcelTable(
                    sheetName = "Committee Winners",
                    title = "Committee Winners & Payout Report (${committee?.name ?: "Committee"})",
                    headers = listOf("Turn #", "Winner Name", "Mobile", "Scheduled Month", "Payout Amount", "Payout Date", "Status", "Confirmed"),
                    rows = turns.sortedBy { it.turnNumber }.map { t ->
                        listOf(
                            "#${t.turnNumber}",
                            t.recipientName,
                            t.recipientPhone ?: "-",
                            MonthUtils.formatMonthDisplay(t.scheduledMonth),
                            FormatUtils.formatMoney(t.payoutAmount, currency),
                            t.payoutDate?.takeIf { it.isNotBlank() } ?: "Unknown",
                            if (t.statusEnum == PayoutStatus.PAID) "Completed" else "Upcoming",
                            if (t.confirmedByAdmin) "Yes" else "Pending"
                        )
                    }
                )
            }

            PdfReportType.PREVIOUS_COMMITTEE_HISTORY -> {
                ExcelTable(
                    sheetName = "Previous History",
                    title = "Previous Committee History",
                    headers = listOf("Committee #", "Name", "Start", "End", "Shares", "Monthly", "Total Collection", "Winner", "Payout", "Date", "Status"),
                    rows = oldRecords.map { r ->
                        listOf(
                            r.committeeNumber, r.committeeName, r.startDate, r.endDate,
                            r.numberOfShares, r.monthlyContributions, r.totalCollection,
                            r.payoutRecipient, r.payoutAmount, r.payoutDate, r.status
                        )
                    }
                )
            }

            PdfReportType.COMPLETE_FINANCIAL_SUMMARY -> {
                ExcelTable(
                    sheetName = "Financial Summary",
                    title = "Complete Financial Summary (${allCommittees.size} Committees)",
                    headers = listOf("Committee", "Members", "Shares", "Monthly Pool", "Total Value", "Duration", "Status"),
                    rows = allCommittees.map { c ->
                        listOf(
                            c.name,
                            c.memberCount.toString(),
                            c.effectiveTotalShares.toString(),
                            FormatUtils.formatMoney(c.monthlyCollectionTotal, currency),
                            FormatUtils.formatMoney(c.totalCommitteeAmount, currency),
                            "${c.totalMonths} months",
                            c.status
                        )
                    }
                )
            }

            PdfReportType.FORWARD_PAYMENTS_SUMMARY -> {
                val summary = ForwardFinancialCalculator.compute(
                    committee = committee,
                    members = members,
                    installments = installments,
                    organizers = forwardOrganizers,
                    forwardPayments = forwardPayments,
                    monthKey = null
                )
                val summaryRows = listOf(
                    listOf("SUMMARY", "Expected from Members", FormatUtils.formatMoney(summary.expectedFromMembers, currency), "-", "-", "-", "-", "Overall"),
                    listOf("SUMMARY", "Collected from Members", FormatUtils.formatMoney(summary.collectedFromMembers, currency), "-", "-", "-", "-", "Overall"),
                    listOf("SUMMARY", "Pending Collection", FormatUtils.formatMoney(summary.pendingMemberCollection, currency), "-", "-", "-", "-", "Overall"),
                    listOf("SUMMARY", "Forward Payable", FormatUtils.formatMoney(summary.forwardPaymentDue, currency), "-", "-", "-", "-", "Overall"),
                    listOf("SUMMARY", "Total Forwarded", FormatUtils.formatMoney(summary.totalForwarded, currency), "-", "-", "-", "-", "Confirmed"),
                    listOf("SUMMARY", "Pending Forward", FormatUtils.formatMoney(summary.pendingForwardPayment, currency), "-", "-", "-", "-", "Overall"),
                    listOf("SUMMARY", "Held by Admin", FormatUtils.formatMoney(summary.amountCurrentlyHeldByAdmin, currency), "-", "-", "-", "-", "Cash-in-Hand")
                )
                val txRows = forwardPayments.map { p ->
                    listOf(
                        p.receiptNumber ?: "-",
                        p.organizerName,
                        FormatUtils.formatMoney(p.amountForwarded, currency),
                        p.organizerPhone,
                        p.paymentDate,
                        p.paymentMethodEnum.label,
                        p.proofLabel ?: (p.proofUri?.let { "Attached" } ?: "None"),
                        when {
                            p.isReversed -> "REVERSED"
                            p.confirmedByAdmin -> "CONFIRMED"
                            else -> "UNCONFIRMED"
                        }
                    )
                }
                ExcelTable(
                    sheetName = "Forward Payments",
                    title = "Collection & Forward Payments Summary (${committee?.name ?: "Committee"})",
                    headers = listOf("Receipt / Row", "Organizer / Metric", "Amount", "Mobile", "Date", "Method", "Proof", "Status"),
                    rows = summaryRows + txRows
                )
            }
        }
    }

    private fun xmlEsc(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
