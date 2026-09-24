package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The application entry point, and the one fatal-startup boundary: anything thrown while
 * building the game - notably {@link td.util.GameStartupException} - is logged here and
 * exits non-zero, rather than each failing component showing its own dialog and calling
 * {@code System.exit} itself.
 * <p>
 * Also owns per-run log file naming, which is why {@code jtd.logTimestamp} is set as the
 * very first statement in {@link #main}: {@code logback.xml} resolves it while configuring
 * itself, so any class touching SLF4J before that point would fix the property's absence
 * into the whole run's log filename.
 */
public class Main {

    private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int LOG_FILES_TO_KEEP = 20;

    static void main(String[] args) throws Exception {
        // must run before any class touches SLF4J/Logback, so logback.xml sees it
        System.setProperty("jtd.logTimestamp", LocalDateTime.now().format(LOG_TIMESTAMP));
        // Returns rather than logs: the logger is not configured yet.
        List<File> undeleted = pruneOldLogs();

        Logger log = LoggerFactory.getLogger(Main.class);
        undeleted.forEach(f -> log.warn("Could not delete old log file {}", f));
        log.info("jTD {} starting, logging to logs/jTD-{}.log", TowerDefense.VERSION, System.getProperty("jtd.logTimestamp"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("jTD shutting down")));

        // invokeAndWait: Swing is built on the EDT, and a build failure is reported here rather
        // than lost in the EDT's uncaught-exception handler.
        AtomicReference<Exception> startupFailure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                new TowerDefense();
            } catch (Exception e) {
                startupFailure.set(e);
            }
        });
        Exception failure = startupFailure.get();
        if (failure != null) {
            log.error("jTD failed to start", failure);
            System.exit(1);
        }
    }

    /**
     * Keeps the most recent {@value #LOG_FILES_TO_KEEP} run logs, so a long-lived checkout's
     * {@code logs/} does not grow without bound.
     *
     * @return the files it tried and failed to delete, for the caller to report once it has a
     * logger - never null, and empty in the ordinary case
     */
    private static List<File> pruneOldLogs() {
        File logDir = new File("logs");
        File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("jTD-") && name.endsWith(".log"));
        if (logFiles == null || logFiles.length <= LOG_FILES_TO_KEEP) {
            return List.of();
        }
        return Arrays.stream(logFiles)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .skip(LOG_FILES_TO_KEEP)
                .filter(f -> !f.delete())
                .toList();
    }

}
