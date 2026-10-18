package com.payvoice.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payvoice.app.data.local.entity.BankAccountEntity
import com.payvoice.app.data.preferences.AppSettings
import com.payvoice.app.ui.theme.PayVoiceIncomeGreen
import com.payvoice.app.ui.theme.PayVoicePrimary
import com.payvoice.app.ui.theme.PayVoiceSecondary
import com.payvoice.app.utils.BatteryOptimizationHelper
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    bankAccounts: List<BankAccountEntity>,
    onSetLanguage: (String) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSetRepeatTwice: (Boolean) -> Unit,
    onSetMinAmount: (Double) -> Unit,
    onSetQuietHours: (Boolean, Int, Int) -> Unit,
    onSetAnnounceSender: (Boolean) -> Unit,
    onAddBankAccount: (String, String) -> Unit,
    onToggleAccount: (BankAccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onSetAppLock: (Boolean, String) -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showBatteryTipsDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showMinAmountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Soundbox Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure voice, speech, quiet hours, and accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Language Selection
        item {
            SettingsCard(title = "Announcement Language", icon = Icons.Default.Language) {
                Column {
                    LanguageOption(
                        label = "English (Five hundred rupees received)",
                        isSelected = settings.language == "en",
                        onClick = { onSetLanguage("en") }
                    )
                    LanguageOption(
                        label = "हिन्दी (पाँच सौ रुपये प्राप्त हुए)",
                        isSelected = settings.language == "hi",
                        onClick = { onSetLanguage("hi") }
                    )
                    LanguageOption(
                        label = "ગુજરાતી (પાંચસો રૂપિયા મળ્યા)",
                        isSelected = settings.language == "gu",
                        onClick = { onSetLanguage("gu") }
                    )
                }
            }
        }

        // 2. Volume & Voice Control
        item {
            SettingsCard(title = "Speech Volume & Playback", icon = Icons.Default.VolumeUp) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume: ${(settings.speechVolume * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = onTestVoice,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test", fontSize = 12.sp)
                        }
                    }

                    Slider(
                        value = settings.speechVolume,
                        onValueChange = onSetVolume,
                        valueRange = 0.1f..1.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = PayVoicePrimary,
                            activeTrackColor = PayVoicePrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("speech_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Repeat Announcement Twice",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Speaks the payment amount two times in noisy shops",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.repeatAnnouncementTwice,
                            onCheckedChange = onSetRepeatTwice,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PayVoiceIncomeGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Announce Payer Name",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (settings.announceSenderName) "Amount + Customer Name" else "Amount Only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.announceSenderName,
                            onCheckedChange = onSetAnnounceSender,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PayVoiceIncomeGreen
                            )
                        )
                    }
                }
            }
        }

        // 3. Minimum Amount & Quiet Hours
        item {
            SettingsCard(title = "Filters & Quiet Hours", icon = Icons.Default.NightsStay) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMinAmountDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Minimum Amount to Announce", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (settings.minAmountToAnnounce <= 0.0) "All amounts (₹0+)" else "₹${settings.minAmountToAnnounce.toInt()}+",
                                style = MaterialTheme.typography.bodySmall,
                                color = PayVoicePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(onClick = { showMinAmountDialog = true }, shape = RoundedCornerShape(10.dp)) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Quiet Hours", style = MaterialTheme.typography.titleMedium)
                            val startH = settings.quietHoursStartMinutes / 60
                            val startM = settings.quietHoursStartMinutes % 60
                            val endH = settings.quietHoursEndMinutes / 60
                            val endM = settings.quietHoursEndMinutes % 60
                            val timeStr = String.format("%02d:%02d to %02d:%02d", startH, startM, endH, endM)
                            Text(
                                text = if (settings.quietHoursEnabled) "Active ($timeStr)" else "Mute soundbox during night hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.quietHoursEnabled,
                            onCheckedChange = { enabled ->
                                onSetQuietHours(enabled, settings.quietHoursStartMinutes, settings.quietHoursEndMinutes)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PayVoiceIncomeGreen
                            )
                        )
                    }

                    if (settings.quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { showQuietHoursDialog = true }) {
                            Text("Change Quiet Hours Timing", color = PayVoicePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Manage Bank Accounts
        item {
            SettingsCard(title = "Tracked Bank Accounts", icon = Icons.Default.Person) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bankAccounts.isEmpty()) "All accounts tracked" else "${bankAccounts.size} account(s) configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PayVoicePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Account", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bankAccounts.isEmpty()) {
                        Text(
                            text = "No specific accounts restricted. PayVoice will announce credits from any bank SMS matching credit patterns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bankAccounts.forEach { acc ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = acc.bankName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "A/c ending in ${acc.last4Digits}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = acc.isEnabled,
                                                onCheckedChange = { onToggleAccount(acc) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = PayVoiceIncomeGreen
                                                )
                                            )
                                            IconButton(onClick = { onDeleteAccount(acc.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
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

        // 5. Battery Optimization & Background Reliability
        item {
            SettingsCard(title = "Background Reliability", icon = Icons.Default.BatteryAlert) {
                Column {
                    Text(
                        text = "To keep PayVoice announcing 24/7 when the phone is locked, ensure battery saver restrictions are disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { BatteryOptimizationHelper.openBatteryOptimizationSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = PayVoicePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Settings", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showBatteryTipsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Brand Tips", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 6. Security (App Lock)
        item {
            SettingsCard(title = "App Lock (PIN)", icon = Icons.Default.Security) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lock App with PIN", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (settings.appLockEnabled) "App lock is active" else "Protect transaction history & settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.appLockEnabled) PayVoiceIncomeGreen else PayVoicePrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (settings.appLockEnabled) "Change" else "Set PIN", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        var bankName by remember { mutableStateOf("") }
        var last4 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Bank Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the bank name and last 4 digits of the account number:")
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name (e.g. SBI, HDFC)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("Last 4 digits") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bankName.isNotBlank() && last4.length == 4) {
                            onAddBankAccount(bankName.trim(), last4.trim())
                            showAddAccountDialog = false
                        }
                    },
                    enabled = bankName.isNotBlank() && last4.length == 4
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMinAmountDialog) {
        var minInput by remember { mutableStateOf(settings.minAmountToAnnounce.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showMinAmountDialog = false },
            title = { Text("Minimum Amount to Announce") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payments below this amount will not be spoken out loud.")
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it },
                        label = { Text("Min Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 10, 50, 100, 500).forEach { amt ->
                            OutlinedButton(
                                onClick = { minInput = amt.toString() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("₹$amt", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = minInput.toDoubleOrNull() ?: 0.0
                    onSetMinAmount(amt)
                    showMinAmountDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMinAmountDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuietHoursDialog) {
        var startHour by remember { mutableIntStateOf(settings.quietHoursStartMinutes / 60) }
        var endHour by remember { mutableIntStateOf(settings.quietHoursEndMinutes / 60) }

        AlertDialog(
            onDismissRequest = { showQuietHoursDialog = false },
            title = { Text("Set Quiet Hours") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Soundbox will remain silent during these hours:")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start: ${String.format("%02d:00", startHour)}")
                        Row {
                            IconButton(onClick = { startHour = (startHour + 23) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            IconButton(onClick = { startHour = (startHour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End: ${String.format("%02d:00", endHour)}")
                        Row {
                            IconButton(onClick = { endHour = (endHour + 23) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            IconButton(onClick = { endHour = (endHour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSetQuietHours(true, startHour * 60, endHour * 60)
                    showQuietHoursDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuietHoursDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBatteryTipsDialog) {
        val tips = BatteryOptimizationHelper.getOemTips()
        var selectedBrandIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showBatteryTipsDialog = false },
            title = { Text("Battery Saver Tips for Phones") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Chinese phone manufacturers (Xiaomi, Realme, Oppo, Vivo) aggressively close background services. Follow these steps:")
                    Spacer(modifier = Modifier.height(10.dp))

                    tips.forEachIndexed { index, tip ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedBrandIndex = if (selectedBrandIndex == index) -1 else index },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedBrandIndex == index) PayVoicePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = tip.brandName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedBrandIndex == index) PayVoicePrimary else MaterialTheme.colorScheme.onSurface
                                )
                                AnimatedVisibility(visible = selectedBrandIndex == index) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        tip.steps.forEach { step ->
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
                    showBatteryTipsDialog = false
                }) {
                    Text("Open Battery Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryTipsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (settings.appLockEnabled) "Change or Remove PIN" else "Set 4-Digit PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit PIN:")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            onSetAppLock(true, pinInput)
                            showPinDialog = false
                        }
                    },
                    enabled = pinInput.length == 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                if (settings.appLockEnabled) {
                    TextButton(onClick = {
                        onSetAppLock(false, "")
                        showPinDialog = false
                    }) {
                        Text("Disable Lock", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PayVoicePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = PayVoicePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PayVoicePrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
