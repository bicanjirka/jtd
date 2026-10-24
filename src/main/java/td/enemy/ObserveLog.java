package td.enemy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Where trace lines are written. */
@FunctionalInterface
public interface ObserveLog {

    /**
     * The session log, through its own logger, which {@code logback.xml} gives a queue that waits rather than drops
     * a line.
     */
    static ObserveLog toSessionLog() {
        Logger log = LoggerFactory.getLogger(EnemyObservation.class);
        return log::info;
    }

    void write(String line);
}
