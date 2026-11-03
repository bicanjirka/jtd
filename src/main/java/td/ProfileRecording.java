package td;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A Flight Recorder recording of a harness run, and the summary printed from it: where the CPU samples land, which
 * game code allocates, and what the collector paused for. A dev tool, like the harness that starts it.
 */
final class ProfileRecording {

    private static final Path FILE = Path.of("target", "perf.jfr");
    private static final String GAME_PACKAGE = "td.";
    private static final int ROWS = 15;
    private static final int BYTES_PER_KB = 1024;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final String EXECUTION_SAMPLE = "jdk.ExecutionSample";
    private static final String ALLOCATION_SAMPLE = "jdk.ObjectAllocationSample";
    private static final String GARBAGE_COLLECTION = "jdk.GarbageCollection";

    private final Recording recording;

    private ProfileRecording(Recording recording) {
        this.recording = recording;
    }

    /** Starts recording CPU samples, sampled allocations and collections. */
    static ProfileRecording started() {
        Recording recording = new Recording();
        recording.enable(EXECUTION_SAMPLE).withPeriod(Duration.ofMillis(1));
        recording.enable(ALLOCATION_SAMPLE).with("throttle", "2000/s").withStackTrace();
        recording.enable(GARBAGE_COLLECTION);
        recording.start();
        return new ProfileRecording(recording);
    }

    /** Stops, writes the recording to {@link #FILE} and prints its summary. */
    void stopAndReport() {
        try {
            this.recording.stop();
            Files.createDirectories(FILE.getParent());
            this.recording.dump(FILE);
            this.recording.close();
            print(RecordingFile.readAllEvents(FILE));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + FILE, e);
        }
    }

    private static void print(List<RecordedEvent> events) {
        System.out.println("=== Profile: " + FILE.toAbsolutePath() + " (timings above ran under the recorder) ===");
        List<RecordedEvent> samples = ofType(events, EXECUTION_SAMPLE);
        printTop("CPU samples, the method running", samples, event -> 1L, ProfileRecording::topFrame);
        printTop("CPU samples, the game method running or calling", samples, event -> 1L, ProfileRecording::gameFrame);
        printTop("Allocated KB (sampled estimate), by game method and class", ofType(events, ALLOCATION_SAMPLE),
                event -> event.getLong("weight") / BYTES_PER_KB,
                event -> gameFrame(event) + "  " + event.getClass("objectClass").getName());
        List<RecordedEvent> collections = ofType(events, GARBAGE_COLLECTION);
        double pausedMillis = collections.stream().mapToLong(event -> event.getLong("sumOfPauses")).sum()
                / NANOS_PER_MILLI;
        double longestMillis = collections.stream().mapToLong(event -> event.getLong("longestPause")).max().orElse(0L)
                / NANOS_PER_MILLI;
        System.out.printf("GC: %d collections, %.1f ms paused, longest %.2f ms%n", collections.size(), pausedMillis,
                longestMillis);
    }

    private static List<RecordedEvent> ofType(List<RecordedEvent> events, String type) {
        return events.stream().filter(event -> event.getEventType().getName().equals(type)).toList();
    }

    private static void printTop(String title, List<RecordedEvent> events, Function<RecordedEvent, Long> weight,
            Function<RecordedEvent, String> key) {
        Map<String, Long> totals = new HashMap<>();
        events.forEach(event -> totals.merge(key.apply(event), weight.apply(event), Long::sum));
        long all = totals.values().stream().reduce(0L, Long::sum);
        System.out.println("-- " + title + " (total " + all + ")");
        totals.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(ROWS)
                .forEach(entry -> System.out.printf("%8d  %5.1f%%  %s%n", entry.getValue(),
                        all == 0 ? 0.0 : 100.0 * entry.getValue() / all, entry.getKey()));
    }

    private static String topFrame(RecordedEvent event) {
        return frames(event).stream().findFirst().map(ProfileRecording::name).orElse("(no stack)");
    }

    /** The innermost frame in game code, so time spent in the JDK lands on the game method that asked for it. */
    private static String gameFrame(RecordedEvent event) {
        return frames(event).stream().filter(frame -> frame.getMethod().getType().getName().startsWith(GAME_PACKAGE))
                .findFirst().map(ProfileRecording::name).orElse("(outside the game)");
    }

    private static List<RecordedFrame> frames(RecordedEvent event) {
        return Optional.ofNullable(event.getStackTrace()).map(RecordedStackTrace::getFrames).orElse(List.of());
    }

    private static String name(RecordedFrame frame) {
        String type = frame.getMethod().getType().getName();
        return type.substring(type.lastIndexOf('.') + 1) + "." + frame.getMethod().getName();
    }
}
