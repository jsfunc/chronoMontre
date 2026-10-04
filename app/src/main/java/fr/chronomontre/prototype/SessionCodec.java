package fr.chronomontre.prototype;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Versioned encoding of the single unfinished session. */
final class SessionCodec {
    static String encode(Stopwatch.Snapshot saved) {
        return "version=1\nrunning=" + saved.running
                + "\nelapsed=" + saved.elapsedMillis
                + "\nmonotonic=" + saved.monotonicMillis
                + "\nwall=" + saved.wallMillis
                + "\nboot=" + saved.bootCount
                + "\nlaps=" + saved.laps.stream().map(String::valueOf)
                        .collect(Collectors.joining(",")) + "\n";
    }

    static Stopwatch.Snapshot decode(String encoded) {
        Properties values = new Properties();
        try {
            values.load(new StringReader(encoded));
        } catch (IOException error) {
            throw new IllegalArgumentException("Unreadable session", error);
        }
        if (!"1".equals(required(values, "version"))) {
            throw new IllegalArgumentException("Unsupported session version");
        }
        String running = required(values, "running");
        if (!running.equals("true") && !running.equals("false")) {
            throw new IllegalArgumentException("Invalid running state");
        }
        List<Long> laps = new ArrayList<>();
        String encodedLaps = required(values, "laps");
        if (!encodedLaps.isEmpty()) {
            for (String value : encodedLaps.split(",", -1)) laps.add(Long.parseLong(value));
        }
        return new Stopwatch.Snapshot(Boolean.parseBoolean(running),
                Long.parseLong(required(values, "elapsed")),
                Long.parseLong(required(values, "monotonic")),
                Long.parseLong(required(values, "wall")),
                Integer.parseInt(required(values, "boot")), laps);
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing session field: " + key);
        return value;
    }
}
