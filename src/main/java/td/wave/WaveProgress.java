package td.wave;

import java.util.List;

/**
 * How far through a level's rounds the player is, as one consistent value.
 * <p>
 * Index and count are correlated: the index counts into the same round list the count measures.
 * Reading them through two separate calls let the UI pair an index from one level with a count
 * from the next while a level was being installed, and let a wave-preview lookup index past the
 * end of a list that had just been replaced. Asking for all of it at once is the fix -
 * {@code GameEngine.waveProgress()} builds this from a single {@code LoadedLevel} snapshot.
 * <p>
 * {@code current}/{@code next} hold one {@link Wave} per path, in path order - a round spawns
 * every path's wave together (see {@code td/wave/CLAUDE.md}'s round model), so the wave-info
 * panel shows one stacked strip per entry rather than a single wave.
 *
 * @param index   how many rounds have been started, so also the 0-based index of the next one
 * @param count   how many rounds the level has in total
 * @param current the round now in play, one wave per path, empty before the first one starts
 * @param next    the round that starting would run, empty once the last one has started
 */
public record WaveProgress(int index, int count, List<Wave> current, List<Wave> next) {

    /**
     * The state of a world with no level installed.
     */
    public static WaveProgress none() {
        return new WaveProgress(0, 0, List.of(), List.of());
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
