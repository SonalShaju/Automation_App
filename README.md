# Automation App

<div align="center">

**A powerful, native Android automation engine built for efficiency and precise control.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

</div>

---

## 📱 Overview

Automation App is a sophisticated Android application that enables users to create custom automation rules with **triggers**, **conditions**, and **actions**. Built with modern Android architecture patterns and leveraging native APIs, it provides seamless automation without compromising battery life or privacy.

Create rules like:
- 🌙 *"When it's 10 PM AND I'm home, turn on Do Not Disturb and dim the screen"*
- 🔋 *"When battery drops below 20%, enable battery saver and reduce brightness"*
- 🚗 *"When I connect to car Bluetooth, turn off silent mode and launch music app"*
- 📍 *"When I arrive at work AND it's a weekday, silence my phone"*

---

## ✨ Key Features

### 🎯 Extensive Triggers
Automation starts when specific events occur:

| Category | Triggers |
|----------|----------|
| **⏰ Time** | Time of day, Time ranges, Scheduled events |
| **📍 Location** | Geofences (enter/exit specific locations) |
| **🔋 Battery** | Battery level thresholds, Charging status changes |
| **📶 Connectivity** | WiFi connection/disconnection, Bluetooth pairing, Airplane mode |
| **🎧 Device State** | Headset connection, Do Not Disturb changes |
| **📱 Apps** | App launch/open events |

### ⚙️ Versatile Actions
Execute powerful actions when rules are triggered:

| Category | Actions |
|----------|---------|
| **💡 Hardware** | Flashlight control, Device vibration |
| **🔊 Audio** | Volume adjustment, Ringer mode (Silent/Vibrate/Normal) |
| **📱 Display** | Brightness control, Auto-rotate toggle, Screen timeout |
| **🔒 System** | Lock screen, Take screenshot, Power dialog |
| **🚀 Apps** | Launch apps, Block app access |
| **🔔 Notifications** | Send custom notifications, Clear all notifications |
| **🌙 Do Not Disturb** | Enable/disable DND mode |

### 🧠 Advanced Logic
- **Multiple Triggers**: Set up several trigger events for a single rule
- **Conditional Execution**: Add conditions that must ALL be satisfied (AND logic)
- **Sequential Actions**: Chain multiple actions to execute in order
- **Rule Priority**: Manage execution order for overlapping rules

### 🏗️ Architecture Highlights
- **Clean Architecture**: Separation of concerns with domain, data, and presentation layers
- **MVVM Pattern**: ViewModel-driven UI with Jetpack Compose
- **Dependency Injection**: Hilt/Dagger for maintainable and testable code
- **Room Database**: Local persistence with migration support
- **Reactive Patterns**: Kotlin Flows for real-time updates
- **Background Processing**: WorkManager and Foreground Services for reliable execution

---

## 🛠️ Tech Stack

### Core Technologies
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 36

### Libraries & Frameworks

| Purpose | Library | Version |
|---------|---------|---------|
| **Dependency Injection** | Hilt / Dagger | Latest |
| **Database** | Room | Latest |
| **Async Processing** | Kotlin Coroutines | Latest |
| **Background Tasks** | WorkManager | Latest |
| **Navigation** | Navigation Compose | Latest |
| **Serialization** | Kotlinx Serialization | Latest |
| **Location Services** | Google Play Services Location | Latest |
| **Maps** | OSMDroid (OpenStreetMap) | 6.1.18 |
| **Data Storage** | DataStore Preferences | Latest |

### Android Services Used
- **AccessibilityService**: System-level actions (lock screen, screenshot)
- **NotificationListenerService**: Notification management
- **ForegroundService**: Continuous trigger monitoring
- **AlarmManager**: Precise time-based triggers
- **GeofencingAPI**: Location-based triggers

---

## 📁 Project Structure

```
app/
├── src/main/java/com/example/automationapp/
│   ├── data/                       # Data layer
│   │   ├── local/                  # Local data sources
│   │   │   ├── dao/                # Room DAOs
│   │   │   ├── entity/             # Database entities
│   │   │   ├── database/           # Database configuration
│   │   │   └── converter/          # Type converters
│   │   ├── preferences/            # DataStore preferences
│   │   └── repository/             # Repository implementations
│   │
│   ├── domain/                     # Domain layer
│   │   ├── executor/               # Action & Trigger executors
│   │   ├── model/                  # Domain models
│   │   ├── repository/             # Repository interfaces
│   │   └── usecase/                # Business logic use cases
│   │
│   ├── ui/                         # Presentation layer
│   │   ├── screens/                # Composable screens
│   │   ├── components/             # Reusable UI components
│   │   ├── viewmodel/              # ViewModels
│   │   ├── navigation/             # Navigation graph
│   │   └── theme/                  # App theming
│   │
│   ├── service/                    # Android Services
│   │   ├── TriggerMonitorService   # Monitors triggers
│   │   ├── AutomationAccessibilityService
│   │   ├── AutomationNotificationListenerService
│   │   ├── AlarmScheduler          # Time-based triggers
│   │   ├── GeofenceManager         # Location triggers
│   │   └── TriggerManager          # Centralized trigger handling
│   │
│   ├── receiver/                   # Broadcast receivers
│   │   ├── BatteryReceiver
│   │   ├── ConnectivityReceiver
│   │   ├── HeadsetReceiver
│   │   └── GeofenceBroadcastReceiver
│   │
│   ├── worker/                     # WorkManager workers
│   │   ├── PeriodicCheckWorker
│   │   ├── RuleEvaluationWorker
│   │   └── TriggerBasedWorker
│   │
│   ├── di/                         # Dependency injection modules
│   └── util/                       # Utility classes
│       ├── PermissionHelper
│       └── PermissionManager
```

