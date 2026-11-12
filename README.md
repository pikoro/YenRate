# YenRate 💴

**YenRate** is a lightweight, modern Android application for tracking live **USD / JPY (US Dollar to Japanese Yen)** exchange rates in real time, featuring 24-hour rate trend graphs and home screen widgets.

Designed for travelers, expats, and financial observers in Japan, **YenRate** provides both the live market interbank FX rate and an estimated **"Real Rate"**—reflecting the actual currency exchange rate you can expect at local businesses and exchange kiosks after typical service margins.

---

## ✨ Features

- 📊 **Live USD/JPY Exchange Rate**: Real-time rate updates with clear positive/negative change indicators (`+` / `-` and `%` change).
- 💛 **"Real" Local Exchange Rate**: Displays the practical retail exchange rate in prominent yellow text alongside the live interbank rate.
- 📈 **24-Hour Trend Chart**: Line graph displaying rate fluctuations over the past 24 hours, formatted with High/Low indicators, gridlines, and X/Y time & rate scale axes.
- 📲 **Home Screen Widgets**:
  - **Standard (3x2) Widget**: Shows live rate, real local exchange rate, 24-hour trend line chart, rate change arrow indicator, last updated time, and a quick manual refresh button.
  - **Compact (1x3) Widget**: Space-efficient horizontal widget displaying currency pair, change indicator arrow, live exchange rate, and real rate.
- ⏰ **Automatic Hourly Background Sync**: Fetches rates in the background once per hour with minimal battery impact.
- 🇯🇵 **Full Bilingual Localization**: Automatically adapts UI and widget strings to **English** or **Japanese** based on the device's system language settings.

---

## 📱 Home Screen Widgets

| Widget Type | Size | Key Elements |
| :--- | :--- | :--- |
| **Standard Widget** | `3x2` | USD/JPY pair label, live rate, yellow real rate, 24h trend graph with X/Y scales, change arrow (▲/▼), last update timestamp, manual refresh button |
| **Compact Widget** | `1x3` | Currency pair label, live exchange rate, yellow real rate (at 50% text size), change arrow (▲/▼) |

### How to Add a Widget
1. Long-press an empty space on your Android Home screen.
2. Tap **Widgets**.
3. Search for **YenRate**.
4. Select either the **Standard (3x2)** or **Compact (1x3)** widget and drag it onto your screen.

---

## 🛠 Tech Stack

- **Language**: 100% [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Background Processing**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (`CoroutineWorker`)
- **Widget Engine**: Android AppWidget Provider & RemoteViews
- **Concurrency**: Kotlin Coroutines & Flow
- **Data Storage**: `SharedPreferences` with JSON serialization for 24-hour rate points
- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 37 (Android 16+)

---

## 🚀 Building & Installation

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 11 or higher
- Android SDK 37

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/dccpikoro/YenRate.git
   cd YenRate
   ```
2. Build the Debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. Build the Release APK:
   ```bash
   ./gradlew :app:assembleRelease
   ```

---

## 👨‍💻 Author

- **Name**: Aaron Anderson
- **Email**: dccpikoro@gmail.com

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more details.
