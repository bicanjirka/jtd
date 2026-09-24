package td.wave;

/** Notified on the game-loop thread when a wave starts and its roster is installed. */
@FunctionalInterface
public interface WaveStartListener {
    void waveStarted();
}
