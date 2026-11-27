package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeTurn
import com.example.model.FormatUtils
import com.example.model.MonthUtils
import com.example.model.PayoutStatus
import com.example.ui.components.EmptyCommitteePrompt
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun CommitteeTurnsScreen(
    strings: UiStrings,
    committee: Committee?,
    turns: List<CommitteeTurn>,
    currency: String = "PKR",
    onCreateCommittee: () -> Unit,
    onAddTurn: () -> Unit,
    onEditTurn: (CommitteeTurn) -> Unit,
    onSwapOrder: (CommitteeTurn, CommitteeTurn) -> Unit,
    onMarkTurnPaid: (CommitteeTurn) -> Unit,
    onRevertTurnPending: (CommitteeTurn) -> Unit,
    onDeleteTurn: (CommitteeTurn) -> Unit,
    onGenerateFromShares: () -> Unit,
    onLoadKnownTurns: () -> Unit
) {
    if (committee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyCommitteePrompt(strings = strings, onCreateCommittee = onCreateCommittee)
        }
        return
    }

    val sortedTurns = remember(turns) { turns.sortedBy { it.turnNumber } }
    val paidCount = sortedTurns.count { it.statusEnum == PayoutStatus.PAID }
    val pendingCount = sortedTurns.count { it.statusEnum == PayoutStatus.PENDING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${committee.name} • ${strings.navTurns}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${strings.eachPayoutAmount}: ${FormatUtils.formatMoney(committee.eachPayoutAmount, currency)} • ${strings.paidLabel}: $paidCount • ${strings.pendingLabel}: $pendingCount",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.reorderPayoutHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddTurn,
                            modifier = Modifier.weight(1f).testTag("add_turn_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addTurnLabel, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onGenerateFromShares,
                            modifier = Modifier.weight(1f).testTag("generate_turns_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.generateTurnsFromSharesLabel, maxLines = 1)
                        }
                    }
                    if (sortedTurns.size < 4) {
                        OutlinedButton(
                            onClick = onLoadKnownTurns,
                            modifier = Modifier.fillMaxWidth().testTag("load_known_turns_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.loadKnownTurnsLabel, maxLines = 1)
                        }
                    }
                }
            }
        }

        if (sortedTurns.isEmpty()) {
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
                            text = "No committee turns yet. Tap Add Turn, Generate from Shares, or Load Known Turns 1–4.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(sortedTurns, key = { _, t -> t.id }) { idx, turn ->
                val windowText = MonthUtils.formatCollectionWindowFull(
                    turn.scheduledMonth,
                    committee.collectionDayStart,
                    committee.collectionDayEnd
                )
                val dateLabel = turn.payoutDate?.takeIf { it.isNotBlank() } ?: strings.unknownDateLabel
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("turn_card_${turn.turnNumber}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Turn #${turn.turnNumber} • ${turn.recipientName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = FormatUtils.formatMoney(turn.payoutAmount, currency) +
                                        (turn.recipientPhone?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Window: $windowText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${strings.paymentDate}: $dateLabel" +
                                        if (turn.confirmedByAdmin) " • ✓ ${strings.confirmedByAdminLabel}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (turn.confirmedByAdmin) StatusPaidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!turn.notes.isNullOrBlank()) {
                                    Text(
                                        text = turn.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            PayoutStatusBadge(turn.statusEnum, strings)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(
                                    onClick = { if (idx > 0) onSwapOrder(turn, sortedTurns[idx - 1]) },
                                    enabled = idx > 0
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = strings.moveUp)
                                }
                                IconButton(
                                    onClick = { if (idx < sortedTurns.lastIndex) onSwapOrder(turn, sortedTurns[idx + 1]) },
                                    enabled = idx < sortedTurns.lastIndex
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = strings.moveDown)
                                }
                                IconButton(onClick = { onEditTurn(turn) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = strings.editLabel)
                                }
                                IconButton(onClick = { onDeleteTurn(turn) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.deleteLabel, tint = StatusUnpaidRed)
                                }
                            }

                            if (turn.statusEnum == PayoutStatus.PENDING) {
                                Button(
                                    onClick = { onMarkTurnPaid(turn) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("mark_turn_paid_${turn.turnNumber}")
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.markPaid, maxLines = 1)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onRevertTurnPending(turn) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = StatusPendingYellow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.markPayoutPending, color = StatusPendingYellow, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
