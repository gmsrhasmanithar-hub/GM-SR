package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.example.data.PdfReportType
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember

private data class PdfReportCardMeta(
    val type: PdfReportType,
    val title: String,
    val subtitle: String,
    val requiresCommittee: Boolean = true
)

@Composable
fun PdfReportsScreen(
    strings: UiStrings,
    companySettings: AppCompanySettings,
    activeCommittee: Committee?,
    members: List<CommitteeMember>,
    onViewPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onPrintPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onDownloadPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onSharePdf: (PdfReportType, CommitteeMember?) -> Unit
) {
    var selectedMember by remember(members) { mutableStateOf(members.firstOrNull()) }

    val reports = remember(strings) {
        listOf(
            PdfReportCardMeta(
                PdfReportType.INDIVIDUAL_MEMBER,
                strings.reportIndividualHistory,
                "Profile, shares, payments, methods, and turns for selected member"
            ),
            PdfReportCardMeta(
                PdfReportType.COMPLETE_MEMBER_LIST,
                strings.reportMemberList,
                "All active members, phone numbers, shares, and monthly contributions"
            ),
            PdfReportCardMeta(
                PdfReportType.MONTHLY_COLLECTION,
                strings.reportCollection,
                "Current month 28th–8th collection ledger with methods and balances"
            ),
            PdfReportCardMeta(
                PdfReportType.PENDING_PAYMENTS,
                strings.reportUnpaidMembers,
                "Unpaid and partial installment balances for follow-up"
            ),
            PdfReportCardMeta(
                PdfReportType.COMPLETED_PAYMENTS,
                strings.reportPaidMembers,
                "Verified paid installments with payment dates, methods, and Tx IDs"
            ),
            PdfReportCardMeta(
                PdfReportType.INDIVIDUAL_COMMITTEE,
                strings.reportIndividualCommittee,
                "Complete configuration, members, shares, turns, and totals for active committee"
            ),
            PdfReportCardMeta(
                PdfReportType.COMMITTEE_PAYOUT_REPORT,
                strings.reportCommitteePayouts,
                "Full payout turn order, recipient names, scheduled windows, and status"
            ),
            PdfReportCardMeta(
                PdfReportType.PREVIOUS_COMMITTEE_HISTORY,
                strings.reportPreviousHistory,
                "Separate Old Committee Records / History archive statement",
                requiresCommittee = false
            ),
            PdfReportCardMeta(
                PdfReportType.COMPLETE_FINANCIAL_SUMMARY,
                strings.reportCompleteSummary,
                "Cross-committee grand totals, collected pools, payouts, and pending balance",
                requiresCommittee = false
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.pdfReportsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${companySettings.companyName} • ${activeCommittee?.name ?: "No active committee"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (members.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.selectMemberForReport, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.sortedBy { it.committeeNumber }.forEach { m ->
                            FilterChip(
                                selected = selectedMember?.id == m.id,
                                onClick = { selectedMember = m },
                                label = { Text("#${m.committeeNumber} ${m.name}") }
                            )
                        }
                    }
                }
            }
        }

        items(reports, key = { it.type.name }) { report ->
            val enabled = !report.requiresCommittee || activeCommittee != null
            val targetMember = if (report.type == PdfReportType.INDIVIDUAL_MEMBER) selectedMember else null
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pdf_report_card_${report.type.name}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(report.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = report.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onViewPdf(report.type, targetMember) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.viewLabel, maxLines = 1)
                        }
                        Button(
                            onClick = { onPrintPdf(report.type, targetMember) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.printLabel, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDownloadPdf(report.type, targetMember) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.downloadLabel, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onSharePdf(report.type, targetMember) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
