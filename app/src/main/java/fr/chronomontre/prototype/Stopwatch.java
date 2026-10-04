package fr.chronomontre.prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/** Elapsed time is calculated from a monotonic clock, never from UI refresh ticks. */
final class Stopwatch {
    private final LongSupplier clock;
    private final LongSupplier wallClock;
    private final IntSupplier bootCount;
    private final List<Long> laps = new ArrayList<>();
    private boolean running;
    private long accumulated;
    private long startedAt;

    Stopwatch(LongSupplier clock) {
        this(clock, clock, () -> 0);
    }

    Stopwatch(LongSupplier clock, LongSupplier wallClock, IntSupplier bootCount) {
        this.clock = clock;
        this.wallClock = wallClock;
        this.bootCount = bootCount;
    }

    boolean isRunning() { return running; }

    long elapsed() {
        return accumulated + (running ? clock.getAsLong() - startedAt : 0);
    }

    void start() {
        if (!running) {
            startedAt = clock.getAsLong();
            running = true;
        }
    }

    void pause() {
        if (running) {
            accumulated = elapsed();
            running = false;
        }
    }

    boolean lap() {
        if (!running) return false;
        laps.add(elapsed());
        return true;
    }

    void reset() {
        if (running) return;
        accumulated = 0;
        laps.clear();
    }

    List<Long> laps() { return Collections.unmodifiableList(laps); }

    long currentLap() {
        return elapsed() - (laps.isEmpty() ? 0 : laps.get(laps.size() - 1));
    }

    Snapshot snapshot() {
        long monotonic = clock.getAsLong();
        long total = accumulated + (running ? monotonic - startedAt : 0);
        return new Snapshot(running, total, monotonic, wallClock.getAsLong(),
                bootCount.getAsInt(), laps);
    }

    void restore(Snapshot saved) {
        long monotonic = clock.getAsLong();
        boolean sameBoot = saved.bootCount >= 0 && saved.bootCount == bootCount.getAsInt()
                && monotonic >= saved.monotonicMillis;
        long sinceSave = sameBoot ? monotonic - saved.monotonicMillis
                : Math.max(0, wallClock.getAsLong() - saved.wallMillis);
        accumulated = Math.addExact(saved.elapsedMillis, saved.running ? sinceSave : 0);
        startedAt = monotonic;
        running = saved.running;
        laps.clear();
        laps.addAll(saved.laps);
    }

    static final class Snapshot {
        final boolean running;
        final long elapsedMillis;
        final long monotonicMillis;
        final long wallMillis;
        final int bootCount;
        final List<Long> laps;

        Snapshot(boolean running, long elapsedMillis, long monotonicMillis,
                long wallMillis, int bootCount, List<Long> laps) {
            if (elapsedMillis < 0 || monotonicMillis < 0 || wallMillis < 0 || bootCount < -1) {
                throw new IllegalArgumentException("Invalid session clock values");
            }
            long previous = 0;
            for (long lap : laps) {
                if (lap < previous || lap > elapsedMillis) {
                    throw new IllegalArgumentException("Invalid lap time");
                }
                previous = lap;
            }
            this.running = running;
            this.elapsedMillis = elapsedMillis;
            this.monotonicMillis = monotonicMillis;
            this.wallMillis = wallMillis;
            this.bootCount = bootCount;
            this.laps = Collections.unmodifiableList(new ArrayList<>(laps));
        }
    }

    static String format(long millis) {
        long hundredths = millis / 10;
        long seconds = hundredths / 100;
        long minutes = seconds / 60;
        if (minutes >= 60) {
            return String.format(Locale.US, "%d:%02d:%02d.%02d",
                    minutes / 60, minutes % 60, seconds % 60, hundredths % 100);
        }
        return String.format(Locale.US, "%02d:%02d.%02d",
                minutes, seconds % 60, hundredths % 100);
    }
}
