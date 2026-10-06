package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Visible enemies within a half-width of the line segment between two points, nearest the start
 * first: whatever a beam through that segment passes.
 */
public final class OnSegmentTargetQuery implements TargetQuery {

    private final double fromX;
    private final double fromY;
    private final double dx;
    private final double dy;
    private final double lengthSquared;
    private final double halfWidth;

    public OnSegmentTargetQuery(double fromX, double fromY, double toX, double toY, double halfWidth) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.dx = toX - fromX;
        this.dy = toY - fromY;
        this.lengthSquared = this.dx * this.dx + this.dy * this.dy;
        this.halfWidth = halfWidth;
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (e.canBeTargeted() && this.alongSegment(e) >= 0 && this.distanceToSegmentSquared(e) <= this.halfWidth * this.halfWidth) {
                matches.add(e);
            }
        }
        matches.sort(Comparator.comparingDouble(this::alongSegment));
        return matches;
    }

    /** How far along the segment the enemy's projection falls, from {@code 0} at the start to {@code 1} at the end. */
    private double alongSegment(EnemyMob e) {
        if (this.lengthSquared == 0) {
            return 0;
        }
        return ((e.getX() - this.fromX) * this.dx + (e.getY() - this.fromY) * this.dy) / this.lengthSquared;
    }

    private double distanceToSegmentSquared(EnemyMob e) {
        double t = Math.max(0, Math.min(1, this.alongSegment(e)));
        double nearestX = this.fromX + t * this.dx;
        double nearestY = this.fromY + t * this.dy;
        double ex = e.getX() - nearestX;
        double ey = e.getY() - nearestY;
        return ex * ex + ey * ey;
    }
}
