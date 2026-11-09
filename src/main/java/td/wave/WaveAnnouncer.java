package td.wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The wave out: its start, broadcast to whoever listens, and the {@link WaveClock} it moves. */
public final class WaveAnnouncer {

    private final List<WaveStartListener> listeners = new CopyOnWriteArrayList<>();
    // Moved on the game loop, read wherever a tower is inspected or a refund worked out.
    private volatile WaveClock clock = WaveClock.first();

    /** Where the game is in its waves now, read as one. */
    public WaveClock clock() {
        return this.clock;
    }

    /** How many waves have started. */
    public long started() {
        return this.clock.started();
    }

    /** Whether no wave is out: before the first, and from a wave's clear until the next starts. */
    public boolean betweenWaves() {
        return !this.clock.out();
    }

    /** How many ticks {@code gameTime} is into the last wave started; what a beat on the wave's clock counts. */
    public int tickOfWave(int gameTime) {
        return gameTime - this.clock.startTick();
    }

    public void addListener(WaveStartListener l) {
        this.listeners.add(l);
    }

    public void removeListener(WaveStartListener l) {
        this.listeners.remove(l);
    }

    /** The wave out is over, or the board was cleared of it. */
    public void ended() {
        this.clock = this.clock.end();
    }

    /** A level starts over. */
    public void reset() {
        this.clock = this.clock.startOver();
    }

    /** A wave starts on tick {@code gameTime}. */
    public void announce(int gameTime) {
        this.clock = this.clock.start(gameTime);
        for (WaveStartListener l : this.listeners) {
            l.waveStarted();
        }
    }
}
