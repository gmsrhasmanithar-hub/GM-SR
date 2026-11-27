package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.UiStrings
import com.example.model.Committee
import com.example.model.CommitteeStatus
import com.example.model.FormatUtils
import com.example.model.InstallmentStatus
import com.example.model.MonthUtils
import com.example.model.MonthlyChartPoint
import com.example.model.PayoutStatus
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingYellow
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidRed

@Composable
fun InstallmentStatusBadge(status: InstallmentStatus, strings: UiStrings) {
    val (bg, fg, label) = when (status) {
        InstallmentStatus.PAID -> Triple(StatusPaidBg, StatusPaidGreen, strings.paidLabel)
        InstallmentStatus.UNPAID -> Triple(StatusUnpaidBg, StatusUnpaidRed, strings.unpaidLabel)
        InstallmentStatus.PARTIAL -> Triple(StatusPendingBg, StatusPendingYellow, strings.partialLabel)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PayoutStatusBadge(status: PayoutStatus, strings: UiStrings) {
    val (bg, fg, label) = when (status) {
        PayoutStatus.PAID -> Triple(StatusPaidBg, StatusPaidGreen, strings.paidLabel)
        PayoutStatus.PENDING -> Triple(StatusPendingBg, StatusPendingYellow, strings.pendingLabel)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CommitteeStatusBadge(status: CommitteeStatus, strings: UiStrings) {
    val (bg, fg, label) = when (status) {
        CommitteeStatus.ACTIVE -> Triple(StatusPaidBg, StatusPaidGreen, strings.activeLabel)
        CommitteeStatus.COMPLETED -> Triple(EmeraldContainer, EmeraldPrimary, strings.completedLabel)
        CommitteeStatus.ARCHIVED -> Triple(StatusPendingBg, StatusPendingYellow, strings.archivedLabel)
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CollectionDonutChart(
    collected: Long,
    target: Long,
    paidMembers: Int,
    partialMembers: Int,
    unpaidMembers: Int,
    strings: UiStrings,
    modifier: Modifier = Modifier
) {
    val ratio = if (target > 0L) (collected.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (ratio * 100).toInt()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = StatusPaidGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * ratio,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = StatusPaidGreen
                    )
                    Text(
                        text = strings.paidLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.collectionProgress,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LegendRow(color = StatusPaidGreen, label = "${strings.paidMembers}: $paidMembers")
                LegendRow(color = StatusPendingYellow, label = "${strings.partialLabel}: $partialMembers")
                LegendRow(color = StatusUnpaidRed, label = "${strings.unpaidMembers}: $unpaidMembers")
                Text(
                    text = "${FormatUtils.formatPkr(collected)} / ${FormatUtils.formatPkr(target)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthlyCollectionBarChart(
    points: List<MonthlyChartPoint>,
    title: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val visiblePoints = points.take(12)
    val maxVal = visiblePoints.maxOfOrNull { maxOf(it.target, it.collected) }?.coerceAtLeast(1L) ?: 1L
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = EmeraldPrimary
    val goldColor = GoldAccent

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val count = visiblePoints.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.56f).coerceAtMost(28.dp.toPx())

                visiblePoints.forEachIndexed { idx, pt ->
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val targetHeight = (pt.target.toFloat() / maxVal.toFloat()) * size.height
                    val collectedHeight = (pt.collected.toFloat() / maxVal.toFloat()) * size.height

                    // Target background bar
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, size.height - targetHeight),
                        size = Size(barWidth, targetHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Collected bar
                    if (collectedHeight > 0f) {
                        val barColor = if (pt.collected >= pt.target && pt.target > 0L) primaryColor else goldColor
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, size.height - collectedHeight),
                            size = Size(barWidth, collectedHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                visiblePoints.forEach { pt ->
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MonthSelectorBar(
    committee: Committee,
    selectedMonthKey: String,
    strings: UiStrings,
    onSelectMonth: (String) -> Unit
) {
    val months = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${strings.collectionWindowLabel}: ${
                        MonthUtils.formatCollectionWindow(
                            selectedMonthKey,
                            committee.collectionDayStart,
                            committee.collectionDayEnd
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEachIndexed { idx, monthKey ->
                val selected = monthKey == selectedMonthKey
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMonth(monthKey) },
                    label = {
                        Text(
                            text = "M${idx + 1} • ${MonthUtils.formatMonthDisplay(monthKey)}",
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("month_chip_$monthKey")
                )
            }
        }
    }
}

@Composable
fun EmptyCommitteePrompt(
    strings: UiStrings,
    onCreateCommittee: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_committee_empty),
                    contentDescription = "Create committee illustration",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gm_committee_fg),
                        contentDescription = "GM HASMANI Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = strings.noCommitteeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.noCommitteeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onCreateCommittee,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_first_committee_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.createFirstCommittee, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
