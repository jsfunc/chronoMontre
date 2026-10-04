package fr.chronomontre.prototype;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String TAG = "ChronoMontre";
    private static final int ACCENT = 0xFF9DFF71;
    private static final int MUTED = 0xFF9C9C9C;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Stopwatch stopwatch;
    private SharedPreferences preferences;
    private PowerManager power;
    private ScrollView mainScroll;
    private ScrollView activeScroll;
    private TextView elapsed;
    private TextView currentLap;
    private TextView lapNumber;
    private LinearLayout lapList;
    private Button startPause;
    private Button lapReset;
    private boolean resumed;
    private boolean settingsVisible;
    private boolean keepAwake;
    private int heldKey = -1;
    private long heldSince;
    private boolean heldWhileRunning;
    private boolean longPress;
    private int renderedLaps = -1;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            renderTime();
            if (resumed && power.isInteractive() && !settingsVisible) {
                handler.postDelayed(this, stopwatch.isRunning() ? 33 : 500);
            }
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) saveSession();
            handler.removeCallbacks(refresh);
            if (resumed && !settingsVisible
                    && !Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) handler.post(refresh);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("stopwatch", MODE_PRIVATE);
        power = getSystemService(PowerManager.class);
        int boot = readBootCount();
        stopwatch = new Stopwatch(SystemClock::elapsedRealtime, System::currentTimeMillis, () -> boot);
        String saved = preferences.getString("session", null);
        if (saved != null) {
            try {
                stopwatch.restore(SessionCodec.decode(saved));
            } catch (IllegalArgumentException | ArithmeticException error) {
                Log.e(TAG, "Could not restore stopwatch session", error);
                Toast.makeText(this, "Could not restore the previous stopwatch session",
                        Toast.LENGTH_LONG).show();
            }
        }
        keepAwake = preferences.getBoolean("keepAwake", true);
        buildMainUi();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
        renderControls();
        renderTime();
        renderLaps();
    }

    private int readBootCount() {
        try {
            return Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (SecurityException error) {
            Log.w(TAG, "Boot counter unavailable; using wall clock for session recovery");
            return -1;
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Shortcut launches return to the one existing stopwatch, including its session.
        if (settingsVisible) {
            settingsVisible = false;
            activeScroll = mainScroll;
            setContentView(mainScroll);
        }
        mainScroll.scrollTo(0, 0);
        renderTime();
        handler.removeCallbacks(refresh);
        if (resumed) handler.post(refresh);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        handler.removeCallbacks(refresh);
        if (!settingsVisible) handler.post(refresh);
        updateAwake();
        saveSession();
    }

    @Override protected void onPause() {
        resumed = false;
        heldKey = -1;
        handler.removeCallbacks(refresh);
        saveSession();
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(refresh);
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_BACK && key != KeyEvent.KEYCODE_STEM_1) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                heldKey = key;
                heldSince = event.getEventTime();
                heldWhileRunning = stopwatch.isRunning();
                longPress = false;
            }
            // Leave the system's long-press path available; repeats never record laps.
            if (event.isLongPress()) {
                longPress = true;
                return super.dispatchKeyEvent(event);
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            boolean shortPress = heldKey == key && !longPress && !event.isCanceled()
                    && event.getEventTime() - heldSince < ViewConfiguration.getLongPressTimeout();
            heldKey = -1;
            if (shortPress && heldWhileRunning && stopwatch.isRunning()) recordLap();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL
                && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float factor = ViewConfiguration.get(this).getScaledVerticalScrollFactor();
            activeScroll.smoothScrollBy(0,
                    Math.round(-event.getAxisValue(MotionEvent.AXIS_SCROLL) * factor));
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private void saveSession() {
        preferences.edit().putString("session", SessionCodec.encode(stopwatch.snapshot())).apply();
    }

    private void recordLap() {
        if (!stopwatch.lap()) return;
        saveSession();
        Vibrator vibrator = getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        renderTime();
        renderLaps();
    }

    private void buildMainUi() {
        mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);
        mainScroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = column(18, 18, 36);
        mainScroll.addView(content);

        elapsed = text("", 54, Color.WHITE);
        elapsed.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        elapsed.setAutoSizeTextTypeUniformWithConfiguration(26, 54, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        elapsed.setContentDescription("Elapsed time. Press and hold to open screen settings.");
        elapsed.setOnLongClickListener(v -> {
            showSettings();
            return true;
        });
        content.addView(elapsed, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout lap = new LinearLayout(this);
        lap.setGravity(Gravity.CENTER_VERTICAL);
        lapNumber = text("", 14, MUTED);
        lap.addView(lapNumber, new LinearLayout.LayoutParams(dp(40), -1));
        currentLap = text("", 26, ACCENT);
        currentLap.setAutoSizeTextTypeUniformWithConfiguration(17, 26, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        lap.addView(currentLap, new LinearLayout.LayoutParams(0, -1, 1));
        content.addView(lap, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(0, dp(8), 0, dp(14));
        lapReset = button("Reset", 0xFF2A2A2A, Color.WHITE);
        startPause = button("Start", ACCENT, Color.BLACK);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, dp(44), 1);
        size.setMargins(dp(3), 0, dp(3), 0);
        controls.addView(lapReset, size);
        controls.addView(startPause, new LinearLayout.LayoutParams(size));
        content.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        startPause.setOnClickListener(v -> {
            if (stopwatch.isRunning()) stopwatch.pause(); else stopwatch.start();
            saveSession();
            renderControls();
            renderTime();
            handler.removeCallbacks(refresh);
            if (resumed && power.isInteractive()) handler.post(refresh);
        });
        lapReset.setOnClickListener(v -> {
            if (stopwatch.isRunning()) recordLap();
            else {
                stopwatch.reset();
                saveSession();
                renderControls();
                renderTime();
                renderLaps();
            }
        });

        lapList = new LinearLayout(this);
        lapList.setOrientation(LinearLayout.VERTICAL);
        content.addView(lapList, new LinearLayout.LayoutParams(-1, -2));
        activeScroll = mainScroll;
        setContentView(mainScroll);
    }

    private void showSettings() {
        settingsVisible = true;
        handler.removeCallbacks(refresh);
        ScrollView settings = new ScrollView(this);
        LinearLayout content = column(24, 38, 38);
        settings.addView(content);

        Switch awake = new Switch(this);
        awake.setText("Keep awake");
        awake.setTextSize(16);
        awake.setTextColor(Color.WHITE);
        awake.setChecked(keepAwake);
        content.addView(awake, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView explanation = text("", 13, MUTED);
        explanation.setPadding(0, dp(10), 0, dp(18));
        content.addView(explanation, new LinearLayout.LayoutParams(-1, -2));
        explanation.setText(keepAwake ? "Screen stays on while timing"
                : "Wake the screen before recording a lap");
        awake.setOnCheckedChangeListener((view, checked) -> {
            keepAwake = checked;
            preferences.edit().putBoolean("keepAwake", checked).apply();
            updateAwake();
            explanation.setText(checked ? "Screen stays on while timing"
                    : "Wake the screen before recording a lap");
        });

        Button back = button("Back", 0xFF2A2A2A, Color.WHITE);
        content.addView(back, new LinearLayout.LayoutParams(-1, dp(44)));
        back.setOnClickListener(v -> {
            settingsVisible = false;
            activeScroll = mainScroll;
            setContentView(mainScroll);
            renderTime();
            handler.removeCallbacks(refresh);
            if (resumed) handler.post(refresh);
        });
        activeScroll = settings;
        setContentView(settings);
    }

    private void renderTime() {
        elapsed.setText(timeText(stopwatch.elapsed(), 0.42f));
        boolean initial = !stopwatch.isRunning() && stopwatch.elapsed() == 0;
        lapNumber.setText(initial ? "" : "Lap " + (stopwatch.laps().size() + 1));
        currentLap.setText(initial ? "" : timeText(stopwatch.currentLap(), 0.7f));
    }

    private void renderControls() {
        startPause.setText(stopwatch.isRunning() ? "Stop" :
                stopwatch.elapsed() == 0 ? "Start" : "Resume");
        lapReset.setText(stopwatch.isRunning() ? "Lap" : "Reset");
        updateAwake();
    }

    private void renderLaps() {
        int count = stopwatch.laps().size();
        if (renderedLaps == count) return;
        if (renderedLaps >= 0 && count == renderedLaps + 1) {
            lapList.addView(lapRow(count - 1), 0);
        } else {
            lapList.removeAllViews();
            for (int i = count - 1; i >= 0; i--) lapList.addView(lapRow(i));
        }
        renderedLaps = count;
    }

    private View lapRow(int index) {
        long total = stopwatch.laps().get(index);
        long duration = total - (index == 0 ? 0 : stopwatch.laps().get(index - 1));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(14));
        TextView number = text("Lap " + (index + 1), 16, MUTED);
        TextView time = text("", 26, Color.WHITE);
        time.setText(timeText(duration, 0.7f));
        time.setAutoSizeTextTypeUniformWithConfiguration(19, 26, 1,
                android.util.TypedValue.COMPLEX_UNIT_SP);
        row.addView(number, new LinearLayout.LayoutParams(-1, dp(24)));
        row.addView(time, new LinearLayout.LayoutParams(-1, dp(36)));
        TextView cumulative = text("Total " + Stopwatch.format(total), 16, MUTED);
        row.addView(cumulative);
        return row;
    }

    private CharSequence timeText(long millis, float fractionSize) {
        String formatted = Stopwatch.format(millis);
        SpannableString value = new SpannableString(formatted);
        value.setSpan(new RelativeSizeSpan(fractionSize), formatted.indexOf('.'),
                formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return value;
    }

    private void updateAwake() {
        if (keepAwake && stopwatch.isRunning()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private LinearLayout column(int horizontal, int top, int bottom) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(horizontal), dp(top), dp(horizontal), dp(bottom));
        return content;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setFontFeatureSettings("tnum");
        return view;
    }

    private Button button(String label, int background, int foreground) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(foreground);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(background);
        shape.setCornerRadius(dp(28));
        button.setBackground(shape);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
