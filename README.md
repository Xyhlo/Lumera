# Lumera

An Android TV streaming application built with Kotlin and Jetpack Compose.

<!-- TODO: Add app logo/screenshot here -->

## Features

- Browse and stream content from Stremio-compatible addons
- Custom profile system with avatars and themes
- D-pad optimized navigation for Android TV
- Built-in torrent streaming engine
- QR code pairing for remote addon installation
- Customizable hub layout with drag-and-drop ordering
- Multiple visual themes with smooth color transitions

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Compose for TV
- **Architecture:** MVVM with Hilt dependency injection
- **Database:** Room (local persistence)
- **Networking:** Retrofit + OkHttp
- **Image Loading:** Coil
- **Video Playback:** Media3 ExoPlayer
- **Torrent Engine:** JLibTorrent

## Building

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Android SDK 34+

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Lumera.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```

## Project Structure

```
Lumera/
├── app/                    # Main application module
│   └── src/main/java/com/lumera/app/
│       ├── data/           # Room database, repositories, API services
│       ├── di/             # Hilt dependency injection modules
│       ├── domain/         # Domain models
│       ├── remote_input/   # QR pairing & hub server
│       └── ui/             # Compose UI screens & components
│           ├── addons/     # Addon management
│           ├── components/ # Reusable UI components
│           ├── details/    # Content detail screen
│           ├── home/       # Home screen & carousel
│           ├── player/     # Video player
│           ├── profiles/   # User profiles & avatars
│           ├── settings/   # Settings & dashboard
│           └── theme/      # Theming system
├── playbackcore/           # Media3/ExoPlayer wrapper library
├── gradle/                 # Gradle wrapper
└── build.gradle.kts        # Root build configuration
```

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
