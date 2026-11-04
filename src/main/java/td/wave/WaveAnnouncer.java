package td.wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The "a wave just started" broadcast. */
public class WaveAnnouncer {

    private final List<WaveStartListener> listeners = new CopyOnWriteArrayList<>();
    // Announced on the game loop and read wherever a tower is inspected.
    private volatile long started = 0;

    /** How many waves have started. */
    public long started() {
        return this.started;
    }

    public void addListener(WaveStartListener l) {
        this.listeners.add(l);
    }

    public void removeListener(WaveStartListener l) {
        this.listeners.remove(l);
    }

    public void announce() {
        this.started++;
        for (WaveStartListener l : this.listeners) {
            l.waveStarted();
        }
    }
}
