# PayVoice 🔊

PayVoice is a 100% offline Soundbox Android app built for small shop owners and merchants. Whenever money is credited to the shop's bank account or received through UPI apps, PayVoice announces the amount out loud in natural spoken words (e.g., *"Five hundred rupees received"* / *"पाँच सौ रुपये प्राप्त हुए"* / *"પાંચસો રૂપિયા મળ્યા"*).

It operates completely offline without cloud servers, user accounts, analytics, ads, or internet permissions (`android.permission.INTERNET` is strictly absent).

---

## 🚀 Key Features

1. **Dual Credit Detection**:
   - **Bank SMS**: BroadcastReceiver parses real-time credit messages from Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Canara, Union Bank, etc.). Matches sender IDs case-insensitively with prefixes and suffixes (e.g. `VM-HDFCBK`, `AD-SBIINB-S`, `JD-ICICIB-T`).
   - **UPI Notifications**: Intercepts notifications from PhonePe, PhonePe Business, Paytm, Paytm for Business, BharatPe, Google Pay, Google Pay for Business, BHIM, CRED, and Amazon Pay.
2. **Cross-Channel Deduplication**:
   - Only treats messages as duplicates if one message is from SMS and the other is from a UPI notification for the same amount and account within 5 minutes.
   - Two SMS or two UPI notifications with the same amount are treated as separate customer payments and both are announced!
3. **Indian Currency Parser**:
   - Parses lakhs and crores with comma notation (e.g., `₹1,25,000`, `Rs. 4,000.00`, `INR 400`).
   - Handles paise accurately (`250 rupees 50 paise`).
   - Ignores available balance ("Avl Bal"), OTPs, promos, debit messages, and loan offers.
   - Ignores unknown/spam sender IDs to prevent fake payment triggers.
4. **Soundbox Foreground Service**:
   - Android 14+ `specialUse` foreground service with persistent status notification.
   - TextToSpeech in English, Hindi, and Gujarati with automatic fallback to English.
   - Brief `WakeLock` during speech to wake the device screen/CPU when locked.
   - `QUEUE_ADD` to queue and announce consecutive customer payments sequentially.
   - Restarts after device reboot (`BOOT_COMPLETED`).
5. **Interactive Testing on Home Screen**:
   - "Test Voice" button to verify audio settings.
   - "Test Payment (₹500)" button to simulate a live transaction, record it in Room, update the widget, and speak it.
6. **History & CSV Export**:
   - Filter by Today, Yesterday, This Week, This Month, or All Time.
   - 7-day daily income trend bar chart.
   - Export CSV report via Android `FileProvider` to WhatsApp, Email, or Google Drive.
7. **Merchant-Friendly Settings**:
   - Language selection (English, Hindi, Gujarati).
   - Speech volume slider and "Repeat Announcement Twice" toggle for noisy markets.
   - Quiet hours schedule (e.g., mute between 10:00 PM and 7:00 AM).
   - Minimum payment amount threshold.
   - Tracked bank accounts manager (by bank name and last 4 digits).
   - PIN App Lock for security.
   - Manufacturer-specific battery saver tips for Xiaomi, Oppo, Vivo, Realme, and Samsung.
8. **Home-Screen Widget**:
   - Shows "Today Received: ₹ X", number of payments, and the latest transaction.
   - Queries Room database directly, functioning even when the app is closed.

---

## 🛠️ Architecture & Tech Stack

- **Package**: `com.payvoice.app`
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM + Clean Architecture with Repository Pattern
- **Local Database**: Room 2.7 (KSP compiler)
- **Preferences**: Jetpack DataStore Preferences
- **Text-to-Speech**: Android Native TextToSpeech Engine with `UtteranceProgressListener`
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 36
- **Java Compatibility**: Java 17

---

## 📂 Project Structure

