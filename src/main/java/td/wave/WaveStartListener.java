package td.wave;

/**
 * Notified when a wave starts and its enemy roster has been installed. {@code SonarTower} is
 * the only subscriber, discarding the hit markers its scan left on the previous wave's
 * enemies.
 * <p>
 * Fired from {@link WaveAnnouncer} on the {@code game-loop} thread, since wave start happens
 * during a tick.
 */
@FunctionalInterface
public interface WaveStartListener {
    void waveStarted();
}
