# Eraser

Eraser is an Android application built with Jetpack Compose that syncs deadlines from your Blackboard external calendar feed into a clean, actionable task view.

## Features

- **Blackboard ICS Sync**: Connects to your private external calendar feed, downloads deadlines, and handles timezone conversions with Daylight Saving Time support.
- **Offline Cache**: Deadlines are persisted locally in a Room SQLite database (`eraser.db`).
- **Urgency Buckets**: Tasks are categorized into Overdue, Today, This Week, and Later.
- **Secure Token Storage**: The calendar feed URL token is kept private and encrypted on-device via `EncryptedSharedPreferences`.
- **Modern Android Stack**: Built with Jetpack Compose (Material 3), Kotlin Coroutines & Flow, Room, and OkHttp.

## Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2+) or newer.
- **JDK 17 or 21** (automatically bundled with Android Studio as JetBrains Runtime).
- **Android SDK Platform 37 / Build Tools 36+**.

### Clone & Run

1. **Clone the repository**:
   ```bash
   git clone <repo-url>
   cd eraser
   ```

2. **Open in Android Studio**:
   - Select **File → Open** and choose the `eraser` root directory.
   - Android Studio will automatically run a Gradle Sync using the bundled Gradle Wrapper (`gradle-wrapper.jar` + `gradle-9.9.0`).
   - If missing SDK platforms are detected, Android Studio will prompt you to install them with one click.

3. **Run on the Emulator**:
   - Open the **Device Manager** in Android Studio (**Tools → Device Manager**).
   - Start an existing Android Virtual Device (e.g., Pixel with API 35 or 37) or create a new one.
   - Select the **`app`** run configuration from the toolbar and click **Run** (green play icon / Shift+F10).

### Command Line Build & Test

You can also build and test the project directly using the Gradle wrapper:

- **Run all unit tests**:
  ```bash
  ./gradlew test
  ```
  *(On Windows PowerShell: `.\gradlew.bat test`)*

- **Build debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```

- **Install directly onto a running device/emulator**:
  ```bash
  ./gradlew installDebug
  ```

