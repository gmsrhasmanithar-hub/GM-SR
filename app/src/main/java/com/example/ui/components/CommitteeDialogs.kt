package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.AuditLogEntry
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.CommitteeUtils
import com.example.model.DuplicateConflictType
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MonthUtils
import com.example.model.PaymentHistoryLog
import com.example.model.PaymentMethod
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommitteeEditorDialog(
    strings: UiStrings,
    existingCommittee: Committee?,
    defaultSettings: AppCompanySettings = AppCompanySettings(),
    hasRecordedPayments: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        memberCount: Long,
        totalShares: Long,
        monthlyInstallment: Long,
        totalMonths: Long,
        collectionStartDay: Long,
        collectionEndDay: Long,
        payoutDay: Long,
        startMonth: String,
        notes: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingCommittee?.name ?: "") }
    var memberCountText by remember {
        mutableStateOf((existingCommittee?.memberCount ?: defaultSettings.defaultTotalShares).toString())
    }
    var totalSharesText by remember {
        mutableStateOf((existingCommittee?.effectiveTotalShares ?: defaultSettings.defaultTotalShares).toString())
    }
    var sharesSyncedToMembers by remember { mutableStateOf(existingCommittee == null) }
    var installmentText by remember {
        mutableStateOf((existingCommittee?.monthlyInstallment ?: defaultSettings.defaultMonthlyContribution).toString())
    }
    var totalMonthsText by remember {
        mutableStateOf((existingCommittee?.totalMonths ?: defaultSettings.defaultDurationMonths).toString())
    }
    var startDayText by remember {
        mutableStateOf((existingCommittee?.collectionDayStart ?: defaultSettings.defaultCollectionStartDay).toString())
    }
    var endDayText by remember {
        mutableStateOf((existingCommittee?.collectionDayEnd ?: defaultSettings.defaultCollectionEndDay).toString())
    }
    var payoutDayText by remember { mutableStateOf((existingCommittee?.payoutDay ?: 8L).toString()) }
    var startMonth by remember { mutableStateOf(existingCommittee?.startMonth ?: MonthUtils.currentMonthKey()) }
    var notes by remember { mutableStateOf(existingCommittee?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showStartedWarning by remember { mutableStateOf(false) }

    val parsedShares = totalSharesText.toLongOrNull()?.coerceIn(1L, 500L) ?: 0L
    val parsedInstallment = installmentText.toLongOrNull()?.coerceIn(100L, 10_000_000L) ?: 0L
    val parsedMonths = totalMonthsText.toLongOrNull()?.coerceIn(2L, 200L) ?: 0L
    val monthlyCollection = parsedShares * parsedInstallment
    val totalCommitteeAmount = monthlyCollection * parsedMonths
    val computedEndMonth = if (MonthUtils.isValidMonthKey(startMonth) && parsedMonths >= 2) {
        MonthUtils.addMonths(startMonth, (parsedMonths - 1).toInt())
    } else "-"

    fun attemptSave(bypassWarning: Boolean) {
        val mCount = memberCountText.toLongOrNull()
        val sCount = totalSharesText.toLongOrNull()
        val inst = installmentText.toLongOrNull()
        val months = totalMonthsText.toLongOrNull()
        val sDay = startDayText.toLongOrNull()
        val eDay = endDayText.toLongOrNull()
        val pDay = payoutDayText.toLongOrNull()

        when {
            name.trim().isEmpty() -> validationError = "Committee name is required"
            mCount == null || mCount !in 1L..500L -> validationError = "Member count must be between 1 and 500"
            sCount == null || sCount !in 1L..500L -> validationError = "Total shares must be between 1 and 500"
            inst == null || inst !in 100L..10_000_000L -> validationError = "Installment must be at least ${defaultSettings.currency} 100"
            months == null || months !in 2L..200L -> validationError = "Duration must be between 2 and 200 months"
            sDay == null || sDay !in 1L..31L -> validationError = "Collection start day must be 1–31"
            eDay == null || eDay !in 1L..31L -> validationError = "Collection end day must be 1–31"
            pDay == null || pDay !in 1L..31L -> validationError = "Payout day must be 1–31"
            !MonthUtils.isValidMonthKey(startMonth.trim()) -> validationError = "Start month must be YYYY-MM (e.g. 2026-11)"
            !bypassWarning && existingCommittee != null && hasRecordedPayments &&
                (mCount != existingCommittee.memberCount ||
                    sCount != existingCommittee.effectiveTotalShares ||
                    inst != existingCommittee.monthlyInstallment ||
                    months != existingCommittee.totalMonths) -> {
                showStartedWarning = true
            }
            else -> {
                onSave(
                    name.trim(), mCount, sCount, inst, months, sDay, eDay, pDay,
                    startMonth.trim(), notes.trim().takeIf { it.isNotEmpty() }
                )
            }
        }
    }

    if (showStartedWarning) {
        AlertDialog(
            onDismissRequest = { showStartedWarning = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusPendingYellow) },
            title = { Text(strings.warningStartedTitle) },
            text = { Text(strings.warningStartedBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showStartedWarning = false
                        attemptSave(bypassWarning = true)
                    }
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showStartedWarning = false }) { Text(strings.cancel) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingCommittee == null) strings.createNewCommittee else strings.editCommittee,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text(strings.committeeName) },
                    placeholder = { Text("e.g. Hasmani Family Committee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("committee_name_input")
                )

                Text(strings.numberOfMembers, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 7, 10, 15, 20, 25).forEach { count ->
                        FilterChip(
                            selected = memberCountText == count.toString(),
                            onClick = {
                                memberCountText = count.toString()
                                if (sharesSyncedToMembers && existingCommittee == null) {
                                    totalSharesText = count.toString()
                                    totalMonthsText = count.toString()
                                }
                            },
                            label = { Text("$count") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memberCountText,
                        onValueChange = {
                            memberCountText = it.filter { c -> c.isDigit() }
                            if (sharesSyncedToMembers && existingCommittee == null && memberCountText.isNotBlank()) {
                                totalSharesText = memberCountText
                                totalMonthsText = memberCountText
                            }
                        },
                        label = { Text(strings.numberOfMembers) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("committee_members_input")
                    )
                    OutlinedTextField(
                        value = totalSharesText,
                        onValueChange = {
                            sharesSyncedToMembers = false
                            totalSharesText = it.filter { c -> c.isDigit() }
                            if (existingCommittee == null && totalSharesText.isNotBlank()) {
                                totalMonthsText = totalSharesText
                            }
                        },
                        label = { Text(strings.totalSharesLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("committee_shares_input")
                    )
                }

                OutlinedTextField(
                    value = totalMonthsText,
                    onValueChange = { totalMonthsText = it.filter { c -> c.isDigit() } },
                    label = { Text(strings.totalDurationMonths) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("committee_months_input")
                )

                Text(strings.monthlyInstallment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000L, 10000L, 15000L, 20000L).forEach { amt ->
                        FilterChip(
                            selected = installmentText == amt.toString(),
                            onClick = { installmentText = amt.toString() },
                            label = { Text(FormatUtils.formatCompactNumber(amt)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = installmentText,
                    onValueChange = { installmentText = it.filter { c -> c.isDigit() } },
                    label = { Text("${strings.monthlyInstallment} (${defaultSettings.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("committee_installment_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startMonth,
                        onValueChange = { startMonth = it },
                        label = { Text(strings.startMonthLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = payoutDayText,
                        onValueChange = { payoutDayText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.payoutDayLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDayText,
                        onValueChange = { startDayText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.collectionStartDay) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDayText,
                        onValueChange = { endDayText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.collectionEndDay) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.paymentNote) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${strings.monthlyCollection}: ${FormatUtils.formatMoney(monthlyCollection, defaultSettings.currency)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${strings.totalCommitteeAmount}: ${FormatUtils.formatMoney(totalCommitteeAmount, defaultSettings.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$parsedShares shares • ${strings.endMonthLabel}: $computedEndMonth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { attemptSave(bypassWarning = false) },
                modifier = Modifier.testTag("save_committee_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun CommitteeDetailDialog(
    strings: UiStrings,
    committee: Committee,
    activeMemberCount: Int,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(committee.name, fontWeight = FontWeight.Bold)
                CommitteeStatusBadge(committee.statusEnum, strings)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(strings.numberOfMembers, "$activeMemberCount / ${committee.memberCount}")
                DetailRow(strings.totalSharesLabel, "${committee.effectiveTotalShares}")
                DetailRow(strings.monthlyInstallment, FormatUtils.formatPkr(committee.monthlyInstallment))
                DetailRow(strings.monthlyCollection, FormatUtils.formatPkr(committee.monthlyCollectionTotal))
                DetailRow(strings.totalCommitteeAmount, FormatUtils.formatPkr(committee.totalCommitteeAmount))
                DetailRow(strings.eachPayoutAmount, FormatUtils.formatPkr(committee.eachPayoutAmount))
                DetailRow(strings.totalDurationMonths, "${committee.totalMonths} (${committee.startMonth} → ${committee.endMonth})")
                DetailRow(strings.collectionWindowLabel, "${committee.collectionDayStart}th – ${committee.collectionDayEnd}th • Payout ${committee.payoutDay}th")
                if (!committee.notes.isNullOrBlank()) {
                    DetailRow(strings.paymentNote, committee.notes)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.printLabel)
                    }
                    OutlinedButton(onClick = onDownload, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.downloadLabel)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.editLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MemberEditorDialog(
    strings: UiStrings,
    existingMember: CommitteeMember?,
    suggestedCommitteeNumber: Long,
    monthlyShareContribution: Long = 5000L,
    existingMembers: List<CommitteeMember> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, committeeNumber: Long, shares: Double) -> Unit
) {
    var name by remember { mutableStateOf(existingMember?.name ?: "") }
    var phone by remember { mutableStateOf(existingMember?.phone ?: "") }
    var numberText by remember {
        mutableStateOf((existingMember?.committeeNumber ?: suggestedCommitteeNumber).toString())
    }
    var sharesText by remember {
        mutableStateOf(
            FormatUtils.formatShares(existingMember?.effectiveShares ?: 1.0)
        )
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedShares = sharesText.toDoubleOrNull()?.coerceIn(0.5, 50.0) ?: 1.0
    val monthlyDuePreview = Math.round(parsedShares * monthlyShareContribution)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingMember == null) strings.addMember else strings.editMember,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("${strings.memberName} *") },
                    placeholder = { Text("e.g. Ghulam Mustafa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("${strings.mobileNumber} *") },
                    placeholder = { Text("e.g. 03001234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_phone_input")
                )
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { numberText = it.filter { c -> c.isDigit() }; errorText = null },
                    label = { Text(strings.committeeNumber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_number_input")
                )

                Text(strings.memberSharesLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0.5", "1", "2", "3").forEach { preset ->
                        FilterChip(
                            selected = sharesText == preset,
                            onClick = { sharesText = preset; errorText = null },
                            label = { Text("$preset Share${if (preset == "1") "" else "s"}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = sharesText,
                    onValueChange = { sharesText = it.filter { c -> c.isDigit() || c == '.' }; errorText = null },
                    label = { Text("${strings.memberSharesLabel} (e.g. 1 or 2)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("member_shares_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${strings.monthlyContributionLabel}: ${FormatUtils.formatPkr(monthlyDuePreview)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = numberText.toLongOrNull()
                    val shares = sharesText.toDoubleOrNull()
                    val conflict = if (num != null && existingMembers.isNotEmpty()) {
                        CommitteeUtils.findDuplicateMember(
                            members = existingMembers,
                            phone = phone,
                            committeeNumber = num,
                            excludeMemberId = existingMember?.id
                        )
                    } else null
                    when {
                        name.trim().isEmpty() -> errorText = "Member name is required"
                        phone.trim().filter { it.isDigit() }.length < 7 -> errorText = "Enter a valid mobile number"
                        num == null || num !in 1L..500L -> errorText = "Committee number must be >= 1"
                        shares == null || shares <= 0.0 || shares > 50.0 -> errorText = "Shares must be between 0.5 and 50"
                        conflict?.type == DuplicateConflictType.PHONE ->
                            errorText = "Mobile number already belongs to #${conflict.existingMember.committeeNumber} ${conflict.existingMember.name}"
                        conflict?.type == DuplicateConflictType.COMMITTEE_NUMBER ->
                            errorText = "Committee #$num is already assigned to ${conflict.existingMember.name}"
                        else -> onSave(name.trim(), phone.trim(), num, shares)
                    }
                },
                modifier = Modifier.testTag("save_member_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun RecordPaymentDialog(
    strings: UiStrings,
    record: InstallmentRecord,
    logs: List<PaymentHistoryLog>,
    onDismiss: () -> Unit,
    onReversePayment: ((reason: String) -> Unit)? = null,
    onSave: (
        paidAmount: Long,
        status: InstallmentStatus,
        paymentDate: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        note: String?
    ) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(record.statusEnum) }
    var paidAmountText by remember {
        mutableStateOf(
            if (record.paidAmount > 0L) record.paidAmount.toString()
            else record.dueAmount.toString()
        )
    }
    var paymentDate by remember { mutableStateOf(record.paymentDate ?: MonthUtils.todayDateString()) }
    var selectedMethod by remember { mutableStateOf(record.paymentMethodEnum) }
    var transactionId by remember { mutableStateOf(record.transactionId ?: "") }
    var note by remember { mutableStateOf(record.paymentNote ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showReverseConfirm by remember { mutableStateOf(false) }
    var reverseReason by remember { mutableStateOf("") }

    val itemLogs = remember(logs, record.id) {
        logs.filter { it.installmentId == record.id }
    }
    val logTimeFmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale.US) }

    if (showReverseConfirm && onReversePayment != null) {
        AlertDialog(
            onDismissRequest = { showReverseConfirm = false },
            icon = { Icon(Icons.Rounded.Undo, contentDescription = null, tint = StatusUnpaidRed) },
            title = { Text(strings.reversePaymentLabel, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reset ${record.memberName}'s payment (${FormatUtils.formatPkr(record.paidAmount)}) to UNPAID while keeping an audit history entry?")
                    OutlinedTextField(
                        value = reverseReason,
                        onValueChange = { reverseReason = it },
                        label = { Text(strings.reverseReasonHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReverseConfirm = false
                        onReversePayment(reverseReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed),
                    modifier = Modifier.testTag("confirm_reverse_payment_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReverseConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(record.memberName, fontWeight = FontWeight.Bold)
                Text(
                    text = "${MonthUtils.formatMonthDisplay(record.monthKey)} • ${strings.dueAmount}: ${FormatUtils.formatPkr(record.dueAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == InstallmentStatus.PAID,
                        onClick = {
                            selectedStatus = InstallmentStatus.PAID
                            paidAmountText = record.dueAmount.toString()
                        },
                        label = { Text(strings.paidLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedStatus == InstallmentStatus.PARTIAL,
                        onClick = {
                            selectedStatus = InstallmentStatus.PARTIAL
                            if (paidAmountText == record.dueAmount.toString() || paidAmountText == "0") {
                                paidAmountText = (record.dueAmount / 2).toString()
                            }
                        },
                        label = { Text(strings.partialLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedStatus == InstallmentStatus.UNPAID,
                        onClick = {
                            selectedStatus = InstallmentStatus.UNPAID
                            paidAmountText = "0"
                        },
                        label = { Text(strings.unpaidLabel) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text(strings.amountPaid) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = selectedStatus != InstallmentStatus.UNPAID,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                if (selectedStatus != InstallmentStatus.UNPAID) {
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text(strings.paymentDate) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text(strings.transactionIdLabel) },
                        placeholder = { Text("e.g. EP-98451234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.paymentNote) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (record.paidAmount > 0L && onReversePayment != null) {
                    OutlinedButton(
                        onClick = { showReverseConfirm = true },
                        modifier = Modifier.fillMaxWidth().testTag("reverse_payment_button")
                    ) {
                        Icon(Icons.Rounded.Undo, contentDescription = null, tint = StatusUnpaidRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.reversePaymentLabel, color = StatusUnpaidRed)
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (itemLogs.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(strings.paymentHistory, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    itemLogs.take(8).forEach { log ->
                        val tsText = logTimeFmt.format(Date(log.createdAtMillis))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "${log.previousStatus} (${FormatUtils.formatCompactNumber(log.previousPaidAmount)}) → ${log.newStatus} (${FormatUtils.formatCompactNumber(log.newPaidAmount)}) [${log.paymentMethod}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = tsText +
                                        (log.transactionId?.let { " • Tx: $it" } ?: "") +
                                        (log.actionNote?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = if (selectedStatus == InstallmentStatus.UNPAID) 0L else paidAmountText.toLongOrNull()
                    if (amt == null || amt < 0L) {
                        errorMsg = "Enter a valid amount"
                    } else {
                        val finalStatus = when {
                            selectedStatus == InstallmentStatus.UNPAID || amt == 0L -> InstallmentStatus.UNPAID
                            amt >= record.dueAmount -> InstallmentStatus.PAID
                            else -> InstallmentStatus.PARTIAL
                        }
                        onSave(
                            amt,
                            finalStatus,
                            paymentDate.trim(),
                            selectedMethod,
                            transactionId.trim().takeIf { it.isNotEmpty() },
                            note.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedStatus) {
                        InstallmentStatus.PAID -> StatusPaidGreen
                        InstallmentStatus.PARTIAL -> StatusPendingYellow
                        InstallmentStatus.UNPAID -> StatusUnpaidRed
                    }
                ),
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun MemberDetailDialog(
    strings: UiStrings,
    member: CommitteeMember,
    monthlyShareContribution: Long = 5000L,
    installments: List<InstallmentRecord>,
    turns: List<CommitteeTurn> = emptyList(),
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: (() -> Unit)? = null,
    onOpenWhatsApp: () -> Unit,
    onOpenSms: () -> Unit
) {
    val memberInstallments = remember(installments, member.id) {
        installments.filter { it.memberId == member.id }.sortedBy { it.monthIndex }
    }
    val memberTurns = remember(turns, member.id, member.name) {
        turns.filter {
            it.recipientMemberId == member.id ||
                it.recipientName.trim().equals(member.name.trim(), ignoreCase = true)
        }.sortedBy { it.turnNumber }
    }
    val totalPaid = memberInstallments.sumOf { it.paidAmount }
    val totalDue = memberInstallments.sumOf { it.dueAmount }
    val monthlyDue = member.monthlyDue(monthlyShareContribution)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("#${member.committeeNumber} • ${member.name}", fontWeight = FontWeight.Bold)
                Text(
                    text = "${member.phone} • ${FormatUtils.formatShares(member.effectiveShares)} Share(s) (${FormatUtils.formatPkr(monthlyDue)}/mo)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.totalReceived}: ${FormatUtils.formatPkr(totalPaid)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusPaidGreen,
                        fontWeight = FontWeight.Bold
                    )
                    PayoutStatusBadge(member.payoutStatusEnum, strings)
                }
                Text(
                    text = "${strings.remainingCollection}: ${FormatUtils.formatPkr((totalDue - totalPaid).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenWhatsApp, modifier = Modifier.weight(1f)) {
                        Text(strings.openWhatsApp)
                    }
                    OutlinedButton(onClick = onOpenSms, modifier = Modifier.weight(1f)) {
                        Text(strings.openSms)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.viewLabel)
                    }
                    if (onPrintPdf != null) {
                        OutlinedButton(
                            onClick = onPrintPdf,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.printLabel)
                        }
                    }
                }

                if (memberTurns.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "${strings.navTurns} (${memberTurns.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    memberTurns.forEach { t ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Turn #${t.turnNumber} • ${FormatUtils.formatPkr(t.payoutAmount)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${t.scheduledMonth} • Date: ${t.payoutDate?.takeIf { it.isNotBlank() } ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PayoutStatusBadge(t.statusEnum, strings)
                            }
                        }
                    }
                }

                HorizontalDivider()

                if (memberInstallments.isEmpty()) {
                    Text(
                        text = strings.noInstallmentsYet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    memberInstallments.forEach { rec ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "M${rec.monthIndex} • ${MonthUtils.formatMonthDisplay(rec.monthKey)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${FormatUtils.formatPkr(rec.paidAmount)} / ${FormatUtils.formatPkr(rec.dueAmount)}" +
                                        (if (rec.paidAmount > 0L) " • ${rec.paymentMethodEnum.label}" else "") +
                                        (rec.transactionId?.let { " ($it)" } ?: "") +
                                        (rec.paymentDate?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            InstallmentStatusBadge(rec.statusEnum, strings)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
fun CommitteeSwitcherDialog(
    strings: UiStrings,
    committees: List<Committee>,
    activeCommitteeId: String?,
    onSelect: (Committee) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.switchCommittee, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                committees.forEach { committee ->
                    val isSelected = committee.id == activeCommitteeId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(committee) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(committee.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${committee.memberCount} Members • ${committee.effectiveTotalShares} Shares • ${FormatUtils.formatPkr(committee.monthlyCollectionTotal)}/mo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                CommitteeStatusBadge(committee.statusEnum, strings)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreateNew, modifier = Modifier.testTag("switcher_create_committee_btn")) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.createNewCommittee)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
fun RecordPayoutDialog(
    strings: UiStrings,
    committee: Committee,
    member: CommitteeMember,
    onDismiss: () -> Unit,
    onConfirm: (payoutAmount: Long, payoutDate: String) -> Unit
) {
    val defaultAmount = if (member.payoutAmount > 0L) member.payoutAmount else committee.eachPayoutAmount
    var amountText by remember { mutableStateOf(defaultAmount.toString()) }
    var dateText by remember { mutableStateOf(member.payoutDate ?: MonthUtils.todayDateString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(strings.recordPayoutTitle, fontWeight = FontWeight.Bold)
                Text(
                    text = "#${member.committeeNumber} • ${member.name} • ${MonthUtils.formatMonthDisplay(member.payoutMonth)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text(strings.payoutAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payout_amount_input")
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; errorMsg = null },
                    label = { Text("${strings.paymentDate} (YYYY-MM-DD or Unknown)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payout_date_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull()
                    when {
                        amt == null || amt <= 0L -> errorMsg = "Enter a valid payout amount"
                        else -> onConfirm(amt, dateText.trim().ifEmpty { "Unknown" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                modifier = Modifier.testTag("confirm_payout_button")
            ) {
                Text(strings.markPayoutPaid)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun PaymentActivityHistoryDialog(
    strings: UiStrings,
    logs: List<PaymentHistoryLog>,
    auditLogs: List<AuditLogEntry> = emptyList(),
    onDismiss: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.US) }
    var tab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.activityHistoryTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = { Text("${strings.paymentEditsTab} (${logs.size})") }
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text("${strings.auditRecordsTab} (${auditLogs.size})") }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tab == 0) {
                        if (logs.isEmpty()) {
                            Text(
                                text = strings.noHistoryYet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            logs.forEach { log ->
                                val tsText = timeFmt.format(Date(log.createdAtMillis))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${log.memberName} • ${MonthUtils.formatMonthDisplay(log.monthKey)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${log.previousStatus} (${FormatUtils.formatCompactNumber(log.previousPaidAmount)}) → ${log.newStatus} (${FormatUtils.formatCompactNumber(log.newPaidAmount)}) [${log.paymentMethod}]",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = tsText +
                                                (log.transactionId?.let { " • Tx: $it" } ?: "") +
                                                (log.actionNote?.let { " • $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (auditLogs.isEmpty()) {
                            Text(
                                text = strings.noHistoryYet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            auditLogs.forEach { entry ->
                                val tsText = timeFmt.format(Date(entry.createdAtMillis))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${entry.actionType} • ${entry.targetName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = entry.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${entry.actorName} • $tsText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
