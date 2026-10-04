# Install ChronoMontre on a Galaxy Watch 6 or 7

Version 1.1.0 adds a tile and watch-face complication. The tested and signed APK is ready at artifacts/chronomontre-1.1.0.apk. If your watch is already paired and connected to ADB, go directly to step 6.

To rebuild the APK later, run this in your normal Linux terminal:

```bash
cd /home/aurelien/repo/chronoMontre
./scripts/build.sh
```

Wait for the final success message, which confirms the APK, tests, lint, and signature verification. The first build on another computer needs internet access.

## 1. Check ADB on Linux

The Android SDK is already installed on this computer. Open a terminal:

```bash
~/Android/Sdk/platform-tools/adb version
```

The commands use this existing executable; there is no need to install another copy.

## 2. Connect the watch to Wi-Fi

On the watch, open **Settings → Connections → Wi-Fi** and join the network used by your Linux computer. Menu labels may vary. The phone can stay paired normally.

## 3. Enable developer options

1. Open **Settings → About watch → Software information** (sometimes **Software**).
2. Tap **Software version** five times, until developer mode is enabled.
3. Return to **Settings → Developer options**.
4. Enable **ADB debugging** and **Wireless debugging**.
5. Accept the prompt permitting debugging on this network.

These settings follow [Samsung's connection guide](https://developer.samsung.com/health/sensor/guide/connect-watch.html).

## 4. Pair the computer

In the watch's **Wireless debugging** menu, choose **Pair new device**. Keep that screen open and use its IP address and pairing port:

```bash
~/Android/Sdk/platform-tools/adb pair WATCH_IP:PAIRING_PORT
```

Replace the uppercase placeholders. Enter the displayed pairing code in the terminal when prompted. If it expires, reopen the pairing screen and use the new values.

## 5. Connect

Return to the main **Wireless debugging** screen. Use the connection port shown there, which usually differs from the pairing port:

```bash
~/Android/Sdk/platform-tools/adb connect WATCH_IP:CONNECTION_PORT
~/Android/Sdk/platform-tools/adb devices -l
```

The watch's row should say **device**. Read the current address again after toggling wireless debugging or changing networks. These steps follow [Google's Wi-Fi debugging guide](https://developer.android.com/training/wearables/get-started/debug-wifi).

## 6. Install the final APK

Run this in your own Linux terminal, replacing WATCH_IP:CONNECTION_PORT with the connected watch's address:

```bash
cd /home/aurelien/repo/chronoMontre
~/Android/Sdk/platform-tools/adb -s WATCH_IP:CONNECTION_PORT install -r artifacts/chronomontre-1.1.0.apk
```

Wait for **Success**. This updates either **ChronoMontre Test** or **ChronoMontre 1.0.0**. Its launcher name is **ChronoMontre**, with the same light-green stopwatch icon. Do not uninstall the existing app first.

The -r option replaces the installed app while retaining its app data. APK installation uses [Android's ADB tools](https://developer.android.com/tools/adb).

## 7. Open and use it

Swipe up from the watch face and tap the light-green stopwatch icon.

- **Start** begins timing. **Stop** pauses it. **Resume** continues.
- While running, tap **Lap** or briefly press the **lower-right physical button** to record a lap.
- Scroll down to read recorded laps. Each shows its number, lap duration, and total elapsed time.
- **Reset** clears the current session while stopped.
- The screen stays awake while running by default. It may sleep once stopped.
- Press and hold the **large elapsed time** to open the Keep awake setting. Tap **Back** to return.
- Use the upper-right Home button to return to the watch face. Timing continues; reopening restores the unfinished session.

There is no app name or diagnostic text on the stopwatch screen. Laps vibrate briefly and make no sound.

On the Watch 6, a press with the screen fully off only wakes the display. Use Keep awake for one-press lap recording; if you disable it, wake the screen before recording a lap.

## 8. Add the tile (optional)

From the watch face, swipe left through your tiles until you reach **Add tiles**. Tap it and choose **ChronoMontre**. Its tile shows a stopwatch icon and **Open stopwatch**. Tap anywhere on it to open the app. Hold the tile to move it closer to the watch face.

You can also manage tiles from **Galaxy Wearable → Tiles** on your S24. Menu names can vary; see [Samsung's tile guide](https://www.samsung.com/us/support/answer/ANS10002835/).

## 9. Add the complication (optional)

1. Press and hold your main watch face.
2. Tap **Customize**, then swipe to **Complications**.
3. Tap an editable complication slot and select **ChronoMontre**.
4. Save the customization or return to the face as directed by its editor.

The shortcut appears as a stopwatch icon or an icon with the short label **Open**, depending on the watch face. Its color is chosen by the face. Tap it to open the stopwatch. It restores the current session and does not start or stop timing.

If ChronoMontre is unavailable, try a slot supporting an icon or short text, or another customizable watch face. Some faces offer no editable complication slots. The complication is a launcher shortcut; it does not show live elapsed time on the face.

You can also use **Galaxy Wearable → Watch faces → Customize**. See [Samsung's watch-face guide](https://www.samsung.com/us/support/answer/ANS10002950/).

Both shortcuts require version 1.1.0; they are absent from version 1.0.0 and the prototype.

## 10. Finish installation

On the watch, disable **Wireless debugging** and **ADB debugging** in Developer options.

The app stays installed and works without debugging, your computer, or a connection to your phone. Watch restarts do not uninstall it. A running unfinished session is recovered when you reopen it; time during the restart counts. A paused session stays paused.

## 11. Quick check of the update

Start, record a button lap, stop, then resume and record another lap. Check that the paused interval is excluded. Pressing the lower-right button while stopped should do nothing.

While running, use Home to return to the face, then open the complication. Confirm timing and laps remain. Repeat using the tile, including once while paused: opening either must preserve the running/paused state. Also check recovery after a watch restart. Reset when finished. The prototype's awake-screen button behavior was confirmed on Watch 6; the new shortcuts, final layout/recovery, and Watch 7 still need a hardware check.

## Updates and troubleshooting

For a later APK, enable debugging, reconnect, and use the same install -r command with the new filename. Preserve this project's .local/debug.keystore so updates use the same signing key.

If the app icon is not visible, launch it from the terminal:

```bash
~/Android/Sdk/platform-tools/adb -s WATCH_IP:CONNECTION_PORT shell am start -n fr.chronomontre.prototype/.MainActivity
```

If connecting fails, check the current connection port and Wi-Fi network. Guest networks may block device-to-device connections; try connecting the watch and computer to your S24's hotspot. If Wi-Fi drops automatically, check Samsung's **Turn off automatic Wi-Fi** developer option.

This coding session cannot open an ADB network connection, so connection and installation commands must be run in your own terminal.
