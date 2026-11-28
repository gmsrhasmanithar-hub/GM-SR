package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.FormatUtils
import com.example.model.InAppNotification
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MonthUtils
import com.example.ui.components.InstallmentStatusBadge
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPortalScreen(
    strings: UiStrings,
    currentLang: AppLanguage,
    companySettings: AppCompanySettings,
    member: CommitteeMember,
    committee: Committee?,
    installments: List<InstallmentRecord>,
    turns: List<CommitteeTurn>,
    notifications: List<InAppNotification>,
    onLanguageToggle: () -> Unit,
    onViewReceipt: (InstallmentRecord) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onLogout: () -> Unit
) {
    val currency = companySettings.currency
    val perShare = committee?.monthlyInstallment ?: companySettings.defaultMonthlyContribution
    val monthlyDue = member.monthlyDue(perShare)
    val sortedInstallments = remember(installments) {
        installments.sortedByDescending { it.monthKey }
    }
    val totalPaid = sortedInstallments.sumOf { it.paidAmount }
    val totalDue = sortedInstallments.sumOf { it.dueAmount }
    val totalRemaining = (totalDue - totalPaid).coerceAtLeast(0L)
    val collectionStart = committee?.collectionDayStart ?: companySettings.defaultCollectionStartDay
    val collectionEnd = committee?.collectionDayEnd ?: companySettings.defaultCollectionEndDay
    val currentMonthRecord = sortedInstallments.firstOrNull { it.monthKey == MonthUtils.currentMonthKey() }
        ?: sortedInstallments.firstOrNull()
    val currentEffectiveStatus = currentMonthRecord?.effectiveStatus(collectionStart, collectionEnd) ?: InstallmentStatus.UNPAID
    val nextDueDate = MonthUtils.dueDateString(
        monthKey = currentMonthRecord?.monthKey ?: MonthUtils.currentMonthKey(),
        startDay = collectionStart,
        endDay = collectionEnd
    )
    val unreadCount = notifications.count { !it.isRead }
    val timeFmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.US) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(28.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_gm_committee_fg),
                                contentDescription = companySettings.companyName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = companySettings.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = currentLang == AppLanguage.UR,
                        onClick = onLanguageToggle,
                        label = { Text(if (currentLang == AppLanguage.EN) "اردو" else "EN", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("member_lang_toggle")
                    )
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("member_logout_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.logoutTitle)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Member Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("member_portal_profile_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(38.dp))
                                Column {
                                    Text(
                                        text = "${member.displayMemberId} • ${member.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val sub = buildString {
                                        if (member.fatherName.isNotBlank()) append("S/O ${member.fatherName} • ")
                                        append(member.phone)
                                    }
                                    Text(text = sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            InstallmentStatusBadge(currentEffectiveStatus, strings)
                        }
                        HorizontalDivider()
                        Text(
                            text = "Committee: ${committee?.name ?: "Active Circle"} • ${FormatUtils.formatShares(member.effectiveShares)} Share(s)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Monthly Contribution", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatUtils.formatMoney(monthlyDue, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Upcoming Due Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(nextDueDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid: ${FormatUtils.formatMoney(totalPaid, currency)}", style = MaterialTheme.typography.bodySmall, color = StatusPaidGreen, fontWeight = FontWeight.Bold)
                            Text("Remaining Due: ${FormatUtils.formatMoney(totalRemaining, currency)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Easypaisa / Contact: ${companySettings.easypaisaNumber.ifBlank { "03423498631" }} (${companySettings.companyName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Member Winner / Turn Cards
            if (turns.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldAccent)
                        Text("My Committee Turns & Payouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                items(turns, key = { it.id }) { turn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Turn #${turn.turnNumber} • ${MonthUtils.formatMonthDisplay(turn.scheduledMonth)}", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${FormatUtils.formatMoney(turn.payoutAmount, currency)} • Date: ${turn.payoutDate?.ifBlank { "Pending" } ?: "Pending"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PayoutStatusBadge(turn.statusEnum, strings)
                        }
                    }
                }
            }

            // Notifications & Announcements Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = EmeraldPrimary)
                        Text(
                            text = "${strings.notificationsTitle}${if (unreadCount > 0) " ($unreadCount New)" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (unreadCount > 0) {
                        TextButton(onClick = onMarkAllNotificationsRead) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark All Read")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No personal notifications or announcements yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(notifications, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!note.isRead) onMarkNotificationRead(note.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (note.isRead) MaterialTheme.colorScheme.surface
                            else StatusUnpaidBg.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(note.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(timeFmt.format(Date(note.createdAtMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(note.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Personal Payment History + Receipts
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = EmeraldPrimary)
                    Text("My Payment History & Receipts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (sortedInstallments.isEmpty()) {
                item {
                    Text(strings.noInstallmentsYet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(sortedInstallments, key = { it.id }) { rec ->
                    val effectiveStatus = rec.effectiveStatus(collectionStart, collectionEnd)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "M${rec.monthIndex} • ${MonthUtils.formatMonthDisplay(rec.monthKey)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${FormatUtils.formatMoney(rec.paidAmount, currency)} / ${FormatUtils.formatMoney(rec.dueAmount, currency)}" +
                                            (rec.receiptNumber?.let { " • $it" } ?: "") +
                                            (rec.paymentDate?.let { " • $it" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                InstallmentStatusBadge(effectiveStatus, strings)
                            }
                            if (rec.paidAmount > 0L) {
                                OutlinedButton(
                                    onClick = { onViewReceipt(rec) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${strings.viewReceiptLabel} / Print / PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
