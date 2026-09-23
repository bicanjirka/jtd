package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate with the most health remaining, the one with the most left to lose -
 * what a Sniper switches to once specialized into its {@code SPECIAL} slot, so its now more
 * expensive, gated shots stop finishing off enemies that were already nearly dead.
 */
public final class HighestHealthSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingInt(EnemyMob::getHealth));
    }
}
