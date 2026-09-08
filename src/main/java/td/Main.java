package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public class Main {

    private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int LOG_FILES_TO_KEEP = 20;

    static void main(String[] args) {
        // must run before any class touches SLF4J/Logback, so logback.xml sees it
        System.setProperty("jtd.logTimestamp", LocalDateTime.now().format(LOG_TIMESTAMP));
        pruneOldLogs();

        Logger log = LoggerFactory.getLogger(Main.class);
        log.info("jTD {} starting, logging to logs/jTD-{}.log", TowerDefense.VERSION, System.getProperty("jtd.logTimestamp"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("jTD shutting down")));

        try {
            @SuppressWarnings("unused")
            TowerDefense game = new TowerDefense();
        } catch (Throwable e) {
            log.error("jTD failed to start", e);
            System.exit(1);
        }
    }

    private static void pruneOldLogs() {
        File logDir = new File("logs");
        File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("jTD-") && name.endsWith(".log"));
        if (logFiles == null || logFiles.length <= LOG_FILES_TO_KEEP) {
            return;
        }
        Arrays.stream(logFiles)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .skip(LOG_FILES_TO_KEEP)
                .forEach(File::delete);
    }

}
