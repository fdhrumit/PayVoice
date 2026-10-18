package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.SoundboxService
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.MainTab
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.pin.PinLockScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.PayVoiceIncomeGreen
import com.example.ui.theme.PayVoicePrimary
import com.example.ui.theme.PayVoiceTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PayVoiceTheme {
                PayVoiceApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure foreground service is running if soundbox is active
        val settings = viewModel.settings.value
        if (settings.isSoundboxActive && settings.isOnboardingCompleted) {
            SoundboxService.start(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayVoiceApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val todayPayments by viewModel.todayPayments.collectAsStateWithLifecycle()
    val filteredPayments by viewModel.filteredPayments.collectAsStateWithLifecycle()
    val dailyTotals by viewModel.last7DaysTotals.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var isPinLocked by remember { mutableStateOf(settings.appLockEnabled) }

    LaunchedEffect(settings.appLockEnabled) {
        if (settings.appLockEnabled && isPinLocked) {
            isPinLocked = true
        }
    }

    if (!settings.isOnboardingCompleted) {
        OnboardingScreen(
            onComplete = { language, accounts ->
                viewModel.setLanguage(language)
                accounts.forEach { acc ->
                    viewModel.addBankAccount(acc.bankName, acc.last4)
                }
                viewModel.completeOnboarding()
            }
        )
    } else if (settings.appLockEnabled && isPinLocked) {
        PinLockScreen(
            correctPin = settings.appLockPin,
            onUnlocked = { isPinLocked = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PayVoicePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = PayVoicePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PayVoice",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PayVoicePrimary
                                )
                                Text(
                                    text = if (settings.isSoundboxActive) "Active Soundbox" else "Soundbox Muted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (settings.isSoundboxActive) PayVoiceIncomeGreen else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick active status indicator pill
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (settings.isSoundboxActive) PayVoiceIncomeGreen.copy(alpha = 0.15f)
                                    else Color.Gray.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.isSoundboxActive) PayVoiceIncomeGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (settings.isSoundboxActive) "ON" else "OFF",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.isSoundboxActive) PayVoiceIncomeGreen else Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.HOME,
                        onClick = { selectedTab = MainTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PayVoicePrimary,
                            selectedTextColor = PayVoicePrimary,
                            indicatorColor = PayVoicePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.HISTORY,
                        onClick = { selectedTab = MainTab.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PayVoicePrimary,
                            selectedTextColor = PayVoicePrimary,
                            indicatorColor = PayVoicePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_history")
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { selectedTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PayVoicePrimary,
                            selectedTextColor = PayVoicePrimary,
                            indicatorColor = PayVoicePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main_tabs",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        todayPayments = todayPayments,
                        settings = settings,
                        onToggleSoundbox = { viewModel.toggleSoundbox(it) },
                        onTestVoice = { viewModel.testVoice() },
                        onSimulatePayment = { amt, sender -> viewModel.simulatePayment(amt, sender) }
                    )
                    MainTab.HISTORY -> HistoryScreen(
                        payments = filteredPayments,
                        dailyTotals = dailyTotals,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { viewModel.setFilter(it) },
                        onExportCsv = { viewModel.exportFilteredCsv(context) }
                    )
                    MainTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        bankAccounts = bankAccounts,
                        onSetLanguage = { viewModel.setLanguage(it) },
                        onSetVolume = { viewModel.setSpeechVolume(it) },
                        onSetRepeatTwice = { viewModel.setRepeatTwice(it) },
                        onSetMinAmount = { viewModel.setMinAmount(it) },
                        onSetQuietHours = { enabled, s, e -> viewModel.setQuietHours(enabled, s, e) },
                        onSetAnnounceSender = { viewModel.setAnnounceSender(it) },
                        onAddBankAccount = { name, last4 -> viewModel.addBankAccount(name, last4) },
                        onToggleAccount = { viewModel.toggleBankAccount(it) },
                        onDeleteAccount = { viewModel.deleteBankAccount(it) },
                        onSetAppLock = { enabled, pin -> viewModel.setAppLock(enabled, pin) },
                        onTestVoice = { viewModel.testVoice() }
                    )
                }
            }
        }
    }
}
