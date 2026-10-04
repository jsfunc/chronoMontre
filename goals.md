# ChronoMontre specification

## Goal and scope

A personal stopwatch app for Samsung Galaxy Watch 6 and Galaxy Watch 7. Samsung's built-in Stopwatch is the reference; a simple digital layout is acceptable.

The lower-right physical button records a lap while running with the display on. The user tested the prototype on Watch 6: awake-screen laps work correctly, but a screen-off press only wakes the watch. The user explicitly accepted using Keep awake instead of requiring screen-off lap recording.

## Stopwatch behavior

- On-screen Start, Stop, Resume, Lap, and Reset controls.
- Stop freezes elapsed time; Resume continues the same session without counting paused intervals.
- Reset is available while stopped and clears elapsed time and all laps.
- A short lower-right button press records exactly one lap while running. It does nothing while paused or reset.
- Repeated events from a held button must not produce laps. Leave the usual system long-press action and upper-right button behavior available.
- Hardware lap recording while another app is in the foreground is outside scope.

## Display

- Large elapsed-time digits and larger lap text than the prototype.
- A dark, distraction-free round-watch layout: no app name, title, clock, diagnostics, or instructional text on the stopwatch screen.
- Display minutes, seconds, and hundredths, adding hours when necessary. Hundredths may be smaller to give the main digits more space.
- Show current lap duration and a scrollable list of recorded laps, newest first.
- Each recorded lap includes its number, individual duration, and cumulative elapsed time.
- Preserve laps through pause/resume; clear them on reset.
- English text.
- No saved-session history or export.

## Screen and feedback

- Keep awake is enabled by default while running and stops keeping the screen on when paused.
- Hide screen settings from the main stopwatch interface. Press and hold the large elapsed time to open the Keep awake setting.
- Save the user's screen-setting choice.
- Brief vibration for each recorded lap; no sound.
- If Keep awake is disabled and the watch sleeps, waking the screen before recording a lap is acceptable. Do not infer a lap from a screen-wake event.

## Timing and recovery

- Timing continues when the app is left or the display is off.
- Save and recover the single unfinished session, including running/paused state and laps, after closing the app or restarting the watch.
- Calculate elapsed time from a monotonic clock on the same boot, independently of display refreshes.
- Across a reboot, recover running elapsed time using wall-clock timestamps; powered-off time counts. A paused session remains paused.
- Wall-clock changes during the powered-off/reboot interval can affect cross-reboot recovery. This limitation does not affect normal same-boot timing.
- Persisting the unfinished session is not saved-session history.

## Delivery and installation

- Provide source, a buildable project, and an installable APK.
- Install from the user's Linux computer using ADB over Wi-Fi. The phone is a Samsung S24; no companion phone app is needed.
- The first final version replaces the existing test installation using the same application ID and signing key.
- The installed app works independently after debugging is disabled and after watch restarts.
- Keep installation, update, and ordinary-use instructions in prepareWatch.md.
- Provide a static tile that opens the stopwatch from the watch's tile carousel.
- Provide a static watch-face complication for icon or short-text slots. Tapping it opens the stopwatch; its appearance depends on the selected face.
- Both shortcuts restore the same current session without starting, pausing, resetting, or recording a lap. No live timing display on these shortcuts is requested.
- Explain direct APK downloads, recipient installation, updates, and future Play distribution in distribute.md.
- No public-store publication or Git repository initialization is requested.

## Acceptance checks

- Large, readable stopwatch and lap text on both watch sizes, with no name or diagnostic clutter.
- Start, stop, resume, reset, touch laps, and lower-right button laps.
- No hardware laps while paused or reset; no repeated laps on long presses.
- Keep awake while running, screen release when stopped, and saved screen preference.
- Correct individual and cumulative lap times across pauses and background periods.
- Recovery of running and paused sessions after app closure and watch restart.
- Brief vibration, no sound, and no history/export.
- Installation as an update to the prototype; operation with ADB and wireless debugging disabled.
- Add the tile and complication on a compatible watch face, then open the app from each while running and paused without changing the session.

Watch 6 awake-screen button handling was confirmed by the user on the prototype. The first final version's UI and recovery still require physical-watch confirmation, and Watch 7 has not yet been tested.

## Reference documentation

- [Wear OS physical buttons](https://developer.android.com/training/wearables/user-input/physical-buttons)
- [Samsung watch connection setup](https://developer.samsung.com/health/sensor/guide/connect-watch.html)
- [Wear OS wireless debugging](https://developer.android.com/training/wearables/get-started/debug-wifi)
- [Android elapsed-time clock](https://developer.android.com/reference/android/os/SystemClock)
