package com.example.ui.components

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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.MessagingGatewayConfig
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.LocalUser
import com.example.model.UploadQueueItem
import com.example.model.UploadQueueStatus
import com.example.model.UserRole
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDeleteConfirmDialog(
    strings: UiStrings,
    title: String,
    message: String,
    currentUser: LocalUser?,
    showDefaultPinHint: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmDelete: (adminPassword: String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    val isAdmin = currentUser?.isAdmin == true && currentUser.isActive

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = StatusUnpaidRed
            )
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)

                if (!isAdmin) {
                    Surface(
                        color = StatusUnpaidRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusUnpaidRed)
                            Text(
                                text = "Only authorized Admin users can delete records.",
                                color = StatusUnpaidRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text(strings.adminPinLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_delete_pin_input")
                    )
                    if (showDefaultPinHint) {
                        Text(
                            text = strings.defaultAdminPinHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = StatusUnpaidRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(pinInput) },
                enabled = isAdmin && pinInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaidRed),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(strings.deleteLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun MessagingGatewayConfigDialog(
    strings: UiStrings,
    currentConfig: MessagingGatewayConfig,
    onDismiss: () -> Unit,
    onSave: (waPhoneId: String, waToken: String, smsUrl: String, smsApiKey: String, smsSender: String) -> Unit
) {
    var waPhoneId by remember { mutableStateOf(currentConfig.whatsappPhoneNumberId) }
    var waToken by remember { mutableStateOf(currentConfig.whatsappAccessToken) }
    var smsUrl by remember { mutableStateOf(currentConfig.smsGatewayUrl) }
    var smsKey by remember { mutableStateOf(currentConfig.smsGatewayApiKey) }
    var smsSender by remember { mutableStateOf(currentConfig.smsSenderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.messagingApiSettingsTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Optional: Enter Meta WhatsApp Business Cloud API & HTTP SMS Gateway credentials. If left blank, the app uses Direct WhatsApp / SMS mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("WhatsApp Business Cloud API", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = waPhoneId,
                    onValueChange = { waPhoneId = it },
                    label = { Text("Phone Number ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waToken,
                    onValueChange = { waToken = it },
                    label = { Text("Permanent / Cloud Access Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("HTTP SMS Gateway", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = smsUrl,
                    onValueChange = { smsUrl = it },
                    label = { Text("Gateway POST URL (e.g. https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = smsKey,
                    onValueChange = { smsKey = it },
                    label = { Text("API Key / Bearer Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = smsSender,
                    onValueChange = { smsSender = it },
                    label = { Text("Sender ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(waPhoneId, waToken, smsUrl, smsKey, smsSender) }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    strings: UiStrings,
    currentUser: LocalUser?,
    showDefaultPinHint: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (currentPass: String, newPass: String, onError: (String) -> Unit) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        title = { Text(strings.changePasswordTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Account: ${currentUser?.displayName ?: "Admin"} (@${currentUser?.username ?: "admin"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showDefaultPinHint) {
                    Text(
                        text = strings.defaultAdminPinHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it; errorText = null },
                    label = { Text(strings.currentPasswordLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("current_password_input")
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; errorText = null },
                    label = { Text(strings.newPasswordLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; errorText = null },
                    label = { Text("${strings.confirm} ${strings.newPasswordLabel}") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(errorText ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPass.isEmpty() -> errorText = "Enter current password / PIN"
                        newPass.length < 4 -> errorText = "New password / PIN must be at least 4 characters"
                        newPass != confirmPass -> errorText = "Passwords do not match"
                        else -> onSubmit(currentPass, newPass) { msg -> errorText = msg }
                    }
                },
                modifier = Modifier.testTag("save_password_button")
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
fun UserManagementDialog(
    strings: UiStrings,
    users: List<LocalUser>,
    currentUser: LocalUser?,
    onDismiss: () -> Unit,
    onCreateUser: (username: String, displayName: String, password: String, role: UserRole, onError: (String) -> Unit) -> Unit,
    onUpdateUser: (LocalUser, UserRole, Boolean) -> Unit,
    onDeleteUser: (LocalUser) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
    var formError by remember { mutableStateOf<String?>(null) }

    val isCurrentAdmin = currentUser?.isAdmin == true

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        title = { Text(strings.userManagementTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isCurrentAdmin) {
                    Text(
                        text = "Signed in as Staff. Only Admin can add or delete users.",
                        color = StatusPendingYellow,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                users.forEach { user ->
                    val isSelf = user.id == currentUser?.id
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName + if (isSelf) " (You)" else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${user.username} • ${user.role}" + if (!user.isActive) " • Disabled" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (user.isAdmin) StatusPaidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrentAdmin && !isSelf) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = user.isActive,
                                            onCheckedChange = { active ->
                                                onUpdateUser(user, user.roleEnum, active)
                                            }
                                        )
                                        IconButton(onClick = { onDeleteUser(user) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = strings.deleteLabel, tint = StatusUnpaidRed)
                                        }
                                    }
                                }
                            }
                            if (isCurrentAdmin && !isSelf) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = user.roleEnum == UserRole.ADMIN,
                                        onClick = { onUpdateUser(user, UserRole.ADMIN, user.isActive) },
                                        label = { Text("ADMIN") }
                                    )
                                    FilterChip(
                                        selected = user.roleEnum == UserRole.STAFF,
                                        onClick = { onUpdateUser(user, UserRole.STAFF, user.isActive) },
                                        label = { Text("STAFF") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isCurrentAdmin) {
                    HorizontalDivider()
                    if (!showAddForm) {
                        OutlinedButton(
                            onClick = { showAddForm = true },
                            modifier = Modifier.fillMaxWidth().testTag("show_add_user_form_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addUserLabel)
                        }
                    } else {
                        Text(strings.addUserLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it; formError = null },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; formError = null },
                            label = { Text(strings.usernameLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; formError = null },
                            label = { Text(strings.passwordPinLabel) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedRole == UserRole.ADMIN,
                                onClick = { selectedRole = UserRole.ADMIN },
                                label = { Text("ADMIN (Can Delete)") }
                            )
                            FilterChip(
                                selected = selectedRole == UserRole.STAFF,
                                onClick = { selectedRole = UserRole.STAFF },
                                label = { Text("STAFF") }
                            )
                        }
                        if (formError != null) {
                            Text(formError ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onCreateUser(username, displayName, password, selectedRole) { err ->
                                        formError = err
                                    }
                                    if (username.trim().length >= 3 && displayName.trim().isNotEmpty() && password.length >= 4) {
                                        username = ""
                                        displayName = ""
                                        password = ""
                                        showAddForm = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.save)
                            }
                            TextButton(onClick = { showAddForm = false }) {
                                Text(strings.cancel)
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

@Composable
fun UploadQueueDialog(
    strings: UiStrings,
    title: String,
    items: List<UploadQueueItem>,
    filterStatus: UploadQueueStatus?,
    onRetryAll: () -> Unit,
    onExportAndCompleteLocal: () -> Unit,
    onClearCompleted: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (filterStatus) {
                    UploadQueueStatus.COMPLETED -> Icons.Rounded.CloudDone
                    UploadQueueStatus.FAILED -> Icons.Rounded.ErrorOutline
                    else -> Icons.Rounded.Sync
                },
                contentDescription = null,
                tint = when (filterStatus) {
                    UploadQueueStatus.COMPLETED -> StatusPaidGreen
                    UploadQueueStatus.FAILED -> StatusUnpaidRed
                    else -> StatusPendingYellow
                }
            )
        },
        title = { Text("$title (${items.size})", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "No items in this upload list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    items.forEach { item ->
                        val color = when (item.statusEnum) {
                            UploadQueueStatus.COMPLETED -> StatusPaidGreen
                            UploadQueueStatus.FAILED -> StatusUnpaidRed
                            UploadQueueStatus.PENDING -> StatusPendingYellow
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${item.status} • Attempts: ${item.attempts} • ${timeFmt.format(Date(item.updatedAtMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!item.errorMessage.isNullOrBlank()) {
                                    Text(
                                        text = item.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusUnpaidRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filterStatus != UploadQueueStatus.COMPLETED && items.isNotEmpty()) {
                    Button(onClick = onRetryAll) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.retryUploadTitle)
                    }
                    OutlinedButton(onClick = onExportAndCompleteLocal) {
                        Text(strings.completeLocalSyncLabel, maxLines = 1)
                    }
                } else if (filterStatus == UploadQueueStatus.COMPLETED && items.isNotEmpty()) {
                    OutlinedButton(onClick = onClearCompleted) {
                        Text("Clear")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
fun AppSettingsDialog(
    strings: UiStrings,
    language: AppLanguage,
    gatewayConfig: MessagingGatewayConfig,
    onLanguageChange: (AppLanguage) -> Unit,
    onSaveSyncUrl: (String) -> Unit,
    onOpenMessagingGateway: () -> Unit,
    onDismiss: () -> Unit
) {
    var syncUrl by remember { mutableStateOf(gatewayConfig.syncWebhookUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
        title = { Text(strings.appSettingsTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Language / زبان", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.label) }
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = syncUrl,
                    onValueChange = { syncUrl = it },
                    label = { Text(strings.syncServerUrlLabel) },
                    placeholder = { Text("https://example.com/committee-sync") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sync_url_input")
                )
                Text(
                    text = "Leave blank for pure offline mode. Enter a webhook URL to auto-sync pending records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onOpenMessagingGateway,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.messagingApiSettingsTitle)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSyncUrl(syncUrl)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_app_settings_btn")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
