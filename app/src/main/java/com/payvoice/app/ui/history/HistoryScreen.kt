package com.payvoice.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payvoice.app.data.local.entity.PaymentEntity
import com.payvoice.app.ui.home.PaymentItemCard
import com.payvoice.app.ui.theme.PayVoiceIncomeGreen
import com.payvoice.app.ui.theme.PayVoicePrimary
import com.payvoice.app.ui.viewmodel.DailyTotal
import com.payvoice.app.ui.viewmodel.HistoryFilter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    payments: List<PaymentEntity>,
    dailyTotals: List<DailyTotal>,
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = payments.sumOf { it.amount }
    val count = payments.size

    val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }
    val formattedTotal = indianFormat.format(totalAmount).replace("₹", "₹ ")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment History",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track, review, and export all received credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onExportCsv,
                    enabled = payments.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PayVoiceIncomeGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem("Today", selectedFilter == HistoryFilter.TODAY) {
                    onSelectFilter(HistoryFilter.TODAY)
                }
                FilterChipItem("Yesterday", selectedFilter == HistoryFilter.YESTERDAY) {
                    onSelectFilter(HistoryFilter.YESTERDAY)
                }
                FilterChipItem("This Week", selectedFilter == HistoryFilter.THIS_WEEK) {
                    onSelectFilter(HistoryFilter.THIS_WEEK)
                }
                FilterChipItem("This Month", selectedFilter == HistoryFilter.THIS_MONTH) {
                    onSelectFilter(HistoryFilter.THIS_MONTH)
                }
                FilterChipItem("All Records", selectedFilter == HistoryFilter.ALL) {
                    onSelectFilter(HistoryFilter.ALL)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Received (${filterName(selectedFilter)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedTotal,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PayVoiceIncomeGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PayVoicePrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$count ${if (count == 1) "Credit" else "Credits"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PayVoicePrimary
                        )
                    }
                }
            }
        }

        if (dailyTotals.isNotEmpty() && dailyTotals.any { it.totalAmount > 0 }) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Last 7 Days Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily income breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        SimpleBarChart(
                            dailyTotals = dailyTotals,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Transaction Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No payments in this period",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch filters or wait for incoming payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(payments, key = { it.id }) { payment ->
                PaymentItemCard(payment = payment)
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PayVoicePrimary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun SimpleBarChart(
    dailyTotals: List<DailyTotal>,
    modifier: Modifier = Modifier
) {
    val maxVal = (dailyTotals.maxOfOrNull { it.totalAmount } ?: 1.0).coerceAtLeast(100.0)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val barCount = dailyTotals.size
            val spacing = size.width / (barCount * 3f)
            val barWidth = (size.width - (spacing * (barCount + 1))) / barCount
            val chartHeight = size.height - 24f

            dailyTotals.forEachIndexed { index, item ->
                val x = spacing + index * (barWidth + spacing)
                val heightPercent = (item.totalAmount / maxVal).toFloat().coerceIn(0.04f, 1f)
                val barH = chartHeight * heightPercent
                val y = chartHeight - barH

                drawRoundRect(
                    color = Color(0xFFF0ECF6),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                drawRoundRect(
                    color = if (item.dayLabel == "Today") PayVoiceIncomeGreen else PayVoicePrimary,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyTotals.forEach { item ->
                Text(
                    text = item.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (item.dayLabel == "Today") PayVoiceIncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (item.dayLabel == "Today") FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun filterName(filter: HistoryFilter): String {
    return when (filter) {
        HistoryFilter.TODAY -> "Today"
        HistoryFilter.YESTERDAY -> "Yesterday"
        HistoryFilter.THIS_WEEK -> "This Week"
        HistoryFilter.THIS_MONTH -> "This Month"
        HistoryFilter.ALL -> "All Time"
    }
}
