package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.i18n.UiStrings
import com.example.model.FormatUtils
import com.example.model.ForwardOrganizer
import com.example.model.ForwardPaymentRecord
import com.example.model.MonthUtils
import com.example.model.PaymentMethod
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidRed
import java.io.File

object ProofFileHelper {
    fun openProofFile(context: Context, proofPathOrUri: String): Boolean {
        return try {
            val file = File(proofPathOrUri)
            val uri: Uri = if (file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.parse(proofPathOrUri)
            }
            val mime = when {
                proofPathOrUri.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                else -> "image/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun ForwardOrganizerEditorDialog(
    strings: UiStrings,
    existingOrganizer: ForwardOrganizer?,
    defaultMonthKey: String,
    defaultPayableAmount: Long,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        totalPayable: Long,
        monthKey: String,
        notes: String?
    ) -> Unit
) {
    var name by remember(existingOrganizer) { mutableStateOf(existingOrganizer?.organizerName.orEmpty()) }
    var phone by remember(existingOrganizer) { mutableStateOf(existingOrganizer?.organizerPhone.orEmpty()) }
    var payableText by remember(existingOrganizer) {
        mutableStateOf(
            existingOrganizer?.totalPayableAmount?.toString()
                ?: defaultPayableAmount.takeIf { it > 0L }?.toString().orEmpty()
        )
    }
    var applyToAllMonths by remember(existingOrganizer) {
        mutableStateOf(existingOrganizer != null && existingOrganizer.monthKey.isBlank())
    }
    var monthKey by remember(existingOrganizer, defaultMonthKey) {
        mutableStateOf(existingOrganizer?.monthKey?.takeIf { it.isNotBlank() } ?: defaultMonthKey)
    }
    var notes by remember(existingOrganizer) { mutableStateOf(existingOrganizer?.notes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingOrganizer == null) strings.addOrganizerLabel.removePrefix("+ ").trim() else strings.editOrganizerLabel,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(strings.organizerNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("organizer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text(strings.organizerMobileLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("organizer_phone_input")
                )
                OutlinedTextField(
                    value = payableText,
                    onValueChange = { ch -> payableText = ch.filter { it.isDigit() }; errorMsg = null },
                    label = { Text(strings.totalPayableLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("organizer_payable_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = applyToAllMonths,
                        onCheckedChange = { applyToAllMonths = it }
                    )
                    Text("Across all committee months (not single month)", style = MaterialTheme.typography.bodySmall)
                }
                if (!applyToAllMonths) {
                    OutlinedTextField(
                        value = monthKey,
                        onValueChange = { monthKey = it },
                        label = { Text("Month (YYYY-MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.paymentNote) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanPhone = phone.trim()
                    val amt = payableText.toLongOrNull()
                    when {
                        cleanName.isBlank() -> errorMsg = "Please enter organizer/recipient name."
                        cleanPhone.isBlank() -> errorMsg = "Please enter mobile number."
                        amt == null || amt < 0L -> errorMsg = "Enter a valid payable amount."
                        else -> onSave(
                            cleanName,
                            cleanPhone,
                            amt,
                            if (applyToAllMonths) "" else monthKey.trim().ifBlank { defaultMonthKey },
                            notes.trim().takeIf { it.isNotBlank() }
                        )
                    }
                },
                modifier = Modifier.testTag("save_organizer_button")
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
fun RecordForwardPaymentDialog(
    strings: UiStrings,
    currency: String,
    organizers: List<ForwardOrganizer>,
    existingPayment: ForwardPaymentRecord?,
    initialOrganizerId: String?,
    defaultMonthKey: String,
    onImportProof: (uri: Uri, onResult: (localPath: String, label: String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        organizerId: String,
        monthKey: String,
        amountForwarded: Long,
        paymentDate: String,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        proofUri: String?,
        proofLabel: String?,
        notes: String?,
        confirmedByAdmin: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedOrganizerId by remember(existingPayment, initialOrganizerId, organizers) {
        mutableStateOf(
            existingPayment?.organizerId
                ?: initialOrganizerId
                ?: organizers.firstOrNull()?.id.orEmpty()
        )
    }
    var amountText by remember(existingPayment) {
        mutableStateOf(existingPayment?.amountForwarded?.toString().orEmpty())
    }
    var paymentDate by remember(existingPayment) {
        mutableStateOf(existingPayment?.paymentDate ?: MonthUtils.todayDateString())
    }
    var monthKey by remember(existingPayment, defaultMonthKey) {
        mutableStateOf(existingPayment?.monthKey ?: defaultMonthKey)
    }
    var method by remember(existingPayment) {
        mutableStateOf(existingPayment?.paymentMethodEnum ?: PaymentMethod.CASH)
    }
    var txId by remember(existingPayment) { mutableStateOf(existingPayment?.transactionId.orEmpty()) }
    var proofPath by remember(existingPayment) { mutableStateOf(existingPayment?.proofUri) }
    var proofLabel by remember(existingPayment) { mutableStateOf(existingPayment?.proofLabel) }
    var notes by remember(existingPayment) { mutableStateOf(existingPayment?.notes.orEmpty()) }
    var confirmedByAdmin by remember(existingPayment) {
        mutableStateOf(existingPayment?.confirmedByAdmin ?: false)
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportProof(uri) { path, label ->
                proofPath = path
                proofLabel = label
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportProof(uri) { path, label ->
                proofPath = path
                proofLabel = label
            }
        }
    }

    val selectedOrg = organizers.firstOrNull { it.id == selectedOrganizerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.recordForwardPaymentLabel,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Recipient / Committee Organizer:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    organizers.forEach { org ->
                        FilterChip(
                            selected = selectedOrganizerId == org.id,
                            onClick = { selectedOrganizerId = org.id; errorMsg = null },
                            label = { Text("${org.organizerName} (${org.organizerPhone})") }
                        )
                    }
                }
                if (selectedOrg != null) {
                    Text(
                        text = "${strings.totalPayableLabel}: ${FormatUtils.formatMoney(selectedOrg.totalPayableAmount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { ch -> amountText = ch.filter { it.isDigit() }; errorMsg = null },
                    label = { Text(strings.forwardedAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("forward_amount_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text(strings.paymentDate) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthKey,
                        onValueChange = { monthKey = it },
                        label = { Text("Month") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = method == pm,
                            onClick = { method = pm },
                            label = { Text(pm.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = txId,
                    onValueChange = { txId = it },
                    label = { Text(strings.transactionIdLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Zero-permission Photo Picker & Document Proof Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("upload_proof_photo_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.attachProofPhotoLabel, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                        },
                        modifier = Modifier.weight(1f).testTag("upload_proof_file_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF / File", maxLines = 1)
                    }
                }

                if (!proofPath.isNullOrBlank()) {
                    Surface(
                        color = StatusPaidBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = proofLabel ?: strings.proofAttachedLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusPaidGreen,
                                    maxLines = 1
                                )
                            }
                            TextButton(onClick = { ProofFileHelper.openProofFile(context, proofPath!!) }) {
                                Text(strings.viewLabel)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.paymentNote) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = confirmedByAdmin,
                            onCheckedChange = { confirmedByAdmin = it; errorMsg = null },
                            modifier = Modifier.testTag("confirm_forward_checkbox")
                        )
                        Text(
                            text = strings.confirmForwardedCheckboxLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    when {
                        selectedOrganizerId.isBlank() -> errorMsg = "Select a recipient/organizer."
                        amt <= 0L -> errorMsg = "Enter forwarded amount > 0."
                        !confirmedByAdmin -> errorMsg = "Please confirm money has been forwarded."
                        else -> onSave(
                            selectedOrganizerId,
                            monthKey.trim().ifBlank { defaultMonthKey },
                            amt,
                            paymentDate.trim().ifBlank { MonthUtils.todayDateString() },
                            method,
                            txId.trim().takeIf { it.isNotBlank() },
                            proofPath,
                            proofLabel,
                            notes.trim().takeIf { it.isNotBlank() },
                            confirmedByAdmin
                        )
                    }
                },
                modifier = Modifier.testTag("save_forward_payment_btn")
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
fun ReverseForwardPaymentDialog(
    strings: UiStrings,
    payment: ForwardPaymentRecord,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmReverse: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.reversePaymentLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${payment.organizerName} • ${FormatUtils.formatMoney(payment.amountForwarded, currency)} (${payment.receiptNumber ?: "-"})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; errorMsg = null },
                    label = { Text(strings.reverseReasonHint) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("reverse_forward_reason_input")
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().isBlank()) {
                        errorMsg = "Please enter reversal reason."
                    } else {
                        onConfirmReverse(reason.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_reverse_forward_btn")
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun OrganizerUrduSummaryDialog(
    strings: UiStrings,
    organizer: ForwardOrganizer?,
    initialMessage: String,
    onCopyMessage: (String) -> Unit,
    onSendWhatsApp: (phone: String, message: String) -> Unit,
    onSendSms: (phone: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    var phone by remember(organizer) { mutableStateOf(organizer?.organizerPhone.orEmpty()) }
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.organizerUrduSummaryTitle, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.organizerMobileLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("اردو مالی خلاصہ (ترمیم کے قابل)") },
                    minLines = 7,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth().testTag("organizer_urdu_summary_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopyMessage(message) },
                        modifier = Modifier.weight(1f).testTag("copy_organizer_summary_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", maxLines = 1)
                    }
                    Button(
                        onClick = { onSendWhatsApp(phone, message) },
                        modifier = Modifier.weight(1f).testTag("whatsapp_organizer_summary_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.openWhatsApp, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = { onSendSms(phone, message) },
                    modifier = Modifier.fillMaxWidth().testTag("sms_organizer_summary_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openSms, maxLines = 1)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
