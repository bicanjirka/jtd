package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyTarget;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the candidate closest to a fixed point. */
public final class NearestSelector implements TargetSelector {

    private final double x;
    private final double y;

    public NearestSelector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Optional<EnemyMob> selectFrom(List<EnemyMob> candidates) {
        return candidates.stream().min(Comparator.comparingDouble(this::distance2));
    }

    private double distance2(EnemyTarget e) {
        double dx = e.getX() - this.x;
        double dy = e.getY() - this.y;
        return dx * dx + dy * dy;
    }
}
