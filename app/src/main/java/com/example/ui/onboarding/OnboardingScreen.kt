package com.example.ui.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.NotificationListener
import com.example.ui.theme.PayVoiceIncomeGreen
import com.example.ui.theme.PayVoicePrimary
import com.example.ui.theme.PayVoiceSecondary
import com.example.utils.BatteryOptimizationHelper

data class TempAccount(val bankName: String, val last4: String)

@Composable
fun OnboardingScreen(
    onComplete: (language: String, accounts: List<TempAccount>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 2 permission states
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationAccess by remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var isBatteryOptIgnored by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    // Step 3 states
    var selectedLanguage by remember { mutableStateOf("en") }
    val accountList = remember { mutableStateListOf<TempAccount>() }
    var bankInput by remember { mutableStateOf("") }
    var last4Input by remember { mutableStateOf("") }

    // Multi-permission launcher
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermission = results[Manifest.permission.RECEIVE_SMS] == true ||
                results[Manifest.permission.READ_SMS] == true
    }

    val postNotifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotificationPermission = granted
    }

    // Refresh states on step change or resume
    LaunchedEffect(currentStep) {
        hasNotificationAccess = isNotificationServiceEnabled(context)
        isBatteryOptIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Step Indicator Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PayVoice Setup",
                    style = MaterialTheme.typography.titleLarge,
                    color = PayVoicePrimary
                )
                Text(
                    text = "Step $currentStep of 3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_steps",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    1 -> Step1Intro(
                        onNext = { currentStep = 2 }
                    )
                    2 -> Step2Permissions(
                        hasSms = hasSmsPermission,
                        onRequestSms = {
                            smsLauncher.launch(
                                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                            )
                        },
                        hasNotificationAccess = hasNotificationAccess,
                        onRequestNotificationAccess = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        hasPostNotif = hasPostNotificationPermission,
                        onRequestPostNotif = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        isBatteryOptIgnored = isBatteryOptIgnored,
                        onRequestBatteryOpt = {
                            BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
                        },
                        onRefreshCheck = {
                            hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                            hasNotificationAccess = isNotificationServiceEnabled(context)
                            isBatteryOptIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                        },
                        onNext = { currentStep = 3 }
                    )
                    3 -> Step3LanguageAndAccounts(
                        selectedLanguage = selectedLanguage,
                        onSelectLanguage = { selectedLanguage = it },
                        accountList = accountList,
                        bankInput = bankInput,
                        onBankInputChange = { bankInput = it },
                        last4Input = last4Input,
                        onLast4InputChange = { last4Input = it },
                        onAddAccount = {
                            if (bankInput.isNotBlank() && last4Input.length == 4) {
                                accountList.add(TempAccount(bankInput.trim(), last4Input.trim()))
                                bankInput = ""
                                last4Input = ""
                            }
                        },
                        onRemoveAccount = { accountList.remove(it) },
                        onComplete = {
                            onComplete(selectedLanguage, accountList.toList())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1Intro(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big Soundbox Visual
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(PayVoicePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = "PayVoice Soundbox",
                tint = PayVoicePrimary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Hear every payment\nyou receive.",
            style = MaterialTheme.typography.displayMedium,
            color = PayVoicePrimary,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A simple voice soundbox for shop owners.\nYour phone speaks incoming bank SMS and UPI credits out loud — completely offline with no cloud or account required.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Key Value Highlights
        HighlightRow(text = "Works 100% offline — zero internet permissions")
        Spacer(modifier = Modifier.height(10.dp))
        HighlightRow(text = "Speaks in English, Hindi, and Gujarati")
        Spacer(modifier = Modifier.height(10.dp))
        HighlightRow(text = "Detects PhonePe, Google Pay, Paytm, BHIM & Bank SMS")

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PayVoicePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HighlightRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PayVoiceIncomeGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PayVoiceIncomeGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Step2Permissions(
    hasSms: Boolean,
    onRequestSms: () -> Unit,
    hasNotificationAccess: Boolean,
    onRequestNotificationAccess: () -> Unit,
    hasPostNotif: Boolean,
    onRequestPostNotif: () -> Unit,
    isBatteryOptIgnored: Boolean,
    onRequestBatteryOpt: () -> Unit,
    onRefreshCheck: () -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Permission Checklist",
            style = MaterialTheme.typography.headlineLarge,
            color = PayVoicePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To announce payments as soon as they arrive, PayVoice needs these system permissions. Each item will show a green tick when ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. SMS Permission
        PermissionItemCard(
            title = "Bank SMS Permission",
            subtitle = "Reads incoming bank SMS credits (SBI, HDFC, ICICI, etc.)",
            icon = Icons.Default.Sms,
            isGranted = hasSms,
            actionLabel = "Allow SMS",
            onAction = onRequestSms
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Notification Access
        PermissionItemCard(
            title = "Notification Access",
            subtitle = "Listens to Google Pay, PhonePe, Paytm & UPI alerts",
            icon = Icons.Default.Notifications,
            isGranted = hasNotificationAccess,
            actionLabel = "Enable Access",
            onAction = onRequestNotificationAccess
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Post Notifications
        PermissionItemCard(
            title = "Soundbox Notification",
            subtitle = "Keeps the Soundbox running reliably in foreground",
            icon = Icons.Default.VolumeUp,
            isGranted = hasPostNotif,
            actionLabel = "Allow Alerts",
            onAction = onRequestPostNotif
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Disable Battery Optimization
        PermissionItemCard(
            title = "Disable Battery Optimization",
            subtitle = "Prevents Android from stopping Soundbox in background",
            icon = Icons.Default.PowerSettingsNew,
            isGranted = isBatteryOptIgnored,
            actionLabel = "Disable",
            onAction = onRequestBatteryOpt
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefreshCheck,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Refresh")
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
                    .testTag("permissions_continue_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PayVoicePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFF1FAF4) else MaterialTheme.colorScheme.surface
        ),
        border = if (isGranted) androidx.compose.foundation.BorderStroke(1.5.dp, PayVoiceIncomeGreen)
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) PayVoiceIncomeGreen.copy(alpha = 0.15f) else PayVoicePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) PayVoiceIncomeGreen else PayVoicePrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PayVoiceIncomeGreen)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Ready ✓",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PayVoicePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = actionLabel, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Step3LanguageAndAccounts(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    accountList: List<TempAccount>,
    bankInput: String,
    onBankInputChange: (String) -> Unit,
    last4Input: String,
    onLast4InputChange: (String) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (TempAccount) -> Unit,
    onComplete: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Voice & Bank Accounts",
            style = MaterialTheme.typography.headlineLarge,
            color = PayVoicePrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select your voice language and add the bank account(s) you wish to track.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Language Selection
        Text(
            text = "Choose Announcement Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LanguageCard(
                label = "English",
                sublabel = "500 rupees received",
                isSelected = selectedLanguage == "en",
                onClick = { onSelectLanguage("en") },
                modifier = Modifier.weight(1f)
            )
            LanguageCard(
                label = "हिन्दी",
                sublabel = "५०० रुपये प्राप्त हुए",
                isSelected = selectedLanguage == "hi",
                onClick = { onSelectLanguage("hi") },
                modifier = Modifier.weight(1f)
            )
            LanguageCard(
                label = "ગુજરાતી",
                sublabel = "૫૦૦ રૂપિયા મળ્યા",
                isSelected = selectedLanguage == "gu",
                onClick = { onSelectLanguage("gu") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bank Accounts Section
        Text(
            text = "Track Bank Accounts (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Enter your bank name and last 4 digits. If left empty, all incoming credits will be announced.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = bankInput,
                onValueChange = onBankInputChange,
                label = { Text("Bank Name (e.g. SBI)") },
                modifier = Modifier.weight(1.3f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = last4Input,
                onValueChange = { if (it.length <= 4) onLast4InputChange(it) },
                label = { Text("Last 4") },
                modifier = Modifier.weight(0.9f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = onAddAccount,
                enabled = bankInput.isNotBlank() && last4Input.length == 4,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (bankInput.isNotBlank() && last4Input.length == 4) PayVoicePrimary else Color.LightGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Bank Account",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of configured accounts
        if (accountList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (acc in accountList) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${acc.bankName}  •  A/c ending with ${acc.last4}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { onRemoveAccount(acc) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("complete_onboarding_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PayVoiceIncomeGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Activate Soundbox 🎉",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LanguageCard(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PayVoicePrimary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PayVoicePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PayVoicePrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (flat != null && flat.isNotEmpty()) {
        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}
