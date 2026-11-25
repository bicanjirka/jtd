package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate whose group, itself and the others within a radius of it, weighs most once scaled by how far
 * along its path it is: a crowd near the exit before a crowd that just came in, and a lone enemy about to leak before a
 * pair far behind it. Among equals, the one closest to leaking.
 */
public final class GroupAheadSelector implements TargetSelector {

    private final float radius;

    public GroupAheadSelector(float radius) {
        this.radius = radius;
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingDouble((EnemyMob e) -> this.weightOf(e, candidates))
                .thenComparingInt(EnemyMob::getProgression));
    }

    private float weightOf(EnemyMob enemy, List<EnemyMob> candidates) {
        float radius2 = this.radius * this.radius;
        int group = 0;
        for (EnemyMob other : candidates) {
            if (WithinRange.of(other, (int) enemy.getX(), (int) enemy.getY(), radius2)) {
                group++;
            }
        }
        return group * enemy.pathShare();
    }
}