---

## 🚀 Getting Started

### Prerequisites
- Android device running **Android 8.0 (Oreo) or higher** (API level 26+)
- Approximately **50 MB** of free storage space
- Allow installation from unknown sources (for APK installation)

### Installation

1. **Download the APK**
   - Go to the [Releases](https://github.com/SonalShaju/Automation_App/releases) page
   - Download the latest `app-release.apk` file

2. **Enable Unknown Sources** (if not already enabled)
   - Go to **Settings** → **Security** (or **Privacy**)
   - Enable **Install from Unknown Sources** or allow your browser/file manager to install apps
   - On Android 8.0+, you'll be prompted to allow the specific app to install APKs

3. **Install the APK**
   - Open the downloaded APK file
   - Tap **Install**
   - Wait for the installation to complete
   - Tap **Open** to launch the app

4. **Grant Required Permissions**
   - Follow the onboarding screens to grant necessary permissions
   - Enable Accessibility Service and Notification Access when prompted

### For Developers

If you want to build from source:

1. **Clone the repository**
   ```bash
   git clone https://github.com/SonalShaju/Automation_App.git
   cd automation-app
   ```

2. **Open in Android Studio**
   - Launch Android Studio Hedgehog or newer
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

**Build Requirements:**
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android SDK 26+
- Gradle 8.x

---

## 📋 Required Permissions

The app requests various permissions based on the features you use:

| Permission | Purpose | When Requested |
|------------|---------|----------------|
| **Accessibility Service** | Execute system actions (lock, screenshot) | On first use of system actions |
| **Notification Access** | Read/dismiss notifications | On first use of notification actions |
| **Location (Fine/Coarse)** | Geofence triggers | When creating location-based rules |
| **Bluetooth** | Monitor Bluetooth connections | When using Bluetooth triggers |
| **Modify System Settings** | Change brightness, rotation | When using display actions |
| **Do Not Disturb Access** | Toggle DND mode | When using DND actions/triggers |
| **Camera** | Control flashlight | When using flashlight actions |
| **Query All Packages** | List installed apps | When selecting apps for rules |

> **Privacy Note**: All data is stored locally on your device. No data is transmitted to external servers.

---

## 💡 Usage

### Creating Your First Rule

1. **Open the app** and tap the "+" button
2. **Name your rule** (e.g., "Bedtime Routine")
3. **Add a Trigger**:
   - Select trigger type (e.g., "Time Based")
   - Configure parameters (e.g., 10:00 PM, Monday-Friday)
4. **Add Conditions** (optional):
   - Add state checks (e.g., "WiFi Connected" to home network)
5. **Add Actions**:
   - Select actions to execute (e.g., "Enable DND", "Adjust Brightness")
6. **Save** and enable the rule

### Example Rules

#### 🌃 Nighttime Automation
```
Trigger: Time Based (10:00 PM, Daily)
Conditions: WiFi Connected (Home Network)
Actions:
  - Enable Do Not Disturb
  - Set Brightness to 20%
  - Disable Auto Rotate
```

#### 🚗 Driving Mode
```
Trigger: Bluetooth Connected (Car Bluetooth)
Actions:
  - Set Ringer Mode to Normal
  - Adjust Media Volume to 80%
  - Launch Music App
```

#### 🔋 Battery Saver
```
Trigger: Battery Level (Below 15%)
Actions:
  - Enable Do Not Disturb
  - Set Brightness to 30%
  - Send Notification ("Low Battery Mode Activated")
```

---

## 🏛️ Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│          Presentation Layer             │
│  (UI, ViewModels, Compose Screens)      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Domain Layer                  │
│  (Use Cases, Repository Interfaces,     │
│   Domain Models, Executors)             │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│            Data Layer                   │
│  (Room Database, DAOs, Entities,        │
│   Repository Implementations)           │
└─────────────────────────────────────────┘
```

### Key Components

- **TriggerEvaluator**: Evaluates if triggers and conditions are satisfied
- **ActionExecutor**: Executes automation actions using native Android APIs
- **TriggerManager**: Centralized trigger registration and monitoring
- **RuleSchedulingManager**: Manages background rule execution
- **GeofenceManager**: Handles location-based triggers
- **AlarmScheduler**: Schedules time-based triggers

---

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Write meaningful commit messages
- Add unit tests for new features
- Update documentation as needed

---

## 📝 Roadmap

- [ ] Add more trigger types (NFC, Calendar events)
- [ ] Support OR logic for conditions
- [ ] Rule templates and sharing
- [ ] Backup/restore automation rules
- [ ] Rule execution history and analytics
- [ ] Integration with Tasker/IFTTT
- [ ] Widget for quick rule toggle

---

## 🐛 Known Issues

- Some actions require specific OEM permissions on certain devices
- Geofencing may have delayed triggers based on device location settings
- Accessibility service may need to be re-enabled after system updates

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Your Name**
- GitHub: [@yourusername](https://github.com/yourusername)
- Email: your.email@example.com

---

## 🙏 Acknowledgments

- [Android Developers](https://developer.android.com/) for comprehensive documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) community
- [OSMDroid](https://github.com/osmdroid/osmdroid) for map functionality
- All contributors and testers

---

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/yourusername/automation-app/issues) page
2. Search for existing solutions
3. Create a new issue with detailed information

---

<div align="center">

**If you find this project helpful, please consider giving it a ⭐!**

Made with ❤️ using Kotlin and Jetpack Compose

</div>

