package td.wave;

import java.util.Optional;

/**
 * How far through a level's waves the player is, as one consistent value.
 * <p>
 * Index and count are correlated: the index counts into the same wave list the count measures.
 * Reading them through two separate calls let the UI pair an index from one level with a count
 * from the next while a level was being installed, and let a wave-preview lookup index past the
 * end of a list that had just been replaced. Asking for all of it at once is the fix -
 * {@code GameEngine.waveProgress()} builds this from a single {@code LoadedLevel} snapshot.
 *
 * @param index   how many waves have been started, so also the 0-based index of the next one
 * @param count   how many waves the level has in total
 * @param current the wave now in play, empty before the first one starts
 * @param next    the wave that starting would run, empty once the last one has started
 */
public record WaveProgress(int index, int count, Optional<Wave> current, Optional<Wave> next) {

    /**
     * The state of a world with no level installed.
     */
    public static WaveProgress none() {
        return new WaveProgress(0, 0, Optional.empty(), Optional.empty());
    }

    /**
     * Whether a wave remains to be started.
     */
    public boolean hasNextWave() {
        return this.index < this.count;
    }

    /**
     * The 1-based number of the wave in play, as the HUD shows it.
     */
    public int currentNumber() {
        return this.index;
    }

    /**
     * The 1-based number of the wave that starting would run.
     */
    public int nextNumber() {
        return this.index + 1;
    }
}
