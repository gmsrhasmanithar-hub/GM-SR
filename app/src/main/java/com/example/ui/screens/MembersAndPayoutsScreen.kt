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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.CommitteeStatus
import com.example.model.FormatUtils
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PayoutStatus
import com.example.ui.components.CommitteeStatusBadge
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun MembersAndPayoutsScreen(
    strings: UiStrings,
    committees: List<Committee>,
    committee: Committee?,
    members: List<CommitteeMember>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCommittee: (Committee) -> Unit,
    onCreateCommittee: () -> Unit,
    onViewCommittee: (Committee) -> Unit,
    onEditCommittee: (Committee) -> Unit,
    onSetCommitteeStatus: (Committee, CommitteeStatus) -> Unit,
    onDeleteCommittee: (Committee) -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (CommitteeMember) -> Unit,
    onArchiveMember: (CommitteeMember) -> Unit,
    onDeleteMember: (CommitteeMember) -> Unit,
    onViewMemberDetail: (CommitteeMember) -> Unit,
    onSwapOrder: (CommitteeMember, CommitteeMember) -> Unit,
    onRequestMarkPayoutPaid: (CommitteeMember) -> Unit,
    onRevertPayoutPending: (CommitteeMember) -> Unit
) {
    if (committee == null && committees.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    var subTab by remember { mutableIntStateOf(0) } // 0 = Members, 1 = Committees, 2 = Payouts
    var showArchived by remember { mutableStateOf(false) }
    var committeeSearch by remember { mutableStateOf("") }
    var memberPendingArchive by remember { mutableStateOf<CommitteeMember?>(null) }

    val activeMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
    }
    val archivedMembers = remember(members) {
        members.filter { it.memberStatusEnum == MemberStatus.ARCHIVED }.sortedBy { it.committeeNumber }
    }

    val filteredMembers = remember(activeMembers, archivedMembers, showArchived, searchQuery) {
        val base = if (showArchived) archivedMembers else activeMembers
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) base
        else base.filter {
            it.name.lowercase().contains(q) ||
                it.phone.lowercase().contains(q) ||
                it.committeeNumber.toString() == q
        }
    }

    val filteredCommittees = remember(committees, committeeSearch) {
        val q = committeeSearch.trim().lowercase()
        if (q.isEmpty()) committees
        else committees.filter {
            it.name.lowercase().contains(q) ||
                it.startMonth.lowercase().contains(q) ||
                it.status.lowercase().contains(q)
        }
    }

    memberPendingArchive?.let { m ->
        AlertDialog(
            onDismissRequest = { memberPendingArchive = null },
            title = { Text(strings.archiveMember) },
            text = {
                Text("Archive ${m.name} from the active committee? Their past payment & payout history will remain preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveMember(m)
                        memberPendingArchive = null
                    }
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingArchive = null }) { Text(strings.cancel) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("${strings.membersSubTab} (${activeMembers.size})", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                modifier = Modifier.testTag("subtab_members")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("${strings.committeesSubTab} (${committees.size})", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                modifier = Modifier.testTag("subtab_committees")
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text(strings.payoutsSubTab, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                modifier = Modifier.testTag("subtab_payouts")
            )
        }

        when (subTab) {
            0 -> {
                if (committee == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                                    text = member.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = member.phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${strings.payoutMonth}: ${MonthUtils.formatMonthDisplay(member.payoutMonth)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
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
                                            if (member.memberStatusEnum == MemberStatus.ACTIVE) {
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
                                                    Icon(Icons.Rounded.Archive, contentDescription = strings.archiveMember)
                                                }
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
            }

            1 -> {
                // Committees Management Tab (Add, View, Edit, Search, Complete/Archive, Safe Delete)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = committeeSearch,
                                onValueChange = { committeeSearch = it },
                                placeholder = { Text(strings.searchCommitteesHint) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).testTag("committee_search_input")
                            )
                            Button(
                                onClick = onCreateCommittee,
                                modifier = Modifier.height(54.dp).testTag("manage_add_committee_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = strings.createNewCommittee)
                            }
                        }
                    }

                    items(filteredCommittees, key = { it.id }) { itemCommittee ->
                        val isSelected = itemCommittee.id == committee?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCommittee(itemCommittee) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemCommittee.name + if (isSelected) " ★" else "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${itemCommittee.memberCount} Members • ${FormatUtils.formatPkr(itemCommittee.monthlyInstallment)}/mo • ${itemCommittee.startMonth} → ${itemCommittee.endMonth}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    CommitteeStatusBadge(itemCommittee.statusEnum, strings)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onViewCommittee(itemCommittee) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.viewLabel, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onEditCommittee(itemCommittee) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.editLabel, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onDeleteCommittee(itemCommittee) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("delete_committee_card_btn_${itemCommittee.id}")
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = StatusUnpaidRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.deleteLabel, color = StatusUnpaidRed, maxLines = 1)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (itemCommittee.statusEnum == CommitteeStatus.ACTIVE) {
                                        OutlinedButton(
                                            onClick = { onSetCommitteeStatus(itemCommittee, CommitteeStatus.COMPLETED) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.completeCommittee)
                                        }
                                    }
                                    if (itemCommittee.statusEnum != CommitteeStatus.ARCHIVED) {
                                        OutlinedButton(
                                            onClick = { onSetCommitteeStatus(itemCommittee, CommitteeStatus.ARCHIVED) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.archiveCommittee)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Payout Schedule Tab
                if (committee == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${strings.eachPayoutAmount}: ${FormatUtils.formatPkr(committee.eachPayoutAmount)} • Day ${committee.payoutDay}th",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = strings.reorderPayoutHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        itemsIndexed(activeMembers, key = { _, m -> m.id }) { idx, member ->
                            val payoutAmt = if (member.payoutAmount > 0L) member.payoutAmount else committee.eachPayoutAmount
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${member.committeeNumber} • ${member.name}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${member.phone} • ${MonthUtils.formatMonthDisplay(member.payoutMonth)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = FormatUtils.formatPkr(payoutAmt) +
                                                    (member.payoutDate?.let { " • ${strings.paymentDate}: $it" } ?: ""),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        PayoutStatusBadge(member.payoutStatusEnum, strings)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row {
                                            IconButton(
                                                onClick = { if (idx > 0) onSwapOrder(member, activeMembers[idx - 1]) },
                                                enabled = idx > 0
                                            ) {
                                                Icon(Icons.Rounded.ArrowUpward, contentDescription = strings.moveUp)
                                            }
                                            IconButton(
                                                onClick = { if (idx < activeMembers.lastIndex) onSwapOrder(member, activeMembers[idx + 1]) },
                                                enabled = idx < activeMembers.lastIndex
                                            ) {
                                                Icon(Icons.Rounded.ArrowDownward, contentDescription = strings.moveDown)
                                            }
                                        }

                                        if (member.payoutStatusEnum == PayoutStatus.PENDING) {
                                            Button(
                                                onClick = { onRequestMarkPayoutPaid(member) },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.testTag("mark_payout_paid_${member.id}")
                                            ) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.markPayoutPaid)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { onRevertPayoutPending(member) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(strings.markPayoutPending, color = StatusPendingYellow)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
