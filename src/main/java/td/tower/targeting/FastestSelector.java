package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate moving fastest right now; among equals, the one closest to leaking. A stopped
 * enemy is the slowest there is, so it drops out of the lead the moment it stops.
 */
public final class FastestSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingDouble(EnemyMob::getSpeed)
                .thenComparingInt(EnemyMob::getProgression));
    }
}
