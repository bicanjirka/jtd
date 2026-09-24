package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate closest to leaking. Ties are within a pixel of each other, so any pick is
 * fine.
 */
public final class FurthestAlongPathSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingInt(EnemyMob::getProgression));
    }
}