```
.
├── .github/
│   └── workflows/
│       └── build.yml
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/payvoice/app/
        │   │   ├── MainActivity.kt
        │   │   ├── PayVoiceApplication.kt
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── AppDatabase.kt
        │   │   │   │   ├── dao/
        │   │   │   │   │   ├── BankAccountDao.kt
        │   │   │   │   │   └── PaymentDao.kt
        │   │   │   │   └── entity/
        │   │   │   │       ├── BankAccountEntity.kt
        │   │   │   │       └── PaymentEntity.kt
        │   │   │   ├── preferences/
        │   │   │   │   ├── AppSettings.kt
        │   │   │   │   └── SettingsDataStore.kt
        │   │   │   └── repository/
        │   │   │       └── PaymentRepository.kt
        │   │   ├── domain/
        │   │   │   ├── parser/
        │   │   │   │   ├── ParsedPayment.kt
        │   │   │   │   └── PaymentParser.kt
        │   │   │   └── tts/
        │   │   │       └── SoundboxTtsManager.kt
        │   │   ├── receiver/
        │   │   │   ├── BootReceiver.kt
        │   │   │   └── SmsReceiver.kt
        │   │   ├── service/
        │   │   │   ├── NotificationListener.kt
        │   │   │   └── SoundboxService.kt
        │   │   ├── ui/
        │   │   │   ├── history/
        │   │   │   │   └── HistoryScreen.kt
        │   │   │   ├── home/
        │   │   │   │   └── HomeScreen.kt
        │   │   │   ├── navigation/
        │   │   │   │   └── NavigationState.kt
        │   │   │   ├── onboarding/
        │   │   │   │   └── OnboardingScreen.kt
        │   │   │   ├── pin/
        │   │   │   │   └── PinLockScreen.kt
        │   │   │   ├── settings/
        │   │   │   │   └── SettingsScreen.kt
        │   │   │   ├── theme/
        │   │   │   │   ├── Color.kt
        │   │   │   │   ├── Theme.kt
        │   │   │   │   └── Type.kt
        │   │   │   └── viewmodel/
        │   │   │       └── MainViewModel.kt
        │   │   ├── utils/
        │   │   │   ├── BatteryOptimizationHelper.kt
        │   │   │   ├── CsvExporter.kt
        │   │   │   ├── Deduplicator.kt
        │   │   │   └── NumberToWordsConverter.kt
        │   │   └── widget/
        │   │       └── PayVoiceWidgetProvider.kt
        │   └── res/
        │       ├── drawable/
        │       │   ├── ic_launcher_background.xml
        │       │   ├── ic_launcher_foreground.xml
        │       │   ├── widget_background.xml
        │       │   └── widget_status_active.xml
        │       ├── layout/
        │       │   └── widget_payvoice.xml
        │       ├── values/
        │       │   └── strings.xml
        │       └── xml/
        │           ├── file_paths.xml
        │           └── payvoice_widget_info.xml
        └── test/java/com/payvoice/app/
            └── PayVoiceComprehensiveTest.kt
```

---

## 📋 How to Grant Permissions

1. **SMS Permission (`RECEIVE_SMS`, `READ_SMS`)**:
   - Tap **"Allow SMS"** in Step 2 of onboarding.
   - Accept the Android system dialog to allow reading incoming bank credit SMS.
2. **Notification Access (Notification Listener)**:
   - Tap **"Enable Access"** in Step 2 of onboarding.
   - In Android's **Device & App Notifications** settings, locate **PayVoice** and toggle **Allow Notification Access** to ON.
   - *Android 13+ note*: If Android displays "Restricted setting", go to Phone Settings > Apps > PayVoice > tap 3 dots (top right) > "Allow restricted settings", then re-enable notification access.
3. **Soundbox Notification (`POST_NOTIFICATIONS`)**:
   - Tap **"Allow Alerts"** in Step 2 to allow the persistent foreground service notification.
4. **Disable Battery Optimization**:
   - Tap **"Disable"** in Step 2.
   - Select "Don't optimize" / "Unrestricted" so Android keeps the soundbox active in the background.

---

## 🔨 Building the Project

- **Build Debug APK locally**:
  ```sh
  gradle assembleDebug --stacktrace
  ```
- **Run Tests**:
  ```sh
  gradle :app:testDebugUnitTest
  ```
- **GitHub Actions**:
  Pushes to any branch automatically trigger `.github/workflows/build.yml` which builds `app-debug.apk` and publishes it as the `payvoice-debug-apk` artifact.
