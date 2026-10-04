package fr.chronomontre.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class StopwatchTest {
    private long now;
    private long wall = 1_000_000;
    private int boot = 4;
    private final Stopwatch stopwatch = create();

    private Stopwatch create() {
        return new Stopwatch(() -> now, () -> wall, () -> boot);
    }

    @Test public void backgroundTimeDoesNotRequireRefreshTicks() {
        now = 1_000;
        stopwatch.start();
        now += 3_600_000;
        assertEquals(3_600_000, stopwatch.elapsed());
    }

    @Test public void lapsExcludePausedIntervals() {
        stopwatch.start();
        now = 1_250;
        assertTrue(stopwatch.lap());
        now = 2_000;
        stopwatch.pause();
        now = 42_000;
        assertEquals(2_000, stopwatch.elapsed());
        assertFalse(stopwatch.lap());
        stopwatch.start();
        now = 43_500;
        assertTrue(stopwatch.lap());
        assertEquals(Long.valueOf(1_250), stopwatch.laps().get(0));
        assertEquals(Long.valueOf(3_500), stopwatch.laps().get(1));
        assertEquals(2_250, stopwatch.laps().get(1) - stopwatch.laps().get(0));
    }

    @Test public void repeatedStartDoesNotRestartTiming() {
        stopwatch.start();
        now = 1_000;
        stopwatch.start();
        now = 2_000;
        assertEquals(2_000, stopwatch.elapsed());
    }

    @Test public void resetRequiresPauseAndClearsLaps() {
        stopwatch.start();
        now = 2_000;
        stopwatch.lap();
        stopwatch.reset();
        assertEquals(1, stopwatch.laps().size());
        assertEquals(2_000, stopwatch.elapsed());
        stopwatch.pause();
        stopwatch.reset();
        assertEquals(0, stopwatch.elapsed());
        assertTrue(stopwatch.laps().isEmpty());
        assertFalse(stopwatch.isRunning());
        assertFalse(stopwatch.lap());
    }

    @Test public void formatsHundredthsAndHoursWithoutRoundingUp() {
        assertEquals("00:00.00", Stopwatch.format(0));
        assertEquals("00:59.99", Stopwatch.format(59_999));
        assertEquals("01:00.00", Stopwatch.format(60_000));
        assertEquals("1:00:00.00", Stopwatch.format(3_600_000));
    }

    @Test public void runningSessionSurvivesProcessRestart() {
        stopwatch.start();
        now = 1_250;
        stopwatch.lap();
        now = 2_000;
        Stopwatch.Snapshot saved = SessionCodec.decode(SessionCodec.encode(stopwatch.snapshot()));
        now = 62_000;
        Stopwatch restored = create();
        restored.restore(saved);
        assertTrue(restored.isRunning());
        assertEquals(62_000, restored.elapsed());
        assertEquals(60_750, restored.currentLap());
        assertEquals(Long.valueOf(1_250), restored.laps().get(0));
    }

    @Test public void pausedSessionSurvivesProcessRestartAndReboot() {
        stopwatch.start();
        now = 1_250;
        stopwatch.lap();
        now = 2_000;
        stopwatch.pause();
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 100;
        wall += 120_000;
        boot++;
        Stopwatch restored = create();
        restored.restore(saved);
        assertFalse(restored.isRunning());
        assertEquals(2_000, restored.elapsed());
        assertEquals(750, restored.currentLap());
        restored.start();
        now = 1_100;
        assertEquals(3_000, restored.elapsed());
    }

    @Test public void runningSessionCountsTimeThroughReboot() {
        stopwatch.start();
        now = 2_000;
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 100;
        wall += 120_000;
        boot++;
        Stopwatch restored = create();
        restored.restore(saved);
        assertEquals(122_000, restored.elapsed());
        now += 500;
        restored.lap();
        assertEquals(Long.valueOf(122_500), restored.laps().get(0));
    }

    @Test public void wallClockChangeDoesNotAffectRecoveryOnSameBoot() {
        stopwatch.start();
        now = 2_000;
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 5_000;
        wall += 3_600_000;
        Stopwatch restored = create();
        restored.restore(saved);
        assertEquals(5_000, restored.elapsed());
    }

    @Test public void multipleRebootsDoNotCountOldTimeTwice() {
        stopwatch.start();
        now = 1_000;
        Stopwatch.Snapshot first = stopwatch.snapshot();
        now = 100;
        wall += 10_000;
        boot++;
        Stopwatch restored = create();
        restored.restore(first);
        now += 500;
        wall += 500;
        Stopwatch.Snapshot second = restored.snapshot();
        now = 20;
        wall += 5_000;
        boot++;
        Stopwatch twiceRestored = create();
        twiceRestored.restore(second);
        assertEquals(16_500, twiceRestored.elapsed());
    }

    @Test public void unavailableBootCounterUsesWallClockRecovery() {
        boot = -1;
        stopwatch.start();
        now = 1_000;
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 5_000;
        wall += 4_000;
        Stopwatch restored = create();
        restored.restore(saved);
        assertEquals(5_000, restored.elapsed());
    }

    @Test public void rebootWithClockMovedBackDoesNotProduceNegativeTime() {
        stopwatch.start();
        now = 1_000;
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 10;
        wall -= 100_000;
        boot++;
        Stopwatch restored = create();
        restored.restore(saved);
        assertEquals(1_000, restored.elapsed());
    }

    @Test public void resetStateRoundTripsWithoutRevivingAnOldSession() {
        stopwatch.start();
        now = 1_000;
        stopwatch.lap();
        stopwatch.pause();
        stopwatch.reset();
        Stopwatch restored = create();
        restored.restore(SessionCodec.decode(SessionCodec.encode(stopwatch.snapshot())));
        assertEquals(0, restored.elapsed());
        assertFalse(restored.isRunning());
        assertTrue(restored.laps().isEmpty());
    }

    @Test public void savedLapsDoNotChangeWhenLiveSessionChanges() {
        stopwatch.start();
        now = 1_000;
        stopwatch.lap();
        Stopwatch.Snapshot saved = stopwatch.snapshot();
        now = 2_000;
        stopwatch.lap();
        assertEquals(1, saved.laps.size());
        assertEquals(2, stopwatch.laps().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteSavedDataIsRejected() {
        SessionCodec.decode("version=1\nrunning=true\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void decreasingSavedLapTimesAreRejected() {
        SessionCodec.decode("version=1\nrunning=false\nelapsed=2000\nmonotonic=2000\n"
                + "wall=1000000\nboot=4\nlaps=1500,1000\n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void LapBeyondSavedElapsedTimeIsRejected() {
        SessionCodec.decode("version=1\nrunning=false\nelapsed=2000\nmonotonic=2000\n"
                + "wall=1000000\nboot=4\nlaps=2500\n");
    }
}
