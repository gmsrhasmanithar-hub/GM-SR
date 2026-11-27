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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.DashboardSummary
import com.example.model.FormatUtils
import com.example.model.MonthUtils
import com.example.ui.components.CollectionDonutChart
import com.example.ui.components.CommitteeStatusBadge
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.MonthlyCollectionBarChart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun DashboardScreen(
    strings: UiStrings,
    committee: Committee?,
    summary: DashboardSummary?,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    onOpenCommitteeSwitcher: () -> Unit,
    onCreateCommittee: () -> Unit,
    onEditCommittee: (Committee) -> Unit,
    onQuickAddMember: () -> Unit,
    onGoToInstallments: () -> Unit,
    onGoToReminders: () -> Unit,
    onGoToReports: () -> Unit
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Hero Committee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gm_hero_banner),
                        contentDescription = "GM HASMANI Banner",
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xE6063D2B),
                                        Color(0xCC0B6E4F)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
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
                                    modifier = Modifier.size(32.dp)
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
                                        text = strings.brandName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                CommitteeStatusBadge(committee.statusEnum, strings)
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditCommittee(committee) },
                                    modifier = Modifier.testTag("edit_committee_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = strings.editCommittee,
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onOpenCommitteeSwitcher,
                                    modifier = Modifier.testTag("switch_committee_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.SwapHoriz,
                                        contentDescription = strings.switchCommittee,
                                        tint = Color.White
                                    )
                                }
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
                            text = "${committee.memberCount} Members • ${FormatUtils.formatPkr(committee.monthlyInstallment)}/mo • ${committee.totalMonths} Months",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.monthlyCollection,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = FormatUtils.formatPkr(committee.monthlyCollectionTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = strings.totalCommitteeAmount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = FormatUtils.formatPkr(committee.totalCommitteeAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
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

        // Next Payout Spotlight
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = GoldAccent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.EventAvailable,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Column {
                            Text(
                                text = strings.nextPayoutMember,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = summary.nextPayoutMember?.let {
                                    "#${it.committeeNumber} • ${it.name}"
                                } ?: "All Scheduled Payouts Completed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.upcomingPayoutDate}: ${summary.upcomingPayoutDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // KPI Grid (2 columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.totalActiveCommittees,
                        value = "${summary.totalActiveCommittees}",
                        subtitle = committee.name,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.activeMembers,
                        value = "${summary.activeMemberCount} / ${summary.targetMemberCount}",
                        subtitle = "${strings.paidMembers}: ${summary.paidMembersCount} • ${strings.partialPaymentsLabel}: ${summary.partialMembersCount}",
                        icon = Icons.Rounded.Group,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.totalCollected,
                        value = FormatUtils.formatPkr(summary.currentMonthCollected),
                        subtitle = "All: ${FormatUtils.formatPkr(summary.totalCommitteeCollectedAllMonths)}",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = StatusPaidGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.totalPending,
                        value = FormatUtils.formatPkr(summary.currentMonthPending),
                        subtitle = "${strings.unpaidMembers}: ${summary.unpaidMembersCount} • ${strings.partialLabel}: ${summary.partialMembersCount}",
                        icon = Icons.Rounded.PendingActions,
                        accentColor = if (summary.currentMonthPending == 0L) StatusPaidGreen else StatusUnpaidRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.partialPaymentsLabel,
                        value = "${summary.partialMembersCount}",
                        subtitle = "${strings.paidMembers}: ${summary.paidMembersCount} / ${summary.activeMemberCount}",
                        icon = Icons.Rounded.Payments,
                        accentColor = StatusPendingYellow,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.currentMonth,
                        value = "M${summary.currentMonthIndex} / ${summary.totalMonths}",
                        subtitle = MonthUtils.formatMonthDisplay(summary.currentMonthKey),
                        icon = Icons.Rounded.CalendarMonth,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.completedPayouts,
                        value = "${summary.completedPayouts} / ${summary.activeMemberCount.coerceAtLeast(summary.targetMemberCount)}",
                        subtitle = FormatUtils.formatPkr(committee.eachPayoutAmount),
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.remainingMonths,
                        value = "${summary.remainingMonths}",
                        subtitle = "${strings.endMonthLabel}: ${MonthUtils.formatMonthDisplay(committee.endMonth)}",
                        icon = Icons.Rounded.Timer,
                        accentColor = StatusPendingYellow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Donut Progress Chart
        item {
            CollectionDonutChart(
                collected = summary.currentMonthCollected,
                target = summary.monthlyCollectionTarget,
                paidMembers = summary.paidMembersCount,
                partialMembers = summary.partialMembersCount,
                unpaidMembers = summary.unpaidMembersCount,
                strings = strings
            )
        }

        // Monthly Bar Chart
        item {
            MonthlyCollectionBarChart(
                points = summary.monthlyChartPoints,
                title = strings.monthlyTrend
            )
        }

        // Quick Actions
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onQuickAddMember,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_add_member_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addMember, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onGoToInstallments,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_payments_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.navInstallments, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoToReminders,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_reminders_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.navReminders, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onGoToReports,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_reports_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF & Archive", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = onCreateCommittee,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dashboard_new_committee_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.createNewCommittee)
                }
            }
        }
    }
}
