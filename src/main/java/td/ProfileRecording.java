package td;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import td.ui.BoardRenderer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        printByPhase("CPU samples", samples, event -> 1L);
        List<RecordedEvent> allocations = ofType(events, ALLOCATION_SAMPLE);
        Function<RecordedEvent, Long> kilobytes = event -> event.getLong("weight") / BYTES_PER_KB;
        // Setup allocates between sparse samples, and a sample carries all the bytes since the last one.
        List<RecordedEvent> played = allocations.stream().filter(event -> Phase.of(frames(event)) != Phase.OTHER)
                .toList();
        printTop("Allocated KB (sampled estimate) in tick and frame build, by game method and class", played, kilobytes,
                event -> gameFrame(event) + "  " + event.getClass("objectClass").getName());
        printByPhase("Allocated KB", allocations, kilobytes);
        long cut = samples.stream().filter(event -> event.getStackTrace() != null && event.getStackTrace().isTruncated())
                .count();
        if (cut > 0) {
            System.out.printf("%d of %d CPU stacks were cut short, so their phase reads as other; rerun with "
                    + "MAVEN_OPTS=-XX:FlightRecorderOptions:stackdepth=256%n", cut, samples.size());
        }
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

    /**
     * Each phase's share of {@code events}, then for the tick and the frame build the game methods with the most of
     * theirs, counting a method once per event wherever it sits on the stack: the method with all it calls.
     */
    private static void printByPhase(String title, List<RecordedEvent> events, Function<RecordedEvent, Long> weight) {
        Map<Phase, Long> totals = new EnumMap<>(Phase.class);
        Map<Phase, Map<String, Long>> inclusive = new EnumMap<>(Phase.class);
        for (RecordedEvent event : events) {
            List<RecordedFrame> frames = frames(event);
            Phase phase = Phase.of(frames);
            long weighed = weight.apply(event);
            totals.merge(phase, weighed, Long::sum);
            Map<String, Long> methods = inclusive.computeIfAbsent(phase, unused -> new HashMap<>());
            frames.stream().filter(ProfileRecording::isGame).map(ProfileRecording::name).distinct()
                    .forEach(method -> methods.merge(method, weighed, Long::sum));
        }
        System.out.println("-- " + title + " by phase: " + totals);
        for (Phase phase : List.of(Phase.TICK, Phase.FRAME_BUILD)) {
            long all = totals.getOrDefault(phase, 0L);
            System.out.println("   " + phase + ", with all it calls (share of the " + phase + ")");
            inclusive.getOrDefault(phase, Map.of()).entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(ROWS)
                    .forEach(entry -> System.out.printf("%8d  %5.1f%%  %s%n", entry.getValue(),
                            all == 0 ? 0.0 : 100.0 * entry.getValue() / all, entry.getKey()));
        }
    }

    /** What a sample was doing, told by the entry point on its stack. */
    private enum Phase {
        TICK(GameEngine.class.getName(), "doTick"), FRAME_BUILD(BoardRenderer.class.getName(), "buildFrame"),
        OTHER("", "");

        private final String type;
        private final String method;

        Phase(String type, String method) {
            this.type = type;
            this.method = method;
        }

        static Phase of(List<RecordedFrame> frames) {
            for (Phase phase : List.of(FRAME_BUILD, TICK)) {
                if (frames.stream().anyMatch(frame -> frame.getMethod().getName().equals(phase.method)
                        && frame.getMethod().getType().getName().equals(phase.type))) {
                    return phase;
                }
            }
            return OTHER;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }

    private static boolean isGame(RecordedFrame frame) {
        String type = frame.getMethod().getType().getName();
        return type.startsWith(GAME_PACKAGE) && !type.equals(PerformanceHarness.class.getName())
                && !frame.getMethod().isHidden();
    }

    private static String topFrame(RecordedEvent event) {
        return frames(event).stream().findFirst().map(ProfileRecording::name).orElse("(no stack)");
    }

    /** The innermost frame in game code, so time spent in the JDK lands on the game method that asked for it. */
    private static String gameFrame(RecordedEvent event) {
        return frames(event).stream().filter(ProfileRecording::isGame).findFirst().map(ProfileRecording::name).orElse("(outside the game)");
    }

    private static List<RecordedFrame> frames(RecordedEvent event) {
        return Optional.ofNullable(event.getStackTrace()).map(RecordedStackTrace::getFrames).orElse(List.of());
    }

    private static String name(RecordedFrame frame) {
        String type = frame.getMethod().getType().getName();
        return type.substring(type.lastIndexOf('.') + 1) + "." + frame.getMethod().getName();
    }
}
