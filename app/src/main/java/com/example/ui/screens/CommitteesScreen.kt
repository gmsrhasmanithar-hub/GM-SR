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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeStatus
import com.example.model.FormatUtils
import com.example.ui.components.CommitteeStatusBadge
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun CommitteesScreen(
    strings: UiStrings,
    committees: List<Committee>,
    activeCommittee: Committee?,
    currency: String = "PKR",
    onSelectCommittee: (Committee) -> Unit,
    onCreateCommittee: () -> Unit,
    onViewCommittee: (Committee) -> Unit,
    onEditCommittee: (Committee) -> Unit,
    onSetCommitteeStatus: (Committee, CommitteeStatus) -> Unit,
    onDeleteCommittee: (Committee) -> Unit
) {
    if (committees.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<CommitteeStatus?>(null) }

    val filtered = remember(committees, searchQuery, statusFilter) {
        val q = searchQuery.trim().lowercase()
        committees.filter { c ->
            val matchesStatus = statusFilter == null || c.statusEnum == statusFilter
            val matchesQuery = q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.startMonth.lowercase().contains(q) ||
                c.status.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onCreateCommittee,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("committees_create_new_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.createNewCommittee, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchCommitteesHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("committee_search_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("${strings.allFilter} (${committees.size})") }
                    )
                    FilterChip(
                        selected = statusFilter == CommitteeStatus.ACTIVE,
                        onClick = { statusFilter = CommitteeStatus.ACTIVE },
                        label = { Text(strings.activeLabel) }
                    )
                    FilterChip(
                        selected = statusFilter == CommitteeStatus.COMPLETED,
                        onClick = { statusFilter = CommitteeStatus.COMPLETED },
                        label = { Text(strings.completedLabel) }
                    )
                    FilterChip(
                        selected = statusFilter == CommitteeStatus.ARCHIVED,
                        onClick = { statusFilter = CommitteeStatus.ARCHIVED },
                        label = { Text(strings.archivedLabel) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { itemCommittee ->
            val isSelected = itemCommittee.id == activeCommittee?.id
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
                                text = "${itemCommittee.memberCount} Members • ${itemCommittee.effectiveTotalShares} Shares • ${FormatUtils.formatMoney(itemCommittee.monthlyInstallment, currency)}/share",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Pool: ${FormatUtils.formatMoney(itemCommittee.monthlyCollectionTotal, currency)}/mo • ${itemCommittee.startMonth} → ${itemCommittee.endMonth} (${itemCommittee.collectionDayStart}th–${itemCommittee.collectionDayEnd}th)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
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
                        } else {
                            OutlinedButton(
                                onClick = { onSetCommitteeStatus(itemCommittee, CommitteeStatus.ACTIVE) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.activeLabel)
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
