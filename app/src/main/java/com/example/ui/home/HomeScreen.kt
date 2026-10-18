package com.example.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PaymentEntity
import com.example.data.preferences.AppSettings
import com.example.ui.theme.PayVoiceIncomeGreen
import com.example.ui.theme.PayVoiceIncomeGreenContainer
import com.example.ui.theme.PayVoicePrimary
import com.example.ui.theme.PayVoiceSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    todayPayments: List<PaymentEntity>,
    settings: AppSettings,
    onToggleSoundbox: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onSimulatePayment: (Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = todayPayments.sumOf { it.amount }
    val paymentCount = todayPayments.size

    val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }
    val formattedTotal = indianFormat.format(totalAmount).replace("₹", "₹ ")

    val cardBgColor by animateColorAsState(
        targetValue = if (settings.isSoundboxActive) PayVoicePrimary else Color(0xFF555261),
        label = "soundbox_card_bg"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Big Today Received Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_received_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today Received",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedTotal,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        // Badge showing payment count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$paymentCount ${if (paymentCount == 1) "Payment" else "Payments"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Soundbox ON/OFF Switch row inside the card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.isSoundboxActive) PayVoiceIncomeGreen else Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (settings.isSoundboxActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (settings.isSoundboxActive) "Soundbox Active" else "Soundbox Stopped",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (settings.isSoundboxActive) "Listening for incoming credits" else "Turn ON to hear payments",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isSoundboxActive,
                                onCheckedChange = onToggleSoundbox,
                                modifier = Modifier.testTag("soundbox_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PayVoiceIncomeGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Action Buttons (Test Voice & Quick Demo)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTestVoice,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("test_voice_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PayVoiceSecondary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Voice",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { onSimulatePayment(500.0, "Ramesh Store") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("simulate_payment_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PayVoicePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+₹500 Test",
                        style = MaterialTheme.typography.titleMedium,
                        color = PayVoicePrimary
                    )
                }
            }
        }

        // 3. Today's Payments Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Payments",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${todayPayments.size} received",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Payments List or Empty State
        if (todayPayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PayVoicePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = PayVoicePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No payments received yet today",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Keep PayVoice running. When customers pay via UPI or Bank transfer, you'll hear the amount spoken out loud instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todayPayments, key = { it.id }) { payment ->
                PaymentItemCard(payment = payment)
            }
        }
    }
}

@Composable
fun PaymentItemCard(
    payment: PaymentEntity,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val formattedTime = timeFormat.format(Date(payment.timestamp))

    val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }
    val formattedAmount = "+ " + indianFormat.format(payment.amount).replace("₹", "₹ ")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_item_${payment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source icon
            val isUpi = payment.source.startsWith("UPI")
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isUpi) PayVoicePrimary.copy(alpha = 0.12f) else PayVoiceSecondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUpi) Icons.Default.Notifications else Icons.Default.Sms,
                    contentDescription = null,
                    tint = if (isUpi) PayVoicePrimary else PayVoiceSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${payment.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PayVoicePrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (payment.accountLast4.isNotBlank()) {
                        Text(
                            text = " • A/c *${payment.accountLast4}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount in Vibrant Income Green
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PayVoiceIncomeGreen
            )
        }
    }
}
