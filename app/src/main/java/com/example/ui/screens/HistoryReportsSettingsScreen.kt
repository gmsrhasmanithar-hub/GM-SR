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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PdfReportType
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.LocalUser
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun HistoryReportsSettingsScreen(
    strings: UiStrings,
    activeCommittee: Committee?,
    members: List<CommitteeMember>,
    currentUser: LocalUser?,
    pendingUploadCount: Int,
    completedUploadCount: Int,
    failedUploadCount: Int,
    onViewPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onPrintPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onDownloadPdf: (PdfReportType, CommitteeMember?) -> Unit,
    onSharePdf: (PdfReportType, CommitteeMember?) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenUploadPending: () -> Unit,
    onOpenUploadCompleted: () -> Unit,
    onOpenFailedUploads: () -> Unit,
    onRetryUpload: () -> Unit,
    onExportBackupJson: () -> Unit,
    onRestoreBackupJson: () -> Unit,
    onOpenUserManagement: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenAuditLog: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedReportType by remember { mutableStateOf(PdfReportType.COMPLETE_SUMMARY) }
    var selectedMemberForReport by remember(members) { mutableStateOf(members.firstOrNull()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Reports (View / Print / Download / Share)
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
                        Text(
                            text = strings.noCommitteeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val reports = listOf(
                            PdfReportType.COMPLETE_SUMMARY to strings.reportCompleteSummary,
                            PdfReportType.COLLECTION_REPORT to strings.reportCollection,
                            PdfReportType.MONTHLY_PAYMENT to strings.reportMonthlyPayment,
                            PdfReportType.PAID_MEMBERS_REPORT to strings.reportPaidMembers,
                            PdfReportType.UNPAID_MEMBERS_REPORT to strings.reportUnpaidMembers,
                            PdfReportType.MEMBER_LIST to strings.reportMemberList,
                            PdfReportType.PAYOUT_REPORT to strings.reportPayout,
                            PdfReportType.INDIVIDUAL_HISTORY to strings.reportIndividualHistory
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reports.forEach { (type, title) ->
                                FilterChip(
                                    selected = selectedReportType == type,
                                    onClick = { selectedReportType = type },
                                    label = { Text(title) }
                                )
                            }
                        }

                        if (selectedReportType == PdfReportType.INDIVIDUAL_HISTORY && members.isNotEmpty()) {
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
                        }

                        val targetMember = if (selectedReportType == PdfReportType.INDIVIDUAL_HISTORY) selectedMemberForReport else null
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onViewPdf(selectedReportType, targetMember) },
                                modifier = Modifier.weight(1f).testTag("report_view_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.viewLabel)
                            }
                            Button(
                                onClick = { onPrintPdf(selectedReportType, targetMember) },
                                modifier = Modifier.weight(1f).testTag("report_print_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.printLabel)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onDownloadPdf(selectedReportType, targetMember) },
                                modifier = Modifier.weight(1f).testTag("report_download_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.downloadLabel)
                            }
                            OutlinedButton(
                                onClick = { onSharePdf(selectedReportType, targetMember) },
                                modifier = Modifier.weight(1f).testTag("report_share_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Simple Settings (Exact 9 requested settings + Backup & Restore buttons + Audit Log)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.settingsSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        currentUser?.let { u ->
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${u.displayName} (${u.role})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 1. App Settings
                    SimpleSettingsItemRow(
                        title = strings.appSettingsTitle,
                        icon = Icons.Rounded.Settings,
                        accent = EmeraldPrimary,
                        onClick = onOpenAppSettings,
                        testTag = "setting_app_settings"
                    )

                    // 2. Upload Pending
                    SimpleSettingsItemRow(
                        title = strings.uploadPendingTitle,
                        badge = "$pendingUploadCount",
                        icon = Icons.Rounded.CloudQueue,
                        accent = StatusPendingYellow,
                        onClick = onOpenUploadPending,
                        testTag = "setting_upload_pending"
                    )

                    // 3. Upload Completed
                    SimpleSettingsItemRow(
                        title = strings.uploadCompletedTitle,
                        badge = "$completedUploadCount",
                        icon = Icons.Rounded.CloudDone,
                        accent = StatusPaidGreen,
                        onClick = onOpenUploadCompleted,
                        testTag = "setting_upload_completed"
                    )

                    // 4. Failed Uploads
                    SimpleSettingsItemRow(
                        title = strings.failedUploadsTitle,
                        badge = "$failedUploadCount",
                        icon = Icons.Rounded.ErrorOutline,
                        accent = StatusUnpaidRed,
                        onClick = onOpenFailedUploads,
                        testTag = "setting_failed_uploads"
                    )

                    // 5. Retry Upload
                    SimpleSettingsItemRow(
                        title = strings.retryUploadTitle,
                        badge = "${pendingUploadCount + failedUploadCount}",
                        icon = Icons.Rounded.Refresh,
                        accent = EmeraldPrimary,
                        onClick = onRetryUpload,
                        testTag = "setting_retry_upload"
                    )

                    // 6. Backup and Restore
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(strings.backupRestoreTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                        }
                    }

                    // 7. User Management
                    SimpleSettingsItemRow(
                        title = strings.userManagementTitle,
                        icon = Icons.Rounded.Group,
                        accent = EmeraldPrimary,
                        onClick = onOpenUserManagement,
                        testTag = "setting_user_management"
                    )

                    // 8. Change Password
                    SimpleSettingsItemRow(
                        title = strings.changePasswordTitle,
                        icon = Icons.Rounded.Lock,
                        accent = EmeraldPrimary,
                        onClick = onOpenChangePassword,
                        testTag = "setting_change_password"
                    )

                    // Audit Log quick entry
                    SimpleSettingsItemRow(
                        title = strings.auditLogTitle,
                        icon = Icons.Rounded.History,
                        accent = EmeraldPrimary,
                        onClick = onOpenAuditLog,
                        testTag = "setting_audit_log"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // 9. Logout
                    SimpleSettingsItemRow(
                        title = strings.logoutTitle,
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        accent = StatusUnpaidRed,
                        onClick = onLogout,
                        testTag = "setting_logout"
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleSettingsItemRow(
    title: String,
    badge: String? = null,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (badge != null) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
