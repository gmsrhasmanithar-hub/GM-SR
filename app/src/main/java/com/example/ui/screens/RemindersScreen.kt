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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MessagingGatewayConfig
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeUtils
import com.example.model.DispatchStatus
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.MemberStatus
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.SmsBlue
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WhatsAppGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    strings: UiStrings,
    language: AppLanguage,
    committee: Committee?,
    members: List<CommitteeMember>,
    installments: List<InstallmentRecord>,
    reminderLogs: List<ReminderLog>,
    selectedMonthKey: String,
    gatewayConfig: MessagingGatewayConfig,
    savedTemplates: Map<Pair<ReminderStage, AppLanguage>, String>,
    renderPreview: (template: String, member: CommitteeMember, dueAmount: Long) -> String,
    onSelectMonth: (String) -> Unit,
    onSaveTemplate: (ReminderStage, AppLanguage, String) -> Unit,
    onSendViaApi: (recipients: List<Pair<CommitteeMember, Long>>, useWhatsApp: Boolean, stage: ReminderStage, template: String) -> Unit,
    onOpenManualApp: (member: CommitteeMember, dueAmount: Long, useWhatsApp: Boolean, stage: ReminderStage, template: String) -> Boolean,
    onOpenGatewaySettings: () -> Unit,
    onCreateCommittee: () -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    val context = LocalContext.current
    var selectedStage by remember { mutableStateOf(ReminderStage.FIRST_28) }
    var useWhatsApp by remember { mutableStateOf(true) }
    var showExcludedPaid by remember { mutableStateOf(false) }

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

    // CRITICAL BUSINESS RULE: Automatically exclude PAID members from all reminders for this month
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

    // Guided Manual Queue Dialog State
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
                        listOf("{name}", "{amount}", "{month}", "{committee}", "{window}").forEach { tag ->
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
                                text = "${member.phone} • ${FormatUtils.formatPkr(dueAmt)}",
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
