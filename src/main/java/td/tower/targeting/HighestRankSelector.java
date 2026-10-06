package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the candidate of the highest rank; among equals, the one closest to leaking. */
public final class HighestRankSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparing(EnemyMob::getRank).thenComparingInt(EnemyMob::getProgression));
    }
}
