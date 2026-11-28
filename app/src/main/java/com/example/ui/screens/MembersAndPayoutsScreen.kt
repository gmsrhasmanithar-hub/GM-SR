package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.FormatUtils
import com.example.model.InstallmentRecord
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun MembersScreen(
    strings: UiStrings,
    committee: Committee?,
    members: List<CommitteeMember>,
    installments: List<InstallmentRecord>,
    currency: String = "PKR",
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateCommittee: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (CommitteeMember) -> Unit,
    onToggleMemberActive: (CommitteeMember) -> Unit,
    onDeleteMember: (CommitteeMember) -> Unit,
    onViewMemberDetail: (CommitteeMember) -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    var showArchived by remember { mutableStateOf(false) }
    var memberPendingArchive by remember { mutableStateOf<CommitteeMember?>(null) }

    val activeMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
    }
    val archivedMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ARCHIVED }.sortedBy { it.committeeNumber }
    }

    val totalSharesAssigned = remember(activeMembers) {
        activeMembers.sumOf { it.effectiveShares }
    }

    val paidByMember = remember(installments) {
        installments.groupBy { it.memberId }.mapValues { (_, list) -> list.sumOf { it.paidAmount } }
    }

    val filteredMembers = remember(activeMembers, archivedMembers, showArchived, searchQuery) {
        val base = if (showArchived) archivedMembers else activeMembers
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) base
        else base.filter {
            it.name.lowercase().contains(q) ||
                it.phone.lowercase().contains(q) ||
                it.displayMemberId.lowercase().contains(q) ||
                it.fatherName.lowercase().contains(q) ||
                it.committeeNumber.toString() == q
        }
    }

    memberPendingArchive?.let { m ->
        val isActive = m.memberStatusEnum == MemberStatus.ACTIVE
        AlertDialog(
            onDismissRequest = { memberPendingArchive = null },
            title = { Text(if (isActive) strings.deactivateMemberLabel else strings.activateMemberLabel) },
            text = {
                Text(
                    if (isActive) "Deactivate ${m.name} (${m.displayMemberId})? Their past payment & payout history will remain preserved."
                    else "Re-activate ${m.name} (${m.displayMemberId}) in this committee?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleMemberActive(m)
                        memberPendingArchive = null
                    }
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingArchive = null }) { Text(strings.cancel) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = committee.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeMembers.size}/${committee.memberCount} Members • ${FormatUtils.formatShares(totalSharesAssigned)}/${committee.effectiveTotalShares} Shares",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${FormatUtils.formatMoney(committee.monthlyInstallment, currency)}/share",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(strings.searchMembersHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("member_search_input")
                )
                Button(
                    onClick = onAddMember,
                    modifier = Modifier.height(54.dp).testTag("add_member_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = strings.addMember)
                }
            }
        }

        if (archivedMembers.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("${strings.activeLabel} (${activeMembers.size})") }
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("${strings.archivedLabel} (${archivedMembers.size})") }
                    )
                }
            }
        }

        if (filteredMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.noMembersYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onAddMember) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.addMember)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(filteredMembers, key = { _, item -> item.id }) { _, member ->
                val monthlyDue = member.monthlyDue(committee.monthlyInstallment)
                val totalPaid = paidByMember[member.id] ?: 0L
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewMemberDetail(member) }
                        .testTag("member_card_${member.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${member.committeeNumber}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${member.displayMemberId} • ${member.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subMeta = buildString {
                                    if (member.fatherName.isNotBlank()) append("S/O ${member.fatherName} • ")
                                    append("${member.phone} • ${FormatUtils.formatShares(member.effectiveShares)} Share(s) (${FormatUtils.formatMoney(monthlyDue, currency)}/mo)")
                                }
                                Text(
                                    text = subMeta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${strings.totalReceived}: ${FormatUtils.formatMoney(totalPaid, currency)} • ${strings.payoutMonth}: ${MonthUtils.formatMonthDisplay(member.payoutMonth)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusPaidGreen
                                )
                            }
                            PayoutStatusBadge(member.payoutStatusEnum, strings)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onViewMemberDetail(member) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.viewLabel, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onEditMember(member) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.editLabel, maxLines = 1)
                            }
                            IconButton(
                                onClick = { memberPendingArchive = member }
                            ) {
                                Icon(
                                    Icons.Rounded.Archive,
                                    contentDescription = if (member.memberStatusEnum == MemberStatus.ACTIVE) strings.deactivateMemberLabel else strings.activateMemberLabel
                                )
                            }
                            IconButton(
                                onClick = { onDeleteMember(member) },
                                modifier = Modifier.testTag("delete_member_btn_${member.id}")
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = strings.deleteMember, tint = StatusUnpaidRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
