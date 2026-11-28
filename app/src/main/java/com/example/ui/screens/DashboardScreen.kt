package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.UiStrings
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.DashboardSummary
import com.example.model.FormatUtils
import com.example.model.ForwardFinancialSummary
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MonthUtils
import com.example.model.PayoutStatus
import com.example.ui.components.CommitteeStatusBadge
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun DashboardScreen(
    strings: UiStrings,
    companySettings: AppCompanySettings,
    committee: Committee?,
    summary: DashboardSummary?,
    members: List<CommitteeMember>,
    installments: List<InstallmentRecord>,
    turns: List<CommitteeTurn>,
    forwardSummary: ForwardFinancialSummary? = null,
    unreadNotificationCount: Int = 0,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    onOpenCommitteeSwitcher: () -> Unit,
    onCreateCommittee: () -> Unit,
    onViewCommittee: (Committee) -> Unit,
    onEditCommittee: (Committee) -> Unit,
    onDeleteCommittee: (Committee) -> Unit,
    onQuickAddMember: () -> Unit,
    onGoToCommittees: () -> Unit,
    onGoToMembers: () -> Unit,
    onGoToInstallments: () -> Unit,
    onGoToPendingInstallments: () -> Unit,
    onGoToForwardPayments: () -> Unit = {},
    onGoToTurns: () -> Unit,
    onGoToOldHistory: () -> Unit,
    onGoToReminders: () -> Unit,
    onGoToReports: () -> Unit,
    onGoToSettings: () -> Unit,
    onOpenPaidDetail: () -> Unit,
    onOpenUnpaidDetail: () -> Unit,
    onOpenUpcomingDetail: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenNotifications: () -> Unit,
    onPrintSummary: () -> Unit,
    onDownloadSummary: () -> Unit,
    onOpenAuditHistory: () -> Unit,
    onRefresh: () -> Unit
) {
    if (committee == null || summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    val currency = companySettings.currency
    val progressRatio = if (summary.monthlyCollectionTarget > 0L) {
        (summary.currentMonthCollected.toFloat() / summary.monthlyCollectionTarget.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val monthInstallments = remember(installments, selectedMonthKey) {
        installments.filter { it.monthKey == selectedMonthKey }.sortedBy { it.committeeNumber }
    }
    val paidMonthList = remember(monthInstallments) {
        monthInstallments.filter { it.statusEnum == InstallmentStatus.PAID }
    }
    val unpaidMonthList = remember(monthInstallments) {
        monthInstallments.filter { it.statusEnum != InstallmentStatus.PAID }
    }
    val paidRecipients = remember(turns, members) {
        if (turns.isNotEmpty()) {
            turns.filter { it.statusEnum == PayoutStatus.PAID }.sortedBy { it.turnNumber }
                .map { "Turn #${it.turnNumber}: ${it.recipientName} (${FormatUtils.formatMoney(it.payoutAmount, currency)})" }
        } else {
            members.filter { it.payoutStatusEnum == PayoutStatus.PAID }.sortedBy { it.committeeNumber }
                .map { "#${it.committeeNumber}: ${it.name}" }
        }
    }
    val nextInLineRecipients = remember(turns, members) {
        if (turns.isNotEmpty()) {
            turns.filter { it.statusEnum == PayoutStatus.PENDING }.sortedBy { it.turnNumber }
                .map { "Turn #${it.turnNumber}: ${it.recipientName} (${MonthUtils.formatMonthDisplay(it.scheduledMonth)})" }
        } else {
            members.filter { it.payoutStatusEnum == PayoutStatus.PENDING }.sortedBy { it.committeeNumber }
                .map { "#${it.committeeNumber}: ${it.name}" }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Brand & Active Committee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gm_hero_banner),
                        contentDescription = "${companySettings.companyName} Banner",
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xEB063D2B), Color(0xD60B6E4F))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_gm_committee_fg),
                                        contentDescription = "GM HASMANI Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    color = GoldAccent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = companySettings.companyName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                CommitteeStatusBadge(committee.statusEnum, strings)
                            }
                            IconButton(
                                onClick = onOpenCommitteeSwitcher,
                                modifier = Modifier.testTag("switch_committee_button")
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = strings.switchCommittee, tint = Color.White)
                            }
                        }

                        Text(
                            text = committee.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onOpenCommitteeSwitcher() }
                        )

                        Text(
                            text = "${committee.memberCount} Members • ${committee.effectiveTotalShares} Shares • ${FormatUtils.formatMoney(committee.monthlyInstallment, currency)}/share • ${committee.totalMonths} Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = strings.monthlyCollection,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FormatUtils.formatMoney(committee.monthlyCollectionTotal, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = strings.totalCommitteeAmount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FormatUtils.formatMoney(committee.totalCommitteeAmount, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { onViewCommittee(committee) },
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("view_committee_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.viewLabel, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = { onEditCommittee(committee) },
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("edit_committee_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.editLabel, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = { onDeleteCommittee(committee) },
                                color = Color(0x55DC2626),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("delete_committee_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.deleteLabel, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Month selector
        item {
            MonthSelectorBar(
                committee = committee,
                selectedMonthKey = selectedMonthKey,
                strings = strings,
                onSelectMonth = onSelectMonth
            )
        }

        // 3x2 Summary KPIs covering every requested metric
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.totalActiveCommittees,
                        value = "${summary.totalActiveCommittees} Active",
                        subtitle = "${strings.completedCommitteesLabel}: ${summary.totalCompletedCommittees}",
                        icon = Icons.Rounded.PieChart,
                        accentColor = EmeraldPrimary,
                        onClick = onGoToCommittees,
                        modifier = Modifier.weight(1f).testTag("kpi_committees")
                    )
                    KpiMetricCard(
                        title = "${strings.activeMembers} & ${strings.totalSharesLabel}",
                        value = "${summary.activeMemberCount} / ${summary.targetMemberCount}",
                        subtitle = "${FormatUtils.formatShares(summary.totalMemberShares)} / ${summary.totalSharesConfigured} Shares",
                        icon = Icons.Rounded.Group,
                        accentColor = EmeraldPrimary,
                        onClick = onGoToMembers,
                        modifier = Modifier.weight(1f).testTag("kpi_members")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.totalCollected,
                        value = FormatUtils.formatMoney(summary.currentMonthCollected, currency),
                        subtitle = "${strings.paidMembers}: ${summary.paidMembersCount} • Today: ${FormatUtils.formatCompactNumber(summary.dailyCollectionToday)}",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = StatusPaidGreen,
                        onClick = onOpenPaidDetail,
                        modifier = Modifier.weight(1f).testTag("kpi_paid")
                    )
                    KpiMetricCard(
                        title = strings.totalPending,
                        value = FormatUtils.formatMoney(summary.currentMonthPending, currency),
                        subtitle = "${strings.unpaidMembers}: ${summary.unpaidMembersCount} • Overdue: ${summary.overdueMembersCount}",
                        icon = Icons.Rounded.PendingActions,
                        accentColor = if (summary.currentMonthPending == 0L) StatusPaidGreen else StatusUnpaidRed,
                        onClick = onOpenUnpaidDetail,
                        modifier = Modifier.weight(1f).testTag("kpi_pending")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val nextLabel = summary.nextPayoutTurn?.let { "Turn #${it.turnNumber}: ${it.recipientName}" }
                        ?: summary.nextPayoutMember?.let { "#${it.committeeNumber} ${it.name}" }
                        ?: strings.completedLabel
                    KpiMetricCard(
                        title = strings.nextPayoutMember,
                        value = nextLabel,
                        subtitle = "${FormatUtils.formatMoney(summary.nextPayoutAmount, currency)} • ${strings.nextDueDateLabel}: ${summary.nextPaymentDueDate}",
                        icon = Icons.Rounded.EventAvailable,
                        accentColor = GoldAccent,
                        onClick = onOpenUpcomingDetail,
                        modifier = Modifier.weight(1f).testTag("kpi_next_payout")
                    )
                    KpiMetricCard(
                        title = strings.totalPayoutsLabel,
                        value = FormatUtils.formatMoney(summary.totalPayoutsAmount, currency),
                        subtitle = "${strings.completedPayouts}: ${summary.completedPayouts}/${summary.totalMonths} • Left: ${summary.remainingMonths}m",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = EmeraldPrimary,
                        onClick = onGoToReports,
                        modifier = Modifier.weight(1f).testTag("kpi_payouts")
                    )
                }
            }
        }

        // Monthly Progress Bar (clickable -> Payments)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoToInstallments() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.collectionProgress} • ${MonthUtils.formatMonthDisplay(summary.currentMonthKey)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusPaidGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = StatusPaidGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${FormatUtils.formatMoney(summary.currentMonthCollected, currency)} / ${FormatUtils.formatMoney(summary.monthlyCollectionTarget, currency)} • ${summary.upcomingPayoutDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 16 Interactive Admin Modules (every item performs a real function)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard Sections (16 Modules)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = onOpenNotifications,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dashboard_notifications_btn")
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = strings.notificationsTitle, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${strings.notificationsTitle}${if (unreadNotificationCount > 0) " ($unreadNotificationCount)" else ""}", maxLines = 1)
                    }
                }

                val heldLabel = if (forwardSummary != null) {
                    "Held ${FormatUtils.formatCompactNumber(forwardSummary.amountCurrentlyHeldByAdmin)}"
                } else {
                    "Organizers & Proof"
                }
                val tiles = listOf(
                    DashboardTile("1. Total Members", "${summary.activeMemberCount} Active", Icons.Rounded.Group, EmeraldPrimary, "tile_total_members", onGoToMembers),
                    DashboardTile("2. Add New Member", "ID, Father, Shares", Icons.Rounded.PersonAdd, EmeraldPrimary, "tile_add_member", onQuickAddMember),
                    DashboardTile("3. Monthly Collection", FormatUtils.formatCompactNumber(summary.currentMonthCollected), Icons.Rounded.Payments, StatusPaidGreen, "tile_monthly_collection", onGoToInstallments),
                    DashboardTile("4. Paid Members", "${paidMonthList.size} Paid", Icons.Rounded.CheckCircle, StatusPaidGreen, "tile_paid_members", onOpenPaidDetail),
                    DashboardTile("5. Unpaid Members", "${unpaidMonthList.size} Due / Overdue", Icons.Rounded.PendingActions, StatusUnpaidRed, "tile_unpaid_members", onOpenUnpaidDetail),
                    DashboardTile("6. Pending Payments", FormatUtils.formatCompactNumber(summary.currentMonthPending), Icons.Rounded.AccountBalanceWallet, StatusPendingYellow, "tile_pending_payments", onGoToPendingInstallments),
                    DashboardTile("7. Committee Winners", "${paidRecipients.size} Won • Draw", Icons.Rounded.EmojiEvents, GoldAccent, "tile_winners", onGoToTurns),
                    DashboardTile("8. Upcoming Committee", summary.nextPayoutTurn?.recipientName ?: "Schedule", Icons.Rounded.EventAvailable, GoldAccent, "tile_upcoming", onOpenUpcomingDetail),
                    DashboardTile("9. Previous History", "Archive & Cycles", Icons.Rounded.History, EmeraldPrimary, "tile_prev_history", onGoToOldHistory),
                    DashboardTile("10. Send Messages", "Urdu WA / SMS / App", Icons.Rounded.Sms, EmeraldPrimary, "tile_send_messages", onGoToReminders),
                    DashboardTile("11. Payment Reports", "11 PDF + Excel", Icons.Rounded.PictureAsPdf, EmeraldPrimary, "tile_payment_reports", onGoToReports),
                    DashboardTile("12. Member Management", "Search, Edit, PIN", Icons.Rounded.Group, EmeraldPrimary, "tile_member_mgmt", onGoToMembers),
                    DashboardTile("13. Committee Mgmt", "${summary.totalActiveCommittees} Active", Icons.Rounded.AccountBalance, EmeraldPrimary, "tile_committee_mgmt", onGoToCommittees),
                    DashboardTile("14. Settings", "Company, 03423498631", Icons.Rounded.Settings, EmeraldPrimary, "tile_settings", onGoToSettings),
                    DashboardTile("15. Backup & Restore", "JSON Save / Load", Icons.Rounded.Backup, EmeraldPrimary, "tile_backup_restore", onOpenBackupRestore),
                    DashboardTile("16. Forward Payments", heldLabel, Icons.Rounded.ForwardToInbox, EmeraldPrimary, "tile_forward_payments", onGoToForwardPayments)
                )

                tiles.chunked(3).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            DashboardModuleTileCard(tile = tile, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Live Paid / Unpaid & Recipients / Next-in-Line Summaries (All clickable)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMiniListCard(
                    title = "${strings.paidMembers} (${paidMonthList.size})",
                    items = paidMonthList.map { "#${it.committeeNumber} ${it.memberName} — ${FormatUtils.formatMoney(it.paidAmount, currency)}" },
                    emptyText = "No paid installments recorded yet for this month.",
                    badgeColor = StatusPaidGreen,
                    badgeBg = StatusPaidBg,
                    onClick = onOpenPaidDetail
                )
                DashboardMiniListCard(
                    title = "${strings.unpaidMembers} / ${strings.partialLabel} (${unpaidMonthList.size})",
                    items = unpaidMonthList.map { "#${it.committeeNumber} ${it.memberName} — Due ${FormatUtils.formatMoney(it.remainingAmount, currency)}" },
                    emptyText = "All members have paid for this month.",
                    badgeColor = StatusUnpaidRed,
                    badgeBg = StatusUnpaidBg,
                    onClick = onOpenUnpaidDetail
                )
                DashboardMiniListCard(
                    title = "Committee Recipients (${paidRecipients.size} Paid • ${nextInLineRecipients.size} Next in Line)",
                    items = paidRecipients.map { "✓ $it" } + nextInLineRecipients.map { "→ Next: $it" },
                    emptyText = "No committee turns configured yet.",
                    badgeColor = StatusPendingYellow,
                    badgeBg = StatusPendingBg,
                    onClick = onGoToTurns
                )
            }
        }

        // Quick Action Buttons
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = strings.createNewCommittee,
                        icon = Icons.Rounded.Add,
                        primary = true,
                        onClick = onCreateCommittee,
                        modifier = Modifier.weight(1f).testTag("dashboard_new_committee_btn")
                    )
                    DashboardActionButton(
                        label = strings.addMember,
                        icon = Icons.Rounded.PersonAdd,
                        primary = true,
                        onClick = onQuickAddMember,
                        modifier = Modifier.weight(1f).testTag("quick_add_member_btn")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = strings.navInstallments,
                        icon = Icons.Rounded.Payments,
                        tonal = true,
                        onClick = onGoToInstallments,
                        modifier = Modifier.weight(1f).testTag("quick_payments_btn")
                    )
                    DashboardActionButton(
                        label = strings.navTurns,
                        icon = Icons.AutoMirrored.Rounded.ListAlt,
                        tonal = true,
                        onClick = onGoToTurns,
                        modifier = Modifier.weight(1f).testTag("quick_turns_btn")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = strings.navMessageRequests,
                        icon = Icons.Rounded.Sms,
                        tonal = true,
                        onClick = onGoToReminders,
                        modifier = Modifier.weight(1f).testTag("quick_reminders_btn")
                    )
                    DashboardActionButton(
                        label = strings.navPdfReports,
                        icon = Icons.Rounded.PictureAsPdf,
                        tonal = true,
                        onClick = onGoToReports,
                        modifier = Modifier.weight(1f).testTag("quick_reports_btn")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = strings.printLabel,
                        icon = Icons.Rounded.Print,
                        onClick = onPrintSummary,
                        modifier = Modifier.weight(1f).testTag("dashboard_print_btn")
                    )
                    DashboardActionButton(
                        label = strings.downloadLabel,
                        icon = Icons.Rounded.Download,
                        onClick = onDownloadSummary,
                        modifier = Modifier.weight(1f).testTag("dashboard_download_btn")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = strings.auditLogTitle,
                        icon = Icons.Rounded.History,
                        onClick = onOpenAuditHistory,
                        modifier = Modifier.weight(1f).testTag("dashboard_audit_btn")
                    )
                    DashboardActionButton(
                        label = strings.refreshLabel,
                        icon = Icons.Rounded.Refresh,
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f).testTag("dashboard_refresh_btn")
                    )
                }
            }
        }
    }
}

private data class DashboardTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardModuleTileCard(
    tile: DashboardTile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { tile.onClick() }
            .testTag(tile.testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = tile.accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = tile.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = tile.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardMiniListCard(
    title: String,
    items: List<String>,
    emptyText: String,
    badgeColor: Color,
    badgeBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = title,
                    color = badgeColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.take(8).forEach { row ->
                    Text(
                        text = row,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (items.size > 8) {
                    Text(
                        text = "+ ${items.size - 8} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardActionButton(
    label: String,
    icon: ImageVector,
    primary: Boolean = false,
    tonal: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        primary -> Button(
            onClick = onClick,
            modifier = modifier.height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        }
        tonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        }
        else -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
    }
}
