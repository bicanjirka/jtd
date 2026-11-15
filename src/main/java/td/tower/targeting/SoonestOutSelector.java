package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate that would reach its exit first at the speed it walks now: its path left over its speed. One
 * number weighs both, so a fast enemy far back and a slow one near the exit compare by the time they have left, and a
 * stopped enemy, which has all the time there is, comes last. Among equals, the one closer to leaking.
 */
public final class SoonestOutSelector implements TargetSelector {

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().min(Comparator.comparingDouble(SoonestOutSelector::ticksToExit)
                .thenComparingDouble(EnemyMob::distanceToExit));
    }

    /** Ticks it would take to walk out; unbounded for one that stands still. */
    static double ticksToExit(EnemyMob enemy) {
        float speed = enemy.getSpeed();
        return speed <= 0f ? Double.POSITIVE_INFINITY : enemy.distanceToExit() / speed;
    }
}
