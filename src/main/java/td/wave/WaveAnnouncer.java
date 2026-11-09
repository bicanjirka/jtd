package td.wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The "a wave just started" broadcast. */
public class WaveAnnouncer {

    private final List<WaveStartListener> listeners = new CopyOnWriteArrayList<>();
    // Announced on the game loop and read wherever a tower is inspected.
    private volatile long started = 0;
    private volatile boolean betweenWaves = true;
    // The tick the wave out started on: what keeps time inside a wave, so a loaded game keeps it alike.
    private volatile int startTick = 0;

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

    /** Whether no wave is out: before the first, and from a wave's clear until the next starts. */
    public boolean betweenWaves() {
        return this.betweenWaves;
    }

    /** How many ticks {@code gameTime} is into the last wave started; what a beat on the wave's clock counts. */
    public int tickOfWave(int gameTime) {
        return gameTime - this.startTick;
    }

    /** The wave out is over, or the board was cleared of it. */
    public void ended() {
        this.betweenWaves = true;
    }

    /** A level starts over: no wave out, and the wave clock from zero. */
    public void reset() {
        this.betweenWaves = true;
        this.startTick = 0;
    }

    /** A wave starts on tick {@code gameTime}. */
    public void announce(int gameTime) {
        this.startTick = gameTime;
        this.betweenWaves = false;
        this.started++;
        for (WaveStartListener l : this.listeners) {
            l.waveStarted();
        }
    }
}
