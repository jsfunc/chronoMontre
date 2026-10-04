# Distribute ChronoMontre

For a few people, share the signed watch APK and let them install it with ADB over Wi-Fi. For convenient installation by a wider audience, prepare a Google Play release.

ChronoMontre is a standalone Wear OS app intended for Galaxy Watch 6 and 7. Downloading its APK onto an Android phone does not install it on the watch. There is no phone companion app. Wear OS packages are installed separately from phone apps; see [Google's Wear OS packaging guide](https://developer.android.com/training/wearables/packaging).

## 1. Produce the file to share

On your Linux computer, open a normal terminal:

```bash
cd /home/aurelien/repo/chronoMontre
./scripts/build.sh
```

The first build needs internet access for Gradle and the AndroidX libraries. It builds a release APK, runs the unit tests and Android lint, and verifies its signature. Wait for the final success message before sharing anything.

For version 1.1.0 it produces:

- artifacts/chronomontre-1.1.0.apk
- artifacts/chronomontre-1.1.0.apk.sha256

Version 1.1.0 adds a launcher tile and a watch-face complication. Its release build, 17 unit tests, Android lint (warnings only), and signature verification have succeeded. Future rebuilt APKs are ready to share after the build script's final success message. The older 1.0.0 APK does not provide these shortcuts.

Install the new APK on your Watch 6 first, following [prepareWatch.md](prepareWatch.md). Check stopwatch controls, button laps, saved sessions, and opening the same running session from both shortcuts. Test Watch 7 before advertising that model as verified; its hardware behavior has not yet been tested.

## 2. Keep the signing key

Back up .local/debug.keystore privately. The build reuses this project's existing key so the APK can update the prototype and version 1.0.0 without losing app data. On a new source checkout, restore that key before building updates.

The signing configuration is inherited from the personal prototype and uses the standard debug-key alias/passwords. The release build is not configured as debuggable, but its certificate is a development certificate. It is suitable for the current direct APK sharing; a public Play release needs production signing preparation. Do not publish the keystore, its backups, or the entire working directory. Share the APK, checksum, and installation instructions.

Updates need the same application ID and signing identity, and a higher version code. Android documents these rules in [Sign your app](https://developer.android.com/studio/publish/app-signing). Losing or replacing the current key prevents ordinary updates to existing sideloaded installations.

## 3. Let people download it

You can send the APK as a file or place it on an HTTPS website. A Git repository is not required.

On a website, upload the APK and checksum into a downloads folder and add a link such as:

```html
<a href="/downloads/chronomontre-1.1.0.apk" download>
  Download ChronoMontre 1.1.0 for Galaxy Watch
</a>
<a href="/downloads/chronomontre-1.1.0.apk.sha256" download>
  Download SHA-256 checksum
</a>
```

These are example paths; uploading files and publishing a site are separate steps you perform with your chosen host. If the host requires a content type for APK files, use application/vnd.android.package-archive.

Beside the link, describe the app and show the watch installation steps below. Explain that it needs a Wear OS watch and a computer for this installation route. Mention Keep awake for button laps, the shortcut tile, and compatible watch-face complication slots.

The checksum helps recipients check that their download matches the file you shared. Publish it through a channel they trust. On Linux, put both downloaded files in one directory and run:

```bash
sha256sum -c chronomontre-1.1.0.apk.sha256
```

It should report OK. On macOS, use shasum -a 256 -c instead of sha256sum -c; on Windows PowerShell, use Get-FileHash -Algorithm SHA256 on the APK and compare with the checksum file.

## 4. Instructions to give recipients

These steps assume Galaxy Watch 6 or 7 and a computer. The watch and computer must be on the same Wi-Fi network.

1. Download the APK to the computer.
2. Download and extract Google's [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) for Linux, Windows, or macOS.
3. On the watch, open Settings → About watch → Software information, and tap Software version five times.
4. Open Developer options and enable ADB debugging and Wireless debugging.
5. In Wireless debugging, select Pair new device. Note its IP address, pairing port, and code.
6. Open a terminal in the extracted platform-tools folder. Run the pairing command, then enter the code when prompted:

```bash
./adb pair WATCH_IP:PAIRING_PORT
```

Return to the main Wireless debugging screen and read its connection port. It usually differs from the pairing port.

```bash
./adb connect WATCH_IP:CONNECTION_PORT
./adb devices -l
./adb -s WATCH_IP:CONNECTION_PORT install -r "/absolute/path/to/chronomontre-1.1.0.apk"
```

Replace the uppercase placeholders and APK path. On Windows PowerShell, use .\adb.exe instead of ./adb and a Windows path such as C:\Users\YourName\Downloads\chronomontre-1.1.0.apk. These commands follow [Wear OS wireless debugging](https://developer.android.com/training/wearables/get-started/debug-wifi) and [ADB installation](https://developer.android.com/tools/adb). The -r option also works for later updates while preserving app data.

After **Success**:

- Disable Wireless debugging and ADB debugging.
- Swipe up from the watch face and open the light-green stopwatch icon named ChronoMontre.
- To add the tile, swipe left through all tiles, tap Add tiles, and choose ChronoMontre.
- To add the complication, hold the watch face, choose Customize, find Complications, tap an editable slot, and choose ChronoMontre. Select a face or slot supporting an icon or short text if it is unavailable.
- Start timing. Keep awake is on by default while running. The lower-right button records laps with the display on.

The tile and complication open the stopwatch; they do not show a live elapsed time or change its state. The watch face chooses the complication's appearance and may show the icon with the word “Open”. See Samsung's [tile guide](https://www.samsung.com/us/support/answer/ANS10002835/) and [watch-face guide](https://www.samsung.com/us/support/answer/ANS10002950/).

Once installed, the app works without the computer or phone. Each recipient has their own stopwatch session.

## 5. Share later updates

Change app/version.properties: increment versionCode (1.1.0 uses 3) and choose a new versionName. Keep applicationId=fr.chronomontre.prototype and the signing key for this distribution channel. Run ./scripts/build.sh again, test the result, and upload the new APK and checksum.

Tell existing users to install with -r. Do not ask them to uninstall first: uninstalling deletes the saved session and settings. Direct APK downloads do not provide automatic updates; users repeat the installation procedure.

## 6. Google Play for easier installation

A Play release lets people install onto their watch through Google Play and receive updates. It requires more preparation than uploading this personal APK:

1. Create and verify a Play Console developer account, then create the app.
2. Set up production signing and an upload key, and adapt the release signing configuration. Preserve the current sideload key separately. Plan how existing personal installations transition: changing certificates prevents an ordinary in-place APK update.
3. Review the target SDK and input handling against the requirements in force when publishing. This personal build deliberately targets API 34 for the tested Samsung button behavior; do not assume that configuration meets current Play policy.
4. Produce the signed release bundle required for a new Play app, after configuring production signing, with ./gradlew :app:bundleRelease. The current build script creates an APK for direct sharing.
5. Prepare the listing: app icon, watch screenshots, description mentioning Wear OS, tile and complication support, and the required privacy/data declarations.
6. Add the Wear OS form factor, complete the applicable testing requirements, and submit for Wear OS review.

Follow the current [Wear OS publishing instructions](https://developer.android.com/training/wearables/packaging), [Wear OS quality criteria](https://developer.android.com/docs/quality-guidelines/wear-app-quality), and [Play release preparation](https://support.google.com/googleplay/android-developer/answer/9859152). Fees, verification, target SDK, and testing requirements can change; check them in Play Console when you decide to publish.

This file describes distribution. It does not create a public download, publish a store listing, or initialize a Git repository.
