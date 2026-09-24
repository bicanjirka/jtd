package td;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Diverts one class's log into memory for the duration of a test, so a test that provokes an
 * error can assert on it instead of printing a stack trace into a passing build.
 */
final class CapturedLog implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender;
    private final boolean wasAdditive;

    CapturedLog(Class<?> source) {
        this.logger = (Logger) LoggerFactory.getLogger(source);
        this.appender = new ListAppender<>();
        this.appender.start();
        this.wasAdditive = this.logger.isAdditive();
        this.logger.addAppender(this.appender);
        this.logger.setAdditive(false);
    }

    /** Read only after the logging thread has been joined; the backing list is not thread-safe. */
    List<ILoggingEvent> atLevel(Level level) {
        return this.appender.list.stream().filter(event -> event.getLevel() == level).toList();
    }

    @Override
    public void close() {
        this.logger.setAdditive(this.wasAdditive);
        this.logger.detachAppender(this.appender);
        this.appender.stop();
    }
}
