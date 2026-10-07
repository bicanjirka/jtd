package td.tower.splash;

import td.util.ThreadConfined;

import java.util.List;
import java.util.Optional;

/**
 * Whose turn it is in the Hexer's pool. Each cast takes the next hex that has a target, skipping
 * the ones that don't; an empty range sends the turn back to the first hex.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexTurn {

    private int next;

    /** The next hex in turn that has a target, and its target; empty when none in the pool has one. */
    public Optional<HexPick> take(List<Hex> pool, HexScene scene) {
        for (int i = 0; i < pool.size(); i++) {
            int index = (this.next + i) % pool.size();
            Hex hex = pool.get(index);
            Optional<HexPick> pick = hex.target(scene).map(target -> new HexPick(hex, target));
            if (pick.isPresent()) {
                this.next = (index + 1) % pool.size();
                return pick;
            }
        }
        return Optional.empty();
    }

    /** The next cast starts at the first hex again. */
    public void restart() {
        this.next = 0;
    }
}
