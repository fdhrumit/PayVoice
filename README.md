# PayVoice 🔊

PayVoice is a 100% offline Soundbox Android app built for small shop owners and merchants. Whenever money is credited to the shop's bank account or received through UPI apps, PayVoice announces the amount out loud in natural spoken words (e.g., *"Five hundred rupees received"* / *"पाँच सौ रुपये प्राप्त हुए"* / *"પાંચસો રૂપિયા મળ્યા"*).

It operates completely offline without cloud servers, user accounts, analytics, ads, or internet permissions (`android.permission.INTERNET` is strictly absent).

---

## 🚀 Key Features

1. **Dual Credit Detection**:
   - **Bank SMS**: BroadcastReceiver parses real-time credit messages from Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, Canara, Union Bank, etc.).
   - **UPI Notifications**: NotificationListenerService intercepts notifications from Google Pay, PhonePe, Paytm, BHIM, CRED, and Amazon Pay.
2. **Indian Currency Parser**:
   - Parses lakhs and crores with comma notation (e.g., `₹1,25,000`, `Rs. 4,000.00`, `INR 400`).
   - Handles paise accurately (`250 rupees 50 paise`).
   - Ignores available balance ("Avl Bal"), OTPs, promos, debit messages, and loan offers.
   - Ignores unknown/spam sender IDs to prevent fake payment triggers.
3. **Soundbox Foreground Service**:
   - Special-use foreground service with persistent status notification.
   - TextToSpeech in English, Hindi, and Gujarati with automatic fallback to English.
   - Brief `WakeLock` during speech to wake the device screen/CPU when locked.
   - `QUEUE_ADD` to queue and announce consecutive customer payments sequentially.
   - Restarts after device reboot (`BOOT_COMPLETED`).
4. **Intelligent 5-Minute Deduplication**:
   - Prevents duplicate speech if both an SMS and UPI notification arrive for the same payment.
5. **History & CSV Export**:
   - Filter by Today, Yesterday, This Week, This Month, or All Time.
   - 7-day daily income trend bar chart.
   - Export CSV report via Android `FileProvider` to WhatsApp, Email, or Google Drive.
6. **Merchant-Friendly Settings**:
   - Language selection (English, Hindi, Gujarati).
   - Speech volume slider and "Repeat Announcement Twice" toggle for noisy markets.
   - Quiet hours schedule (e.g., mute between 10:00 PM and 7:00 AM).
   - Minimum payment amount threshold.
   - Tracked bank accounts manager (by bank name and last 4 digits).
   - PIN App Lock for security.
   - Manufacturer-specific battery saver tips for Xiaomi, Oppo, Vivo, Realme, and Samsung.
7. **Home-Screen Widget**:
   - Shows "Today Received: ₹ X", number of payments, and the latest transaction.
   - Queries Room database directly, functioning even when the app is closed.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM + Clean Architecture with Repository Pattern
- **Local Database**: Room 2.7 (KSP compiler)
- **Preferences**: Jetpack DataStore Preferences
- **Text-to-Speech**: Android Native TextToSpeech Engine with `UtteranceProgressListener`
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)

---

## 📂 Project Structure

```
.
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── PayVoiceApplication.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── BankAccountDao.kt
│   │   │   │   │   │   │   └── PaymentDao.kt
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │       ├── BankAccountEntity.kt
│   │   │   │   │   │       └── PaymentEntity.kt
│   │   │   │   │   ├── preferences/
│   │   │   │   │   │   ├── AppSettings.kt
│   │   │   │   │   │   └── SettingsDataStore.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── PaymentRepository.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── parser/
│   │   │   │   │   │   ├── ParsedPayment.kt
│   │   │   │   │   │   └── PaymentParser.kt
│   │   │   │   │   └── tts/
│   │   │   │   │       └── SoundboxTtsManager.kt
│   │   │   │   ├── receiver/
│   │   │   │   │   ├── BootReceiver.kt
│   │   │   │   │   └── SmsReceiver.kt
│   │   │   │   ├── service/
│   │   │   │   │   ├── NotificationListener.kt
│   │   │   │   │   └── SoundboxService.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── history/
│   │   │   │   │   │   └── HistoryScreen.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   └── HomeScreen.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavigationState.kt
│   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   └── OnboardingScreen.kt
│   │   │   │   │   ├── pin/
│   │   │   │   │   │   └── PinLockScreen.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   └── viewmodel/
│   │   │   │   │       └── MainViewModel.kt
│   │   │   │   ├── utils/
│   │   │   │   │   ├── BatteryOptimizationHelper.kt
│   │   │   │   │   ├── CsvExporter.kt
│   │   │   │   │   ├── Deduplicator.kt
│   │   │   │   │   └── NumberToWordsConverter.kt
│   │   │   │   └── widget/
│   │   │   │       └── PayVoiceWidgetProvider.kt
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       │   ├── ic_launcher_background.xml
│   │   │       │   ├── ic_launcher_foreground.xml
│   │   │       │   ├── widget_background.xml
│   │   │       │   └── widget_status_active.xml
│   │   │       ├── layout/
│   │   │       │   └── widget_payvoice.xml
│   │   │       ├── values/
│   │   │       │   └── strings.xml
│   │   │       └── xml/
│   │   │           ├── file_paths.xml
│   │   │           └── payvoice_widget_info.xml
│   │   └── test/java/com/example/
│   │       ├── ExampleRobolectricTest.kt
│   │       └── PayVoiceComprehensiveTest.kt
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## 📋 How to Grant Permissions

When you open PayVoice for the first time, the Onboarding Checklist will guide you through granting each permission:

1. **SMS Permission (`RECEIVE_SMS`, `READ_SMS`)**:
   - Tap **"Allow SMS"** in Step 2.
   - Accept the Android system dialog to allow reading incoming bank credit SMS.
2. **Notification Access (Notification Listener)**:
   - Tap **"Enable Access"** in Step 2.
   - The app opens Android's **Device & App Notifications** settings.
   - Locate **PayVoice** in the list and toggle **Allow Notification Access** to ON.
   - *Note on Android 13+*: If Android displays "Restricted setting", go to Phone Settings > Apps > PayVoice > tap 3 dots (top right) > "Allow restricted settings", then re-enable notification access.
3. **Soundbox Notification (`POST_NOTIFICATIONS`)**:
   - Tap **"Allow Alerts"** in Step 2 to allow the persistent foreground service notification.
4. **Disable Battery Optimization**:
   - Tap **"Disable"** in Step 2.
   - Choose "Allow" or "Don't optimize" so Android does not kill the soundbox when your phone screen turns off.
   - For Xiaomi, Oppo, Vivo, or Realme devices, check the **Brand Tips** in the Settings tab to turn ON Autostart and set Battery Saver to "No restrictions".

---

## 🔨 Building and Running

1. **Build the Debug APK**:
   ```sh
   gradle :app:assembleDebug
   ```

2. **Run Unit and Robolectric Tests**:
   ```sh
   gradle :app:testDebugUnitTest
   ```
