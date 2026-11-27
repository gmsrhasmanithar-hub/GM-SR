package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MonthUtils
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.InstallmentStatusBadge
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.SmsBlue
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.WhatsAppGreen
import com.example.viewmodel.InstallmentFilter

@Composable
fun InstallmentsScreen(
    strings: UiStrings,
    committee: Committee?,
    members: List<CommitteeMember>,
    installments: List<InstallmentRecord>,
    selectedMonthKey: String,
    activeFilter: InstallmentFilter,
    onSelectMonth: (String) -> Unit,
    onSelectFilter: (InstallmentFilter) -> Unit,
    onSyncMonth: () -> Unit,
    onDeleteUnpaidMonth: (String) -> Unit,
    onQuickMarkPaid: (InstallmentRecord) -> Unit,
    onEditPayment: (InstallmentRecord) -> Unit,
    onDeleteInstallment: (InstallmentRecord) -> Unit,
    onQuickWhatsApp: (CommitteeMember, Long) -> Unit,
    onQuickSms: (CommitteeMember, Long) -> Unit,
    onOpenActivityHistory: () -> Unit,
    onCreateCommittee: () -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    var searchText by remember { mutableStateOf("") }
    val memberMap = remember(members) { members.associateBy { it.id } }

    val monthRecords = remember(installments, selectedMonthKey) {
        installments.filter { it.monthKey == selectedMonthKey }.sortedBy { it.committeeNumber }
    }

    val totalReceived = monthRecords.sumOf { it.paidAmount }
    val totalRemaining = monthRecords.sumOf { it.remainingAmount }
    val paidCount = monthRecords.count { it.statusEnum == InstallmentStatus.PAID }
    val unpaidCount = monthRecords.count { it.statusEnum == InstallmentStatus.UNPAID }
    val partialCount = monthRecords.count { it.statusEnum == InstallmentStatus.PARTIAL }
    val hasUnpaidInMonth = monthRecords.any { it.paidAmount == 0L }

    val visibleRecords = remember(monthRecords, activeFilter, searchText) {
        val byStatus = when (activeFilter) {
            InstallmentFilter.ALL -> monthRecords
            InstallmentFilter.PAID -> monthRecords.filter { it.statusEnum == InstallmentStatus.PAID }
            InstallmentFilter.UNPAID -> monthRecords.filter { it.statusEnum == InstallmentStatus.UNPAID }
            InstallmentFilter.PARTIAL -> monthRecords.filter { it.statusEnum == InstallmentStatus.PARTIAL }
        }
        val q = searchText.trim().lowercase()
        if (q.isEmpty()) byStatus
        else byStatus.filter {
            it.memberName.lowercase().contains(q) || it.memberPhone.lowercase().contains(q)
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

        // Monthly Collection Summary + Action Buttons
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.totalReceived, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatPkr(totalReceived),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusPaidGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.remainingCollection, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = FormatUtils.formatPkr(totalRemaining),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalRemaining == 0L) StatusPaidGreen else StatusUnpaidRed
                            )
                        }
                    }

                    Text(
                        text = "${strings.paidLabel}: $paidCount  •  ${strings.partialLabel}: $partialCount  •  ${strings.unpaidLabel}: $unpaidCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSyncMonth,
                            modifier = Modifier.weight(1f).testTag("sync_installments_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.generateInstallments, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenActivityHistory,
                            modifier = Modifier.testTag("activity_history_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = strings.activityHistoryTitle, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.paymentHistory, maxLines = 1)
                        }
                    }

                    if (hasUnpaidInMonth) {
                        OutlinedButton(
                            onClick = { onDeleteUnpaidMonth(selectedMonthKey) },
                            modifier = Modifier.fillMaxWidth().testTag("delete_unpaid_month_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = StatusUnpaidRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.deleteUnpaidMonthLabel} (${MonthUtils.formatMonthDisplay(selectedMonthKey)})",
                                color = StatusUnpaidRed,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(strings.searchMembersHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == InstallmentFilter.ALL,
                        onClick = { onSelectFilter(InstallmentFilter.ALL) },
                        label = { Text("${strings.allFilter} (${monthRecords.size})") }
                    )
                    FilterChip(
                        selected = activeFilter == InstallmentFilter.PAID,
                        onClick = { onSelectFilter(InstallmentFilter.PAID) },
                        label = { Text("${strings.paidLabel} ($paidCount)") }
                    )
                    FilterChip(
                        selected = activeFilter == InstallmentFilter.UNPAID,
                        onClick = { onSelectFilter(InstallmentFilter.UNPAID) },
                        label = { Text("${strings.unpaidLabel} ($unpaidCount)") }
                    )
                    FilterChip(
                        selected = activeFilter == InstallmentFilter.PARTIAL,
                        onClick = { onSelectFilter(InstallmentFilter.PARTIAL) },
                        label = { Text("${strings.partialLabel} ($partialCount)") }
                    )
                }
            }
        }

        if (visibleRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.noInstallmentsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onSyncMonth) {
                            Icon(Icons.Rounded.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.generateInstallments)
                        }
                    }
                }
            }
        } else {
            items(visibleRecords, key = { it.id }) { rec ->
                val member = memberMap[rec.memberId]
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("installment_card_${rec.id}"),
                    shape = RoundedCornerShape(16.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${rec.committeeNumber} • ${rec.memberName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rec.memberPhone + (rec.paymentDate?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${FormatUtils.formatPkr(rec.paidAmount)} / ${FormatUtils.formatPkr(rec.dueAmount)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when (rec.statusEnum) {
                                        InstallmentStatus.PAID -> StatusPaidGreen
                                        InstallmentStatus.PARTIAL -> StatusPendingYellow
                                        InstallmentStatus.UNPAID -> StatusUnpaidRed
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            InstallmentStatusBadge(rec.statusEnum, strings)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rec.statusEnum != InstallmentStatus.PAID) {
                                Button(
                                    onClick = { onQuickMarkPaid(rec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("mark_paid_btn_${rec.id}")
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.markPaid, maxLines = 1)
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${strings.paidLabel} • ${rec.paymentDate ?: MonthUtils.todayDateString()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusPaidGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onEditPayment(rec) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("edit_payment_btn_${rec.id}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (rec.paidAmount > 0L) strings.editLabel else strings.markPartial, maxLines = 1)
                            }

                            if (rec.paidAmount == 0L) {
                                IconButton(
                                    onClick = { onDeleteInstallment(rec) },
                                    modifier = Modifier.testTag("delete_installment_btn_${rec.id}")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.deleteLabel, tint = StatusUnpaidRed)
                                }
                            }

                            if (rec.statusEnum != InstallmentStatus.PAID && member != null) {
                                IconButton(
                                    onClick = { onQuickWhatsApp(member, rec.remainingAmount.coerceAtLeast(rec.dueAmount)) }
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = strings.openWhatsApp, tint = WhatsAppGreen)
                                }
                                IconButton(
                                    onClick = { onQuickSms(member, rec.remainingAmount.coerceAtLeast(rec.dueAmount)) }
                                ) {
                                    Icon(Icons.Rounded.Sms, contentDescription = strings.openSms, tint = SmsBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
