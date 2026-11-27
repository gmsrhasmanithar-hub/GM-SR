package com.example.ui.components

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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.CommitteeTurn
import com.example.model.FormatUtils
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun AddManualPaymentDialog(
    strings: UiStrings,
    committee: Committee,
    members: List<CommitteeMember>,
    initialMonthKey: String,
    onDismiss: () -> Unit,
    onSave: (
        member: CommitteeMember,
        monthKey: String,
        paidAmount: Long,
        status: InstallmentStatus,
        paymentDate: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        note: String?
    ) -> Unit
) {
    val activeMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
    }
    val monthKeys = remember(committee.startMonth, committee.totalMonths) {
        MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
    }
    var selectedMember by remember { mutableStateOf(activeMembers.firstOrNull()) }
    var selectedMonth by remember { mutableStateOf(if (initialMonthKey in monthKeys) initialMonthKey else monthKeys.firstOrNull() ?: committee.startMonth) }
    var selectedStatus by remember { mutableStateOf(InstallmentStatus.PAID) }
    var amountText by remember {
        mutableStateOf((selectedMember?.monthlyDue(committee.monthlyInstallment) ?: committee.monthlyInstallment).toString())
    }
    var paymentDate by remember { mutableStateOf(MonthUtils.todayDateString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var transactionId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addManualPaymentLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeMembers.isEmpty()) {
                    Text(strings.noMembersYet, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(strings.selectMemberForReport, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeMembers.forEach { m ->
                            FilterChip(
                                selected = selectedMember?.id == m.id,
                                onClick = {
                                    selectedMember = m
                                    amountText = m.monthlyDue(committee.monthlyInstallment).toString()
                                },
                                label = { Text("#${m.committeeNumber} ${m.name}") }
                            )
                        }
                    }

                    Text(strings.currentMonth, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        monthKeys.forEachIndexed { idx, key ->
                            FilterChip(
                                selected = selectedMonth == key,
                                onClick = { selectedMonth = key },
                                label = { Text("M${idx + 1} • ${MonthUtils.formatMonthDisplay(key)}") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatus == InstallmentStatus.PAID,
                            onClick = {
                                selectedStatus = InstallmentStatus.PAID
                                amountText = (selectedMember?.monthlyDue(committee.monthlyInstallment) ?: committee.monthlyInstallment).toString()
                            },
                            label = { Text(strings.paidLabel) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedStatus == InstallmentStatus.PARTIAL,
                            onClick = {
                                selectedStatus = InstallmentStatus.PARTIAL
                                val due = selectedMember?.monthlyDue(committee.monthlyInstallment) ?: committee.monthlyInstallment
                                amountText = (due / 2).toString()
                            },
                            label = { Text(strings.partialLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text(strings.amountPaid) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_payment_amount_input")
                    )

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

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(strings.paymentNote) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedMember != null,
                onClick = {
                    val m = selectedMember ?: return@Button
                    val amt = amountText.toLongOrNull()
                    if (amt == null || amt < 0L) {
                        errorMsg = "Enter a valid payment amount"
                    } else {
                        val due = m.monthlyDue(committee.monthlyInstallment)
                        val finalStatus = when {
                            amt == 0L -> InstallmentStatus.UNPAID
                            amt >= due -> InstallmentStatus.PAID
                            else -> InstallmentStatus.PARTIAL
                        }
                        onSave(
                            m,
                            selectedMonth,
                            amt,
                            finalStatus,
                            paymentDate.trim().ifEmpty { MonthUtils.todayDateString() },
                            selectedMethod,
                            transactionId.trim().takeIf { it.isNotEmpty() },
                            note.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                modifier = Modifier.testTag("save_manual_payment_btn")
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
fun TurnEditorDialog(
    strings: UiStrings,
    committee: Committee,
    existingTurn: CommitteeTurn?,
    suggestedTurnNumber: Long,
    members: List<CommitteeMember>,
    onDismiss: () -> Unit,
    onSave: (
        turnNumber: Long,
        recipientMemberId: String?,
        recipientName: String,
        recipientPhone: String?,
        payoutAmount: Long,
        scheduledMonth: String,
        payoutDate: String?,
        status: PayoutStatus,
        confirmedByAdmin: Boolean,
        notes: String?
    ) -> Unit
) {
    var turnNumberText by remember { mutableStateOf((existingTurn?.turnNumber ?: suggestedTurnNumber).toString()) }
    var selectedMemberId by remember { mutableStateOf(existingTurn?.recipientMemberId) }
    var recipientName by remember { mutableStateOf(existingTurn?.recipientName ?: "") }
    var recipientPhone by remember { mutableStateOf(existingTurn?.recipientPhone ?: "") }
    var payoutAmountText by remember { mutableStateOf((existingTurn?.payoutAmount ?: committee.eachPayoutAmount).toString()) }
    var scheduledMonth by remember { mutableStateOf(existingTurn?.scheduledMonth ?: MonthUtils.currentMonthKey()) }
    var payoutDateText by remember { mutableStateOf(existingTurn?.payoutDate ?: "") }
    var status by remember { mutableStateOf(existingTurn?.statusEnum ?: PayoutStatus.PENDING) }
    var confirmedByAdmin by remember { mutableStateOf(existingTurn?.confirmedByAdmin ?: true) }
    var notes by remember { mutableStateOf(existingTurn?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val activeMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingTurn == null) strings.addTurnLabel else strings.editTurnLabel,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeMembers.isNotEmpty()) {
                    Text("Quick Pick Member (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeMembers.forEach { m ->
                            FilterChip(
                                selected = selectedMemberId == m.id,
                                onClick = {
                                    selectedMemberId = m.id
                                    recipientName = m.name
                                    recipientPhone = m.phone
                                },
                                label = { Text("#${m.committeeNumber} ${m.name}") }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = turnNumberText,
                        onValueChange = { turnNumberText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.turnNumberLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = scheduledMonth,
                        onValueChange = { scheduledMonth = it },
                        label = { Text("Month (YYYY-MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = {
                        recipientName = it
                        selectedMemberId = null
                        errorMsg = null
                    },
                    label = { Text("${strings.memberName} *") },
                    placeholder = { Text("e.g. Ghulam Mustafa or Upcoming") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("turn_recipient_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = { recipientPhone = it },
                        label = { Text(strings.mobileNumber) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = payoutAmountText,
                        onValueChange = { payoutAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.payoutAmountLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = payoutDateText,
                    onValueChange = { payoutDateText = it },
                    label = { Text("${strings.paymentDate} (Optional / Unknown)") },
                    placeholder = { Text("YYYY-MM-DD or leave blank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == PayoutStatus.PAID,
                        onClick = { status = PayoutStatus.PAID },
                        label = { Text(strings.paidLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = status == PayoutStatus.PENDING,
                        onClick = { status = PayoutStatus.PENDING },
                        label = { Text(strings.pendingLabel) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.confirmedByAdminLabel, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = confirmedByAdmin, onCheckedChange = { confirmedByAdmin = it })
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.paymentNote) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tNum = turnNumberText.toLongOrNull()
                    val amt = payoutAmountText.toLongOrNull()
                    when {
                        tNum == null || tNum <= 0L -> errorMsg = "Enter a valid turn number"
                        recipientName.trim().isEmpty() -> errorMsg = "Recipient name is required"
                        amt == null || amt <= 0L -> errorMsg = "Enter a valid payout amount"
                        else -> onSave(
                            tNum,
                            selectedMemberId,
                            recipientName.trim(),
                            recipientPhone.trim().takeIf { it.isNotEmpty() },
                            amt,
                            scheduledMonth.trim().ifEmpty { MonthUtils.currentMonthKey() },
                            payoutDateText.trim().takeIf { it.isNotEmpty() },
                            status,
                            confirmedByAdmin,
                            notes.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                modifier = Modifier.testTag("save_turn_button")
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
fun OldCommitteeRecordEditorDialog(
    strings: UiStrings,
    existingRecord: OldCommitteeRecord?,
    onDismiss: () -> Unit,
    onSave: (OldCommitteeRecord) -> Unit
) {
    var committeeNumber by remember { mutableStateOf(existingRecord?.committeeNumber ?: "") }
    var committeeName by remember { mutableStateOf(existingRecord?.committeeName ?: "") }
    var startDate by remember { mutableStateOf(existingRecord?.startDate ?: "") }
    var endDate by remember { mutableStateOf(existingRecord?.endDate ?: "") }
    var memberNames by remember { mutableStateOf(existingRecord?.memberNames ?: "") }
    var contactNumbers by remember { mutableStateOf(existingRecord?.contactNumbers ?: "") }
    var numberOfShares by remember { mutableStateOf(existingRecord?.numberOfShares ?: "") }
    var monthlyContributions by remember { mutableStateOf(existingRecord?.monthlyContributions ?: "") }
    var totalCollection by remember { mutableStateOf(existingRecord?.totalCollection ?: "") }
    var paymentHistory by remember { mutableStateOf(existingRecord?.paymentHistory ?: "") }
    var payoutRecipient by remember { mutableStateOf(existingRecord?.payoutRecipient ?: "") }
    var payoutAmount by remember { mutableStateOf(existingRecord?.payoutAmount ?: "") }
    var payoutDate by remember { mutableStateOf(existingRecord?.payoutDate ?: "") }
    var status by remember { mutableStateOf(existingRecord?.status ?: CommitteeStatus.COMPLETED.name) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingRecord == null) strings.addOldRecordLabel else strings.editOldRecordLabel,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = committeeNumber,
                        onValueChange = { committeeNumber = it },
                        label = { Text("Committee #") },
                        placeholder = { Text("1, 2, 3...") },
                        singleLine = true,
                        modifier = Modifier.weight(0.75f)
                    )
                    OutlinedTextField(
                        value = committeeName,
                        onValueChange = { committeeName = it; errorMsg = null },
                        label = { Text("${strings.committeeName} *") },
                        placeholder = { Text("e.g. Old Committee 1") },
                        singleLine = true,
                        modifier = Modifier.weight(1.25f).testTag("old_record_name_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("Unknown") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date") },
                        placeholder = { Text("Unknown") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = payoutRecipient,
                        onValueChange = { payoutRecipient = it; errorMsg = null },
                        label = { Text("Payout Recipient") },
                        placeholder = { Text("e.g. Ghulam Mustafa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = payoutAmount,
                        onValueChange = { payoutAmount = it },
                        label = { Text("Payout Amount") },
                        placeholder = { Text("Rs. 50,000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = payoutDate,
                    onValueChange = { payoutDate = it },
                    label = { Text("Payout Date (Blank / Unknown allowed)") },
                    placeholder = { Text("Unknown") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numberOfShares,
                        onValueChange = { numberOfShares = it },
                        label = { Text("Shares") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = monthlyContributions,
                        onValueChange = { monthlyContributions = it },
                        label = { Text("Monthly Share") },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                    OutlinedTextField(
                        value = totalCollection,
                        onValueChange = { totalCollection = it },
                        label = { Text("Total Pool") },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                }
                OutlinedTextField(
                    value = memberNames,
                    onValueChange = { memberNames = it },
                    label = { Text("Member Names (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactNumbers,
                    onValueChange = { contactNumbers = it },
                    label = { Text("Contact Numbers (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentHistory,
                    onValueChange = { paymentHistory = it },
                    label = { Text("Payment History / Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == CommitteeStatus.COMPLETED.name || status == "PAID",
                        onClick = { status = CommitteeStatus.COMPLETED.name },
                        label = { Text(strings.completedLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = status == "PENDING" || status == CommitteeStatus.ACTIVE.name,
                        onClick = { status = "PENDING" },
                        label = { Text(strings.pendingLabel) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (committeeName.trim().isEmpty() && payoutRecipient.trim().isEmpty()) {
                        errorMsg = "Enter Committee Name or Payout Recipient"
                        return@Button
                    }
                    val rec = OldCommitteeRecord(
                        id = existingRecord?.id ?: java.util.UUID.randomUUID().toString(),
                        committeeNumber = committeeNumber.trim(),
                        committeeName = committeeName.trim().ifEmpty { payoutRecipient.trim() },
                        startDate = startDate.trim().ifEmpty { "Unknown" },
                        endDate = endDate.trim().ifEmpty { "Unknown" },
                        memberNames = memberNames.trim(),
                        contactNumbers = contactNumbers.trim(),
                        numberOfShares = numberOfShares.trim(),
                        monthlyContributions = monthlyContributions.trim(),
                        totalCollection = totalCollection.trim(),
                        paymentHistory = paymentHistory.trim(),
                        payoutRecipient = payoutRecipient.trim().ifEmpty { "Unknown" },
                        payoutAmount = payoutAmount.trim(),
                        payoutDate = payoutDate.trim().ifEmpty { "Unknown" },
                        status = status,
                        createdAtMillis = existingRecord?.createdAtMillis ?: System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    onSave(rec)
                },
                modifier = Modifier.testTag("save_old_record_btn")
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
fun OldCommitteeRecordDetailDialog(
    strings: UiStrings,
    record: OldCommitteeRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val numPrefix = record.committeeNumber.takeIf { it.isNotBlank() }?.let { "#$it • " } ?: ""
                Text("$numPrefix${record.committeeName}", fontWeight = FontWeight.Bold)
                Text(
                    text = record.payoutRecipient.ifBlank { "Unknown Recipient" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Status", record.status)
                DetailRow("Payout Recipient", record.payoutRecipient.ifBlank { "Unknown" })
                DetailRow("Payout Amount", record.payoutAmount.ifBlank { "Unknown" })
                DetailRow("Payout Date", record.payoutDate.ifBlank { "Unknown" })
                DetailRow("Start Date", record.startDate.ifBlank { "Unknown" })
                DetailRow("End Date", record.endDate.ifBlank { "Unknown" })
                DetailRow("Shares", record.numberOfShares.ifBlank { "Unknown" })
                DetailRow("Monthly Share", record.monthlyContributions.ifBlank { "Unknown" })
                DetailRow("Total Collection", record.totalCollection.ifBlank { "Unknown" })
                if (record.memberNames.isNotBlank()) DetailRow("Members", record.memberNames)
                if (record.contactNumbers.isNotBlank()) DetailRow("Contacts", record.contactNumbers)
                if (record.paymentHistory.isNotBlank()) DetailRow("Payment History", record.paymentHistory)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.editLabel)
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = StatusUnpaidRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.deleteLabel, color = StatusUnpaidRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
