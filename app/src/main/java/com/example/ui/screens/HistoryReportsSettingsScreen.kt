package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.LocalUser
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun HistoryReportsSettingsScreen(
    strings: UiStrings,
    companySettings: AppCompanySettings,
    language: AppLanguage,
    currentUser: LocalUser?,
    pendingUploadCount: Int,
    completedUploadCount: Int,
    failedUploadCount: Int,
    onSaveCompanySettings: (AppCompanySettings) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
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
    var companyName by remember(companySettings) { mutableStateOf(companySettings.companyName) }
    var adminName by remember(companySettings) { mutableStateOf(companySettings.adminName) }
    var adminPhone by remember(companySettings) { mutableStateOf(companySettings.adminContactNumber) }
    var pdfFooter by remember(companySettings) { mutableStateOf(companySettings.pdfFooterNote) }

    var easypaisaNumber by remember(companySettings) { mutableStateOf(companySettings.easypaisaNumber) }
    var easypaisaHolder by remember(companySettings) { mutableStateOf(companySettings.easypaisaAccountHolder) }

    var currency by remember(companySettings) { mutableStateOf(companySettings.currency) }
    var defMonthlyContrib by remember(companySettings) { mutableStateOf(companySettings.defaultMonthlyContribution.toString()) }
    var defTotalShares by remember(companySettings) { mutableStateOf(companySettings.defaultTotalShares.toString()) }
    var defDuration by remember(companySettings) { mutableStateOf(companySettings.defaultDurationMonths.toString()) }
    var defStartDay by remember(companySettings) { mutableStateOf(companySettings.defaultCollectionStartDay.toString()) }
    var defEndDay by remember(companySettings) { mutableStateOf(companySettings.defaultCollectionEndDay.toString()) }

    fun persistAllSettings() {
        onSaveCompanySettings(
            companySettings.copy(
                companyName = companyName.trim().ifEmpty { "GM HASMANI COMMITTEE" },
                adminName = adminName.trim().ifEmpty { "GM Hasmani Admin" },
                adminContactNumber = adminPhone.trim(),
                pdfFooterNote = pdfFooter.trim().ifEmpty { "Official GM HASMANI Committee Record" },
                easypaisaNumber = easypaisaNumber.trim().ifEmpty { "03423498631" },
                easypaisaAccountHolder = easypaisaHolder.trim().ifEmpty { "GM Hasmani Admin" },
                currency = currency.trim().ifEmpty { "PKR" },
                defaultMonthlyContribution = defMonthlyContrib.toLongOrNull()?.coerceAtLeast(100L) ?: 5000L,
                defaultTotalShares = defTotalShares.toLongOrNull()?.coerceAtLeast(1L) ?: 10L,
                defaultDurationMonths = defDuration.toLongOrNull()?.coerceAtLeast(2L) ?: 10L,
                defaultCollectionStartDay = defStartDay.toLongOrNull()?.coerceIn(1L, 31L) ?: 28L,
                defaultCollectionEndDay = defEndDay.toLongOrNull()?.coerceIn(1L, 31L) ?: 8L
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Company Settings
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Business, contentDescription = null, tint = EmeraldPrimary)
                        Text(strings.companySettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company / PDF Heading") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_company_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Admin Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = adminPhone,
                            onValueChange = { adminPhone = it },
                            label = { Text("Admin Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = pdfFooter,
                        onValueChange = { pdfFooter = it },
                        label = { Text("PDF Footer Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { persistAllSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // 2. Easypaisa Settings
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = StatusPaidGreen)
                        Text(strings.easypaisaSettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = easypaisaNumber,
                        onValueChange = { easypaisaNumber = it },
                        label = { Text("Easypaisa Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_easypaisa_number_input")
                    )
                    OutlinedTextField(
                        value = easypaisaHolder,
                        onValueChange = { easypaisaHolder = it },
                        label = { Text("Account Holder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_easypaisa_holder_input")
                    )
                    Button(
                        onClick = { persistAllSettings() },
                        modifier = Modifier.fillMaxWidth().testTag("save_easypaisa_settings_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // 3. Committee Default Settings
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = EmeraldPrimary)
                        Text(strings.committeeDefaultsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = defMonthlyContrib,
                            onValueChange = { defMonthlyContrib = it.filter { c -> c.isDigit() } },
                            label = { Text("Default Share Amt") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defTotalShares,
                            onValueChange = { defTotalShares = it.filter { c -> c.isDigit() } },
                            label = { Text("Default Shares") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = defDuration,
                            onValueChange = { defDuration = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (Mo)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defStartDay,
                            onValueChange = { defStartDay = it.filter { c -> c.isDigit() } },
                            label = { Text("Start Day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defEndDay,
                            onValueChange = { defEndDay = it.filter { c -> c.isDigit() } },
                            label = { Text("End Day") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = { persistAllSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // 4. Application Settings + Backup + Users + Security
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
                        Text(strings.appSettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SimpleSettingsItemRow(
                        title = strings.messagingApiSettingsTitle,
                        icon = Icons.Rounded.Settings,
                        accent = EmeraldPrimary,
                        onClick = onOpenAppSettings,
                        testTag = "setting_app_settings"
                    )

                    SimpleSettingsItemRow(
                        title = strings.uploadPendingTitle,
                        badge = "$pendingUploadCount",
                        icon = Icons.Rounded.CloudQueue,
                        accent = StatusPendingYellow,
                        onClick = onOpenUploadPending,
                        testTag = "setting_upload_pending"
                    )

                    SimpleSettingsItemRow(
                        title = strings.uploadCompletedTitle,
                        badge = "$completedUploadCount",
                        icon = Icons.Rounded.CloudDone,
                        accent = StatusPaidGreen,
                        onClick = onOpenUploadCompleted,
                        testTag = "setting_upload_completed"
                    )

                    SimpleSettingsItemRow(
                        title = strings.failedUploadsTitle,
                        badge = "$failedUploadCount",
                        icon = Icons.Rounded.ErrorOutline,
                        accent = StatusUnpaidRed,
                        onClick = onOpenFailedUploads,
                        testTag = "setting_failed_uploads"
                    )

                    SimpleSettingsItemRow(
                        title = strings.retryUploadTitle,
                        badge = "${pendingUploadCount + failedUploadCount}",
                        icon = Icons.Rounded.Refresh,
                        accent = EmeraldPrimary,
                        onClick = onRetryUpload,
                        testTag = "setting_retry_upload"
                    )

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

                    SimpleSettingsItemRow(
                        title = strings.userManagementTitle,
                        icon = Icons.Rounded.Group,
                        accent = EmeraldPrimary,
                        onClick = onOpenUserManagement,
                        testTag = "setting_user_management"
                    )

                    SimpleSettingsItemRow(
                        title = strings.changePasswordTitle,
                        icon = Icons.Rounded.Lock,
                        accent = EmeraldPrimary,
                        onClick = onOpenChangePassword,
                        testTag = "setting_change_password"
                    )

                    SimpleSettingsItemRow(
                        title = strings.auditLogTitle,
                        icon = Icons.Rounded.History,
                        accent = EmeraldPrimary,
                        onClick = onOpenAuditLog,
                        testTag = "setting_audit_log"
                    )

                    Text(
                        text = "${companySettings.companyName} • Offline Room DB v3 (Preserving All User Data)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

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
