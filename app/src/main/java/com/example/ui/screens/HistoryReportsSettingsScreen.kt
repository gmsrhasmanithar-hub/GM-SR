package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MessagingGatewayConfig
import com.example.data.PdfReportType
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.FormatUtils
import com.example.ui.components.CommitteeStatusBadge

@Composable
fun HistoryReportsSettingsScreen(
    strings: UiStrings,
    language: AppLanguage,
    adminEmail: String,
    committees: List<Committee>,
    activeCommittee: Committee?,
    members: List<CommitteeMember>,
    gatewayConfig: MessagingGatewayConfig,
    onLanguageChange: (AppLanguage) -> Unit,
    onSelectCommittee: (Committee) -> Unit,
    onCreateCommittee: () -> Unit,
    onSetCommitteeStatus: (Committee, CommitteeStatus) -> Unit,
    onExportPdf: (PdfReportType, CommitteeMember?, Boolean) -> Unit,
    onExportBackupJson: () -> Unit,
    onRestoreBackupJson: () -> Unit,
    onOpenGatewayConfig: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedMemberForReport by remember(members) {
        mutableStateOf(members.firstOrNull())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: GM HASMANI PDF Reports
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.pdfReportsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (activeCommittee == null) {
                        Text(strings.noCommitteeTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val reports = listOf(
                            PdfReportType.COLLECTION_REPORT to strings.reportCollection,
                            PdfReportType.MEMBER_LIST to strings.reportMemberList,
                            PdfReportType.MONTHLY_PAYMENT to strings.reportMonthlyPayment,
                            PdfReportType.PAID_MEMBERS_REPORT to strings.reportPaidMembers,
                            PdfReportType.UNPAID_MEMBERS_REPORT to strings.reportUnpaidMembers,
                            PdfReportType.PAYOUT_REPORT to strings.reportPayout,
                            PdfReportType.COMPLETE_SUMMARY to strings.reportCompleteSummary
                        )
                        reports.forEach { (type, title) ->
                            PdfReportActionRow(
                                title = title,
                                onView = { onExportPdf(type, null, false) },
                                onShare = { onExportPdf(type, null, true) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(strings.reportIndividualHistory, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (members.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                members.sortedBy { it.committeeNumber }.forEach { m ->
                                    FilterChip(
                                        selected = selectedMemberForReport?.id == m.id,
                                        onClick = { selectedMemberForReport = m },
                                        label = { Text("#${m.committeeNumber} ${m.name}") }
                                    )
                                }
                            }
                            PdfReportActionRow(
                                title = "${strings.reportIndividualHistory}: ${selectedMemberForReport?.name ?: ""}",
                                onView = { onExportPdf(PdfReportType.INDIVIDUAL_HISTORY, selectedMemberForReport, false) },
                                onShare = { onExportPdf(PdfReportType.INDIVIDUAL_HISTORY, selectedMemberForReport, true) }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Committee History & Archive
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.historyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onCreateCommittee,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("history_new_committee_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.createNewCommittee)
                }
            }
        }

        if (committees.isEmpty()) {
            item {
                Text(strings.noHistoryYet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(committees, key = { it.id }) { itemCommittee ->
                val isSelected = itemCommittee.id == activeCommittee?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCommittee(itemCommittee) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surface
                    )
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
                                Text(itemCommittee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${itemCommittee.memberCount} Members • ${FormatUtils.formatPkr(itemCommittee.monthlyInstallment)}/mo • ${itemCommittee.startMonth} → ${itemCommittee.endMonth}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CommitteeStatusBadge(itemCommittee.statusEnum, strings)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (itemCommittee.statusEnum == CommitteeStatus.ACTIVE) {
                                OutlinedButton(
                                    onClick = { onSetCommitteeStatus(itemCommittee, CommitteeStatus.COMPLETED) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.completeCommittee)
                                }
                            }
                            if (itemCommittee.statusEnum != CommitteeStatus.ARCHIVED) {
                                OutlinedButton(
                                    onClick = { onSetCommitteeStatus(itemCommittee, CommitteeStatus.ARCHIVED) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.archiveCommittee)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Database Backup & Restore + Gateway + Language + Account
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.backupRestoreTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportBackupJson,
                            modifier = Modifier.weight(1f).testTag("export_backup_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportBackupJson, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onRestoreBackupJson,
                            modifier = Modifier.weight(1f).testTag("restore_backup_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.restoreBackupJson, maxLines = 1)
                        }
                    }

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = onOpenGatewayConfig,
                        modifier = Modifier.fillMaxWidth().testTag("open_gateway_config_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${strings.messagingApiSettingsTitle} (" +
                                (if (gatewayConfig.isWhatsAppApiConfigured || gatewayConfig.isSmsGatewayConfigured) "Configured" else "Optional") + ")"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null)
                            Text("Language / زبان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang.label) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Admin Account", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(adminEmail.ifBlank { "Signed in with Google" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.signOut)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReportActionRow(
    title: String,
    onView: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = "Share $title", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
