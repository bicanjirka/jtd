package td.wave;

/**
 * Notified when a wave starts and its enemy roster has been installed. {@code TowerThree} is
 * the only subscriber, resetting the per-enemy arrays its round robin indexes into.
 * <p>
 * Fired from {@link WaveAnnouncer} on the {@code game-loop} thread, since wave start happens
 * during a tick.
 */
@FunctionalInterface
public interface WaveStartListener {
    void waveStarted();
}
