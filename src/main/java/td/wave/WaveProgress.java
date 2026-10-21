package td.wave;

import java.util.List;

/**
 * How far through a level's rounds the player is, as one value built from a single level snapshot,
 * so an index is never paired with another level's count.
 *
 * @param index   rounds started so far, which is also the 0-based index of the next
 * @param rounds  every round in the level, in order
 * @param current the round in play, one wave per path; empty before the first
 * @param next    the round starting next would run; empty after the last
 */
public record WaveProgress(int index, List<RoundSummary> rounds, List<Wave> current, List<Wave> next) {

    public WaveProgress {
        rounds = List.copyOf(rounds);
    }

    public static WaveProgress none() {
        return new WaveProgress(0, List.of(), List.of(), List.of());
    }

    /** Rounds in the level. */
    public int count() {
        return this.rounds.size();
    }

    public boolean hasNextWave() {
        return this.index < this.count();
    }

    /** 1-based, as the HUD shows it. */
    public int currentNumber() {
        return this.index;
    }

    /** 1-based, as the HUD shows it. */
    public int nextNumber() {
        return this.index + 1;
    }
}
