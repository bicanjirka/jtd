package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate closest to leaking - the one with the highest
 * {@link EnemyMob#getProgression()}. Ties are broken arbitrarily, which is fine: two enemies
 * at the same integer progression are within a pixel of each other.
 */
public final class FurthestAlongPathSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingInt(EnemyMob::getProgression));
    }
}
