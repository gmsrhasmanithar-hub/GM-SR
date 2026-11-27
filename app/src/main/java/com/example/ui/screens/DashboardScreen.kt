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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
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
import com.example.model.Committee
import com.example.model.DashboardSummary
import com.example.model.FormatUtils
import com.example.model.MonthUtils
import com.example.ui.components.CommitteeStatusBadge
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaidGreen
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
    onViewCommittee: (Committee) -> Unit,
    onEditCommittee: (Committee) -> Unit,
    onDeleteCommittee: (Committee) -> Unit,
    onQuickAddMember: () -> Unit,
    onGoToInstallments: () -> Unit,
    onGoToReminders: () -> Unit,
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

    val progressRatio = if (summary.monthlyCollectionTarget > 0L) {
        (summary.currentMonthCollected.toFloat() / summary.monthlyCollectionTarget.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Simple Brand & Active Committee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gm_hero_banner),
                        contentDescription = "GM HASMANI THAR Banner",
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
                                        text = strings.brandName,
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
                            text = "${committee.memberCount} Members • ${FormatUtils.formatPkr(committee.monthlyInstallment)}/mo • ${committee.totalMonths} Months",
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
                                    text = FormatUtils.formatPkr(committee.monthlyCollectionTotal),
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
                                    text = FormatUtils.formatPkr(committee.totalCommitteeAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }

                        // Action row on committee card: View, Edit, Delete
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

        // Clean 2x2 Summary KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.activeMembers,
                        value = "${summary.activeMemberCount} / ${summary.targetMemberCount}",
                        subtitle = "${strings.totalActiveCommittees}: ${summary.totalActiveCommittees}",
                        icon = Icons.Rounded.Group,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.nextPayoutMember,
                        value = summary.nextPayoutMember?.let { "#${it.committeeNumber} ${it.name}" } ?: strings.completedLabel,
                        subtitle = summary.upcomingPayoutDate,
                        icon = Icons.Rounded.EventAvailable,
                        accentColor = GoldAccent,
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
                        subtitle = "${strings.paidMembers}: ${summary.paidMembersCount}",
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
            }
        }

        // Simple Monthly Progress Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "${FormatUtils.formatPkr(summary.currentMonthCollected)} / ${FormatUtils.formatPkr(summary.monthlyCollectionTarget)} • ${strings.completedPayouts}: ${summary.completedPayouts}/${summary.totalMonths}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Large Simple Action Buttons for Android Phone
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
                        label = strings.navReminders,
                        icon = Icons.Rounded.Sms,
                        tonal = true,
                        onClick = onGoToReminders,
                        modifier = Modifier.weight(1f).testTag("quick_reminders_btn")
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
