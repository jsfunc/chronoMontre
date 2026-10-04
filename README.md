# ChronoMontre

Personal stopwatch for Galaxy Watch 6 and 7. The requirements are in [goals.md](goals.md).

## Version 1.1.0: tile and complication

The main screen contains large elapsed time, current lap time, and Start/Stop and Lap/Reset controls. Scroll to read recorded laps. There is no app name, title, wall clock, diagnostic panel, or tutorial text in the stopwatch interface.

A short lower-right button press records one lap while running. Each lap vibrates briefly. Keep awake is enabled by default while running. Press and hold the large elapsed time to open its setting. While stopped, the screen may sleep normally.

Version 1.1.0 adds a tile labeled “Open stopwatch” and a watch-face complication offering a stopwatch icon or short “Open” text. Tapping either opens the same stopwatch session without changing its state. A compatible editable watch-face slot is required. These are static shortcuts; elapsed time is displayed in the app.

The single unfinished session is saved, including its running/paused state and laps. Timing uses Android's monotonic clock on the same boot, even when the process is absent. After reboot, recovery uses wall-clock timestamps and includes powered-off time for a running stopwatch; a paused stopwatch stays paused. Cross-reboot timing can be affected by wall-clock changes. There is no session history or export.

Use [prepareWatch.md](prepareWatch.md) for installation, updates, shortcuts, and ordinary use. See [distribute.md](distribute.md) for sharing APK downloads and preparing a future Google Play release.

## Build and tests

The project uses Java 17, Android SDK platform 36, Android Gradle Plugin 8.12.1, Gradle 8.13, and AndroidX Wear libraries. A Gradle wrapper is included. The first build needs internet access.

```bash
./scripts/build.sh
```

This runs the release build, unit tests, Android lint, and APK signature verification. After success it copies the APK to artifacts/chronomontre-1.1.0.apk and writes its SHA-256 checksum beside it.

The script defaults to ~/Android/Sdk and Android Studio's JDK at ~/android-studio/jbr. Override ANDROID_SDK_ROOT, JAVA_HOME, or WATCH_BUILD_TOOLS for another installation. Gradle needs the SDK platform declared in app/version.properties and its required build tools; install missing components using Android Studio's SDK Manager.

Preserve .local/debug.keystore for future sideloaded updates; it is excluded from Git. Restore the existing key before building in another checkout. Release builds currently use that personal development certificate and are not configured as debuggable. Public Play distribution needs production signing and policy preparation, described in distribute.md.

The independent stopwatch tests can also run without Gradle:

```bash
./scripts/test.sh
```

This script uses Bash, Python 3, a JDK, and cached JUnit 4.13.2/Hamcrest Core 1.3. Override WATCH_JUNIT_JAR and WATCH_HAMCREST_JAR for other jar locations.

## Validation

All 17 stopwatch JUnit tests pass, covering pause/lap arithmetic, background timing, process restart, reboot recovery, wall-clock changes, resetting stored state, and invalid stored data. All application Java sources compile against the downloaded AndroidX libraries.

The full 1.1.0 Gradle release build and all 17 unit tests passed from the user's terminal. Android lint completed with no errors and nine warnings about SDK attributes, dependency versions, vector preview complexity, backup configuration, recents, and English UI strings. The [lint report](app/build/reports/lint-results-release.html) contains the details. The coding sandbox cannot create Gradle's local sockets.

The ready-to-install [1.1.0 APK](artifacts/chronomontre-1.1.0.apk) passed signature and [checksum](artifacts/chronomontre-1.1.0.apk.sha256) verification. Its signing certificate matches the prototype and 1.0.0, and its manifest is not marked debuggable or test-only. The merged manifest registers both shortcut services and adds no internet permission.

The new version keeps the prototype's application ID and signing key and uses version code 3. Install with ADB install -r to preserve the current session.

The user confirmed awake-screen button laps on Watch 6 with the prototype and accepted Keep awake after discovering that a screen-off press only wakes the watch. The new shortcuts, final layout, and recovery still need physical testing; Watch 7 is untested.

The app's source requests vibration permission only and has no network feature, accessibility service, background remapper, boot receiver, or background timing service. No periodic UI work runs while the activity is paused. Shortcut services do not read or mutate the session, and the complication requests no periodic updates.
