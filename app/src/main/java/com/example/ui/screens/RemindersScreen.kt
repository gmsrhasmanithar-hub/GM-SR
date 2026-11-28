package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MessagingGatewayConfig
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeUtils
import com.example.model.DispatchStatus
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import com.example.model.UrduMessageType
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SmsBlue
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WhatsAppGreen
import androidx.compose.material3.Switch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    strings: UiStrings,
    language: AppLanguage,
    companySettings: AppCompanySettings,
    committee: Committee?,
    members: List<CommitteeMember>,
    installments: List<InstallmentRecord>,
    reminderLogs: List<ReminderLog>,
    selectedMonthKey: String,
    gatewayConfig: MessagingGatewayConfig,
    savedTemplates: Map<Pair<ReminderStage, AppLanguage>, String>,
    buildPaymentRequestText: (memberName: String, amount: Long, dueDate: String) -> String,
    onCopyMessage: (String) -> Unit,
    onShareRawWhatsApp: (phone: String, message: String) -> Unit,
    onShareRawSms: (phone: String, message: String) -> Unit,
    renderPreview: (template: String, member: CommitteeMember, dueAmount: Long) -> String,
    onSelectMonth: (String) -> Unit,
    onSaveTemplate: (ReminderStage, AppLanguage, String) -> Unit,
    onSendViaApi: (recipients: List<Pair<CommitteeMember, Long>>, useWhatsApp: Boolean, stage: ReminderStage, template: String) -> Unit,
    onOpenManualApp: (member: CommitteeMember, dueAmount: Long, useWhatsApp: Boolean, stage: ReminderStage, template: String) -> Boolean,
    renderUrduPreview: (type: UrduMessageType, memberName: String, amount: Long, date: String, remainingAmount: Long, adminMessage: String, customTemplate: String?) -> String,
    onSendUrduMessages: (
        type: UrduMessageType,
        recipients: List<CommitteeMember>,
        installmentsForMonth: List<InstallmentRecord>,
        customBodyOrTemplate: String,
        adminMessage: String,
        winnerDate: String,
        sendWhatsApp: Boolean,
        sendSms: Boolean,
        sendInApp: Boolean,
        preventDuplicateInApp: Boolean
    ) -> Unit,
    onOpenGatewaySettings: () -> Unit,
    onCreateCommittee: () -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    var modeTab by remember { mutableIntStateOf(0) } // 0 = 6 Urdu Templates, 1 = Payment Request, 2 = Bulk Reminders
    var selectedStage by remember { mutableStateOf(ReminderStage.FIRST_28) }
    var useWhatsApp by remember { mutableStateOf(true) }
    var showExcludedPaid by remember { mutableStateOf(false) }

    val activeMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
    }

    // Payment Request generator state
    var reqMemberId by remember { mutableStateOf(activeMembers.firstOrNull()?.id) }
    var reqName by remember { mutableStateOf(activeMembers.firstOrNull()?.name ?: "") }
    var reqPhone by remember { mutableStateOf(activeMembers.firstOrNull()?.phone ?: "") }
    var reqAmountText by remember {
        mutableStateOf((activeMembers.firstOrNull()?.monthlyDue(committee.monthlyInstallment) ?: committee.monthlyInstallment).toString())
    }
    var reqDueDate by remember(selectedMonthKey, committee.collectionDayStart, committee.collectionDayEnd) {
        mutableStateOf(MonthUtils.formatDueDate(selectedMonthKey, committee.collectionDayStart, committee.collectionDayEnd))
    }
    var reqCustomBody by remember(reqName, reqAmountText, reqDueDate, companySettings) {
        mutableStateOf(
            buildPaymentRequestText(
                reqName.ifBlank { "Member" },
                reqAmountText.toLongOrNull() ?: committee.monthlyInstallment,
                reqDueDate
            )
        )
    }

    val defaultTemplateForStage = remember(selectedStage, language, savedTemplates) {
        savedTemplates[selectedStage to language]
            ?: if (language == AppLanguage.UR) selectedStage.defaultTemplateUr else selectedStage.defaultTemplateEn
    }
    var messageText by remember { mutableStateOf(defaultTemplateForStage) }
    LaunchedEffect(selectedStage, language, savedTemplates) {
        messageText = savedTemplates[selectedStage to language]
            ?: if (language == AppLanguage.UR) selectedStage.defaultTemplateUr else selectedStage.defaultTemplateEn
    }

    val monthInstallments = remember(installments, selectedMonthKey) {
        installments.filter { it.monthKey == selectedMonthKey }
    }

    val eligibility = remember(members, monthInstallments, committee.monthlyInstallment) {
        CommitteeUtils.filterUnpaidForReminders(members, monthInstallments, committee.monthlyInstallment)
    }
    val paidExcludedMembers = eligibility.excludedPaidMembers
    val eligibleRecipients = eligibility.eligibleUnpaidOrPartial

    val selectedIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(eligibleRecipients) {
        val validIds = eligibleRecipients.map { it.first.id }.toSet()
        selectedIds.removeAll { it !in validIds }
        if (selectedIds.isEmpty() && eligibleRecipients.isNotEmpty()) {
            selectedIds.addAll(validIds)
        }
    }

    var manualQueueList by remember { mutableStateOf<List<Pair<CommitteeMember, Long>>>(emptyList()) }
    var manualQueueIndex by remember { mutableIntStateOf(0) }

    if (manualQueueList.isNotEmpty() && manualQueueIndex < manualQueueList.size) {
        val (targetMember, dueAmt) = manualQueueList[manualQueueIndex]
        val preview = renderPreview(messageText, targetMember, dueAmt)
        AlertDialog(
            onDismissRequest = { manualQueueList = emptyList() },
            title = {
                Text("${strings.manualQueueTitle} (${manualQueueIndex + 1}/${manualQueueList.size})", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "#${targetMember.committeeNumber} • ${targetMember.name} (${targetMember.phone})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onOpenManualApp(targetMember, dueAmt, useWhatsApp, selectedStage, messageText)
                        if (manualQueueIndex + 1 < manualQueueList.size) {
                            manualQueueIndex++
                        } else {
                            manualQueueList = emptyList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useWhatsApp) WhatsAppGreen else SmsBlue
                    )
                ) {
                    Text(strings.markSentAndNext)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (manualQueueIndex + 1 < manualQueueList.size) manualQueueIndex++
                            else manualQueueList = emptyList()
                        }
                    ) { Text(strings.skipMember) }
                    TextButton(onClick = { manualQueueList = emptyList() }) { Text(strings.close) }
                }
            }
        )
    }

    val isApiReadyForSelectedChannel = if (useWhatsApp) gatewayConfig.isWhatsAppApiConfigured else gatewayConfig.isSmsGatewayConfigured
    val timeFmt = remember { SimpleDateFormat("dd MMM HH:mm", Locale.US) }

    // Urdu 6-Template Control Panel State
    var urduType by remember { mutableStateOf(UrduMessageType.MONTHLY_PAYMENT_REMINDER) }
    var urduTargetMode by remember { mutableIntStateOf(0) } // 0 = All Active, 1 = Unpaid/Overdue, 2 = Custom Select
    val urduSelectedIds = remember { mutableStateListOf<String>() }
    var urduDateInput by remember(selectedMonthKey) { mutableStateOf(MonthUtils.todayDateString()) }
    var urduAdminNote by remember { mutableStateOf("") }
    var sendChannelWa by remember { mutableStateOf(true) }
    var sendChannelSms by remember { mutableStateOf(false) }
    var sendChannelInApp by remember { mutableStateOf(true) }
    var preventDuplicateInApp by remember { mutableStateOf(true) }

    LaunchedEffect(activeMembers) {
        val valid = activeMembers.map { it.id }.toSet()
        urduSelectedIds.removeAll { it !in valid }
        if (urduSelectedIds.isEmpty() && activeMembers.isNotEmpty()) {
            urduSelectedIds.addAll(valid)
        }
    }

    val sampleMember = activeMembers.firstOrNull()
    val sampleInst = monthInstallments.firstOrNull { it.memberId == sampleMember?.id }
    val sampleDue = sampleInst?.dueAmount ?: sampleMember?.monthlyDue(committee.monthlyInstallment) ?: committee.monthlyInstallment
    val samplePaid = sampleInst?.paidAmount ?: sampleDue
    val sampleRemaining = sampleInst?.remainingAmount ?: sampleDue
    val urduPreviewGenerated = remember(urduType, sampleMember, samplePaid, sampleDue, sampleRemaining, urduDateInput, urduAdminNote) {
        val amtForType = when (urduType) {
            UrduMessageType.PAYMENT_RECEIVED -> samplePaid.coerceAtLeast(sampleDue)
            UrduMessageType.PENDING_PAYMENT_REMINDER -> sampleRemaining.coerceAtLeast(sampleDue)
            UrduMessageType.WINNER_ANNOUNCEMENT -> committee.eachPayoutAmount
            else -> sampleDue
        }
        renderUrduPreview(
            urduType,
            sampleMember?.name ?: "Member",
            amtForType,
            urduDateInput,
            sampleRemaining,
            urduAdminNote.ifBlank { "براہ کرم وقت پر ادائیگی یقینی بنائیں۔" },
            null
        )
    }
    var urduEditableBody by remember(urduPreviewGenerated) { mutableStateOf(urduPreviewGenerated) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = modeTab) {
            Tab(
                selected = modeTab == 0,
                onClick = { modeTab = 0 },
                text = { Text("Urdu (A–F)", maxLines = 1, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_urdu_templates")
            )
            Tab(
                selected = modeTab == 1,
                onClick = { modeTab = 1 },
                text = { Text(strings.paymentRequestTitle, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_payment_request")
            )
            Tab(
                selected = modeTab == 2,
                onClick = { modeTab = 2 },
                text = { Text(strings.bulkRemindersTitle, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_bulk_reminders")
            )
        }

        if (modeTab == 0) {
            val urduTargets = remember(urduTargetMode, activeMembers, eligibleRecipients, urduSelectedIds.toList()) {
                when (urduTargetMode) {
                    0 -> activeMembers
                    1 -> eligibleRecipients.map { it.first }
                    else -> activeMembers.filter { it.id in urduSelectedIds }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MonthSelectorBar(
                        committee = committee,
                        selectedMonthKey = selectedMonthKey,
                        strings = strings,
                        onSelectMonth = onSelectMonth
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${companySettings.companyName} • 6 Urdu Templates (A–F)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val labels = listOf(
                                    UrduMessageType.MONTHLY_PAYMENT_REMINDER to "A. ماہانہ ادائیگی",
                                    UrduMessageType.PAYMENT_RECEIVED to "B. وصولی تصدیق",
                                    UrduMessageType.PENDING_PAYMENT_REMINDER to "C. بقایا ادائیگی",
                                    UrduMessageType.WINNER_ANNOUNCEMENT to "D. کمیٹی ونر",
                                    UrduMessageType.MONTHLY_COMMITTEE_ANNOUNCEMENT to "E. ماہانہ اعلان",
                                    UrduMessageType.GENERAL_ANNOUNCEMENT to "F. عمومی اعلان"
                                )
                                labels.forEach { (t, lbl) ->
                                    FilterChip(
                                        selected = urduType == t,
                                        onClick = { urduType = t },
                                        label = { Text(lbl) },
                                        modifier = Modifier.testTag("urdu_type_${t.name}")
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = urduDateInput,
                                onValueChange = { urduDateInput = it },
                                label = { Text("${strings.paymentDate} [Date]") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (urduType == UrduMessageType.GENERAL_ANNOUNCEMENT) {
                                OutlinedTextField(
                                    value = urduAdminNote,
                                    onValueChange = { urduAdminNote = it },
                                    label = { Text("[Admin Message] / ایڈمن کا پیغام") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth().testTag("urdu_admin_message_input")
                                )
                            }

                            OutlinedTextField(
                                value = urduEditableBody,
                                onValueChange = { urduEditableBody = it },
                                label = { Text("Editable Urdu Preview (keeps G M S R HASHMANI THAR & 03423498631)") },
                                minLines = 6,
                                maxLines = 12,
                                modifier = Modifier.fillMaxWidth().testTag("urdu_message_preview_input")
                            )

                            Text("Target Recipients (${urduTargets.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = urduTargetMode == 0,
                                    onClick = { urduTargetMode = 0 },
                                    label = { Text("All Active (${activeMembers.size})") }
                                )
                                FilterChip(
                                    selected = urduTargetMode == 1,
                                    onClick = { urduTargetMode = 1 },
                                    label = { Text("Unpaid Only (${eligibleRecipients.size})") }
                                )
                                FilterChip(
                                    selected = urduTargetMode == 2,
                                    onClick = { urduTargetMode = 2 },
                                    label = { Text("Select (${urduSelectedIds.size})") }
                                )
                            }

                            if (urduTargetMode == 2) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    activeMembers.forEach { m ->
                                        val isChecked = m.id in urduSelectedIds
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                if (isChecked) urduSelectedIds.remove(m.id) else urduSelectedIds.add(m.id)
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { c ->
                                                    if (c) {
                                                        if (m.id !in urduSelectedIds) urduSelectedIds.add(m.id)
                                                    } else {
                                                        urduSelectedIds.remove(m.id)
                                                    }
                                                }
                                            )
                                            Text("${m.displayMemberId} • ${m.name} (${m.phone})", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = sendChannelInApp,
                                    onClick = { sendChannelInApp = !sendChannelInApp },
                                    label = { Text("In-App") }
                                )
                                FilterChip(
                                    selected = sendChannelWa,
                                    onClick = { sendChannelWa = !sendChannelWa },
                                    label = { Text("WhatsApp") }
                                )
                                FilterChip(
                                    selected = sendChannelSms,
                                    onClick = { sendChannelSms = !sendChannelSms },
                                    label = { Text("SMS") }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Prevent duplicate In-App notification this month", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Switch(checked = preventDuplicateInApp, onCheckedChange = { preventDuplicateInApp = it })
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onCopyMessage(urduEditableBody) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.copyMessageLabel, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        onSendUrduMessages(
                                            urduType,
                                            urduTargets,
                                            monthInstallments,
                                            urduEditableBody,
                                            urduAdminNote,
                                            urduDateInput,
                                            sendChannelWa,
                                            sendChannelSms,
                                            sendChannelInApp,
                                            preventDuplicateInApp
                                        )
                                    },
                                    enabled = urduTargets.isNotEmpty() && (sendChannelInApp || sendChannelWa || sendChannelSms),
                                    modifier = Modifier.weight(1.2f).testTag("send_urdu_message_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send (${urduTargets.size})", maxLines = 1, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (modeTab == 1) {
            // Payment Request Message System (Easypaisa + Copy + WhatsApp + SMS)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MonthSelectorBar(
                        committee = committee,
                        selectedMonthKey = selectedMonthKey,
                        strings = strings,
                        onSelectMonth = onSelectMonth
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Easypaisa: ${companySettings.easypaisaNumber} (${companySettings.easypaisaAccountHolder})",
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusPaidGreen,
                                fontWeight = FontWeight.Bold
                            )

                            if (activeMembers.isNotEmpty()) {
                                Text(strings.selectMemberForReport, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    activeMembers.forEach { m ->
                                        FilterChip(
                                            selected = reqMemberId == m.id,
                                            onClick = {
                                                reqMemberId = m.id
                                                reqName = m.name
                                                reqPhone = m.phone
                                                reqAmountText = m.monthlyDue(committee.monthlyInstallment).toString()
                                            },
                                            label = { Text("#${m.committeeNumber} ${m.name}") }
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = reqName,
                                    onValueChange = { reqName = it; reqMemberId = null },
                                    label = { Text(strings.memberName) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("req_member_name_input")
                                )
                                OutlinedTextField(
                                    value = reqPhone,
                                    onValueChange = { reqPhone = it },
                                    label = { Text(strings.mobileNumber) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("req_member_phone_input")
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = reqAmountText,
                                    onValueChange = { reqAmountText = it.filter { c -> c.isDigit() } },
                                    label = { Text("${strings.dueAmount} (${companySettings.currency})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = reqDueDate,
                                    onValueChange = { reqDueDate = it },
                                    label = { Text(strings.nextDueDateLabel) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = reqCustomBody,
                                onValueChange = { reqCustomBody = it },
                                label = { Text(strings.reminderMessage) },
                                minLines = 6,
                                maxLines = 10,
                                modifier = Modifier.fillMaxWidth().testTag("payment_request_preview_input")
                            )

                            Button(
                                onClick = { onCopyMessage(reqCustomBody) },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("copy_payment_request_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.copyMessageLabel, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onShareRawWhatsApp(reqPhone, reqCustomBody) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_request_whatsapp_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareWhatsAppLabel, maxLines = 1)
                                }
                                Button(
                                    onClick = { onShareRawSms(reqPhone, reqCustomBody) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SmsBlue),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_request_sms_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareSmsLabel, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MonthSelectorBar(
                        committee = committee,
                        selectedMonthKey = selectedMonthKey,
                        strings = strings,
                        onSelectMonth = onSelectMonth
                    )
                }

                // Channel + API Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = useWhatsApp,
                                        onClick = { useWhatsApp = true },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.channelWhatsApp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WhatsAppGreen,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("channel_whatsapp_chip")
                                    )
                                    FilterChip(
                                        selected = !useWhatsApp,
                                        onClick = { useWhatsApp = false },
                                        leadingIcon = { Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.channelSms, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SmsBlue,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("channel_sms_chip")
                                    )
                                }
                                IconButton(onClick = onOpenGatewaySettings) {
                                    Icon(Icons.Rounded.Settings, contentDescription = strings.messagingApiSettingsTitle)
                                }
                            }

                            Surface(
                                color = if (isApiReadyForSelectedChannel) StatusPaidBg else StatusPendingBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isApiReadyForSelectedChannel) strings.apiConfiguredBadge else strings.manualModeBadge,
                                    color = if (isApiReadyForSelectedChannel) StatusPaidGreen else StatusPendingYellow,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 5-Stage Reminder Schedule
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.reminderSchedule, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val stageLabels = listOf(
                                ReminderStage.FIRST_28 to strings.stage28th,
                                ReminderStage.SECOND_3_4 to strings.stage3rd4th,
                                ReminderStage.THIRD_5 to strings.stage5th,
                                ReminderStage.FINAL_7 to strings.stage7th,
                                ReminderStage.LAST_8 to strings.stage8th,
                                ReminderStage.CUSTOM to strings.stageCustom
                            )
                            stageLabels.forEach { (stage, label) ->
                                FilterChip(
                                    selected = selectedStage == stage,
                                    onClick = { selectedStage = stage },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("stage_chip_${stage.name}")
                                )
                            }
                        }
                    }
                }

                // Editable Message Composer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = { Text(strings.reminderMessage) },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth().testTag("reminder_message_input")
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("{name}", "{amount}", "{month}", "{committee}", "{window}", "{easypaisa}", "{holder}").forEach { tag ->
                                    AssistChip(
                                        onClick = { messageText = "$messageText $tag" },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onSaveTemplate(selectedStage, language, messageText) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.save)
                            }
                        }
                    }
                }

                // Paid-Member Auto Exclusion Banner
                item {
                    Surface(
                        color = StatusPaidBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExcludedPaid = !showExcludedPaid }
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "${strings.paidExcludedBanner} (${paidExcludedMembers.size} PAID)",
                                        color = StatusPaidGreen,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (paidExcludedMembers.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (showExcludedPaid) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = strings.showExcludedPaid,
                                        tint = StatusPaidGreen
                                    )
                                }
                            }
                            AnimatedVisibility(visible = showExcludedPaid && paidExcludedMembers.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    paidExcludedMembers.forEach { m ->
                                        Text(
                                            text = "✓ #${m.committeeNumber} ${m.name} (${m.phone}) — PAID",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusPaidGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bulk Select & Send Action Bar
                if (eligibleRecipients.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    selectedIds.clear()
                                    selectedIds.addAll(eligibleRecipients.map { it.first.id })
                                }) { Text(strings.selectAllUnpaid) }
                                TextButton(onClick = { selectedIds.clear() }) { Text(strings.clearSelection) }
                            }
                            Button(
                                onClick = {
                                    val targets = eligibleRecipients.filter { it.first.id in selectedIds }
                                    if (isApiReadyForSelectedChannel) {
                                        onSendViaApi(targets, useWhatsApp, selectedStage, messageText)
                                    } else {
                                        manualQueueList = targets
                                        manualQueueIndex = 0
                                    }
                                },
                                enabled = selectedIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useWhatsApp) WhatsAppGreen else SmsBlue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("send_selected_reminders_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${strings.sendSelected} (${selectedIds.size})")
                            }
                        }
                    }
                }

                if (eligibleRecipients.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(28.dp))
                                Text(
                                    text = strings.noUnpaidMembers,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusPaidGreen
                                )
                            }
                        }
                    }
                } else {
                    items(eligibleRecipients, key = { it.first.id }) { (member, dueAmt) ->
                        val checked = member.id in selectedIds
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (member.id !in selectedIds) selectedIds.add(member.id)
                                        } else {
                                            selectedIds.remove(member.id)
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${member.committeeNumber} • ${member.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${member.phone} • ${FormatUtils.formatMoney(dueAmt, companySettings.currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (gatewayConfig.isWhatsAppApiConfigured) {
                                            onSendViaApi(listOf(member to dueAmt), true, selectedStage, messageText)
                                        } else {
                                            onOpenManualApp(member, dueAmt, true, selectedStage, messageText)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = strings.openWhatsApp, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (gatewayConfig.isSmsGatewayConfigured) {
                                            onSendViaApi(listOf(member to dueAmt), false, selectedStage, messageText)
                                        } else {
                                            onOpenManualApp(member, dueAmt, false, selectedStage, messageText)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Sms, contentDescription = strings.openSms, tint = SmsBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Dispatch Logs
                if (reminderLogs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(strings.reminderLogsTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(reminderLogs.take(20), key = { it.id }) { log ->
                        val ts = timeFmt.format(Date(log.createdAtMillis))
                        val statusColor = when (DispatchStatus.fromRaw(log.dispatchStatus)) {
                            DispatchStatus.SENT_API -> StatusPaidGreen
                            DispatchStatus.OPENED_APP -> StatusPendingYellow
                            DispatchStatus.FAILED -> StatusUnpaidRed
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${log.memberName} • ${log.channel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.dispatchStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${log.scheduleStage} • $ts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!log.errorDetail.isNullOrBlank()) {
                                    Text(
                                        text = log.errorDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusUnpaidRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
