package td.tower.targeting;

import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the candidate with the most other candidates within a radius of it, so an area or a bounce
 * from it finds the most; among equals, the one closest to leaking.
 */
public final class MostNeighboursSelector implements TargetSelector {

    private final float radius;

    public MostNeighboursSelector(float radius) {
        this.radius = radius;
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().max(Comparator.comparingInt((EnemyMob e) -> this.neighboursOf(e, candidates))
                .thenComparingInt(EnemyMob::getProgression));
    }

    private int neighboursOf(EnemyMob enemy, List<EnemyMob> candidates) {
        float radius2 = this.radius * this.radius;
        int neighbours = 0;
        for (EnemyMob other : candidates) {
            if (other != enemy && WithinRange.of(other, (int) enemy.getX(), (int) enemy.getY(), radius2)) {
                neighbours++;
            }
        }
        return neighbours;
    }
}
