# Talon 🦅

**Talon** is a modern, privacy-focused Android application for tracking body composition and fitness progress. Designed to work with Bluetooth smart scales, it provides detailed insights into your health metrics without relying on cloud services.

## ✨ Features

### 📊 Comprehensive Metrics

Track more than just weight. Talon captures and analyzes:

- **Weight** & **BMI** (with color-coded categories)
- **Body Fat %**
- **Muscle Mass**
- **Water %**
- **Bone Mass**
- **Metabolic Age**
- **BMR** (Basal Metabolic Rate)
- **Impedance**

### 📈 Visualization & Trends

- Interactive **Charts** for all metrics (powered by Vico)
- Filter data by **Week, Month, 3 Months, Year, or All Time**
- View key statistics: Average, Minimum, Maximum, and Change over time

### 🎯 Goal Tracking

- Set personalized goals for **Weight**, **Body Fat**, or **Muscle Mass**
- Track progress with visual indicators
- Set deadlines and receive achievement alerts

### 💾 Data Management

- **Offline First**: All data is stored locally on your device using Room Database.
- **Export**: Export your data to **CSV** for analysis in spreadsheets.
- **Backup**: Create full **JSON backups** of your history and settings.
- **Edit/Delete**: Full control to modify or remove incorrect measurements.

### ⚙️ Customization

- **Dark/Light Theme** support
- **Reminders**: Schedule daily weigh-in notifications
- **Onboarding**: Guided setup for new users

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Storage**: Room Database (SQLite)
- **Asynchronous**: Coroutines & Flow
- **Charts**: Vico
- **Background Tasks**: WorkManager
- **Navigation**: Compose Navigation Suite

## 📂 Project Structure

The project follows a **layered architecture** based on Android best practices, separating concerns into UI, Data, and Domain layers:

```
com.aquilesorei.talon
├── ui/              # Presentation Layer
│   ├── screens/     # Composable screens (Home, History, etc.)
│   ├── components/  # Reusable UI components
│   └── theme/       # Theme definitions
├── domain/          # Domain Layer
│   ├── models/      # Business objects (Measurement, UserProfile)
│   └── usecases/    # Business logic
├── data/            # Data Layer
│   ├── local/       # Local storage (Room DB, DAOs, Entities)
│   └── repository/  # Single source of truth for data
├── viewmodels/      # State holders
├── workers/         # Background tasks (WorkManager)
└── utils/           # Utility classes
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android Device with Bluetooth LE support (min SDK 31 recommended)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Aquilesorei/talon.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle files.
4. Build and run on your device or emulator.

> **Note**: Bluetooth scanning requires a physical device. Emulators cannot scan for Bluetooth Low Energy devices.

## 📱 Usage

1. **Connect Scale**: Go to the **Measure** tab and tap the Bluetooth icon to scan for your supported smart scale.
2. **Weigh In**: Stand on the scale. The app will automatically capture your metrics.
3. **Track**: Switch to the **Charts** tab to see your trends.
4. **Set Goals**: Use the **Goals** tab to define your targets.
5. **Settings**: Customize your theme and notification preferences in **Settings**.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
