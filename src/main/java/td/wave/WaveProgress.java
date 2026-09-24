package td.wave;

import java.util.List;

/**
 * How far through a level's rounds the player is, as one value built from a single level snapshot,
 * so an index is never paired with another level's count.
 *
 * @param index   rounds started so far, which is also the 0-based index of the next
 * @param count   rounds in the level
 * @param current the round in play, one wave per path; empty before the first
 * @param next    the round starting next would run; empty after the last
 */
public record WaveProgress(int index, int count, List<Wave> current, List<Wave> next) {

    public static WaveProgress none() {
        return new WaveProgress(0, 0, List.of(), List.of());
    }

    public boolean hasNextWave() {
        return this.index < this.count;
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
