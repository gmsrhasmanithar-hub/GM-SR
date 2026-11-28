package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.FormatUtils
import com.example.model.ForwardFinancialSummary
import com.example.model.ForwardOrganizer
import com.example.model.ForwardPaymentRecord
import com.example.model.MonthUtils
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.ProofFileHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun ForwardPaymentsScreen(
    strings: UiStrings,
    companySettings: AppCompanySettings,
    committee: Committee?,
    selectedMonthKey: String,
    monthlySummary: ForwardFinancialSummary?,
    overallSummary: ForwardFinancialSummary?,
    organizers: List<ForwardOrganizer>,
    forwardPayments: List<ForwardPaymentRecord>,
    onSelectMonth: (String) -> Unit,
    onCreateCommittee: () -> Unit,
    onAddOrganizer: () -> Unit,
    onEditOrganizer: (ForwardOrganizer) -> Unit,
    onDeleteOrganizer: (ForwardOrganizer) -> Unit,
    onRecordForwardPayment: (organizerId: String?) -> Unit,
    onEditForwardPayment: (ForwardPaymentRecord) -> Unit,
    onReverseForwardPayment: (ForwardPaymentRecord) -> Unit,
    onDeleteForwardPayment: (ForwardPaymentRecord) -> Unit,
    onOpenUrduSummary: (organizer: ForwardOrganizer?, isMonthlyScope: Boolean) -> Unit,
    onExportForwardReportPdf: () -> Unit,
    onShowProofMissingSnackbar: () -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    val context = LocalContext.current
    val currency = companySettings.currency
    var useMonthlyScope by remember { mutableStateOf(true) }
    val activeSummary = if (useMonthlyScope) monthlySummary else overallSummary
    val scopedHistory = remember(forwardPayments, selectedMonthKey, useMonthlyScope) {
        if (useMonthlyScope) forwardPayments.filter { it.monthKey == selectedMonthKey } else forwardPayments
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Scope Switch
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ForwardToInbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.forwardPaymentsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${companySettings.companyName} • ${committee.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = strings.forwardPaymentsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useMonthlyScope,
                            onClick = { useMonthlyScope = true },
                            label = { Text("${strings.selectedMonthScopeLabel} (${MonthUtils.formatMonthDisplay(selectedMonthKey)})") },
                            modifier = Modifier.testTag("forward_scope_monthly_chip")
                        )
                        FilterChip(
                            selected = !useMonthlyScope,
                            onClick = { useMonthlyScope = false },
                            label = { Text(strings.overallCommitteeScopeLabel) },
                            modifier = Modifier.testTag("forward_scope_overall_chip")
                        )
                    }
                    if (useMonthlyScope) {
                        MonthSelectorBar(
                            committee = committee,
                            selectedMonthKey = selectedMonthKey,
                            strings = strings,
                            onSelectMonth = onSelectMonth
                        )
                    }
                }
            }
        }

        // Automatic Financial Summary Card (All 6+ values, zero double-counting)
        if (activeSummary != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("forward_financial_summary_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Automatic Financial Summary (${activeSummary.scopeLabel})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryStatCell(
                                label = strings.expectedFromMembersLabel,
                                value = FormatUtils.formatMoney(activeSummary.expectedFromMembers, currency),
                                tint = EmeraldPrimary,
                                bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatCell(
                                label = strings.receivedFromMembersLabel,
                                value = FormatUtils.formatMoney(activeSummary.collectedFromMembers, currency),
                                tint = StatusPaidGreen,
                                bg = StatusPaidBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryStatCell(
                                label = strings.notYetReceivedLabel,
                                value = FormatUtils.formatMoney(activeSummary.pendingMemberCollection, currency),
                                tint = StatusUnpaidRed,
                                bg = StatusUnpaidBg,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatCell(
                                label = strings.totalPayableLabel,
                                value = FormatUtils.formatMoney(activeSummary.forwardPaymentDue, currency),
                                tint = GoldAccent,
                                bg = StatusPendingBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryStatCell(
                                label = strings.forwardedAmountLabel,
                                value = FormatUtils.formatMoney(activeSummary.totalForwarded, currency),
                                tint = StatusPaidGreen,
                                bg = StatusPaidBg,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatCell(
                                label = strings.pendingForwardLabel,
                                value = FormatUtils.formatMoney(activeSummary.pendingForwardPayment, currency),
                                tint = StatusPendingYellow,
                                bg = StatusPendingBg,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Highlight Cash Currently Held by Admin
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = EmeraldPrimary)
                                    Column {
                                        Text(
                                            text = strings.heldByAdminLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Received (${FormatUtils.formatCompactNumber(activeSummary.collectedFromMembers)}) − Forwarded (${FormatUtils.formatCompactNumber(activeSummary.totalForwarded)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = FormatUtils.formatMoney(activeSummary.amountCurrentlyHeldByAdmin, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (activeSummary.amountCurrentlyHeldByAdmin < 0L) StatusUnpaidRed else EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddOrganizer,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("add_organizer_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addOrganizerLabel.removePrefix("+ ").trim(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { onRecordForwardPayment(null) },
                        enabled = organizers.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("record_forward_payment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.recordForwardPaymentLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onOpenUrduSummary(organizers.firstOrNull(), useMonthlyScope) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("send_organizer_urdu_summary_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.sendOrganizerUrduSummaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onExportForwardReportPdf,
                        modifier = Modifier.weight(0.75f).height(48.dp).testTag("export_forward_pdf_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", maxLines = 1)
                    }
                }
            }
        }

        // Organizers / Recipients List
        item {
            Text(
                text = "Committee Organizers / Recipients (${activeSummary?.organizerBreakdowns?.size ?: organizers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val breakdowns = activeSummary?.organizerBreakdowns.orEmpty()
        if (breakdowns.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No committee organizer added for this scope yet. Tap Add Organizer to record who receives forwarded funds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(breakdowns, key = { it.organizer.id }) { item ->
                val org = item.organizer
                val ratio = if (org.totalPayableAmount > 0L) {
                    (item.forwardedAmount.toFloat() / org.totalPayableAmount.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("organizer_card_${org.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(org.organizerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${org.organizerPhone} • ${if (org.monthKey.isBlank()) "All Months" else MonthUtils.formatMonthDisplay(org.monthKey)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onEditOrganizer(org) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel)
                                }
                                IconButton(onClick = { onDeleteOrganizer(org) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.deleteLabel, tint = StatusUnpaidRed)
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = StatusPaidGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Payable: ${FormatUtils.formatMoney(org.totalPayableAmount, currency)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Forwarded: ${FormatUtils.formatMoney(item.forwardedAmount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusPaidGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rem: ${FormatUtils.formatMoney(item.remainingAmount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.remainingAmount > 0L) StatusUnpaidRed else StatusPaidGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onRecordForwardPayment(org.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pay / Forward", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onOpenUrduSummary(org, useMonthlyScope) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Urdu Summary", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Forward Payment History Section
        item {
            Text(
                text = "${strings.forwardHistoryTitle} (${scopedHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (scopedHistory.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No forward payment transfers recorded for this period yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(scopedHistory, key = { it.id }) { payment ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (payment.isReversed) StatusUnpaidBg.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("forward_history_card_${payment.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${payment.organizerName} • ${FormatUtils.formatMoney(payment.amountForwarded, currency)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${payment.receiptNumber ?: "-"} • ${payment.paymentDate} • ${payment.paymentMethodEnum.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (payment.isReversed) StatusUnpaidBg else StatusPaidBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (payment.isReversed) "REVERSED" else "CONFIRMED",
                                    color = if (payment.isReversed) StatusUnpaidRed else StatusPaidGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (!payment.transactionId.isNullOrBlank()) {
                            Text("Ref / Tx ID: ${payment.transactionId}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!payment.notes.isNullOrBlank()) {
                            Text("Note: ${payment.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (payment.isReversed && !payment.reversedReason.isNullOrBlank()) {
                            Text(
                                text = "Reversal Reason: ${payment.reversedReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusUnpaidRed
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!payment.proofUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        if (!ProofFileHelper.openProofFile(context, payment.proofUri)) {
                                            onShowProofMissingSnackbar()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.viewProofLabel, maxLines = 1)
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = strings.noProofAttachedLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (!payment.isReversed) {
                                IconButton(onClick = { onEditForwardPayment(payment) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel)
                                }
                                IconButton(onClick = { onReverseForwardPayment(payment) }) {
                                    Icon(Icons.Rounded.Undo, contentDescription = strings.reversePaymentLabel, tint = StatusUnpaidRed)
                                }
                            } else {
                                IconButton(onClick = { onDeleteForwardPayment(payment) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.deleteLabel, tint = StatusUnpaidRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCell(
    label: String,
    value: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
