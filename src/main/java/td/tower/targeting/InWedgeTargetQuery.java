package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.TurretAim;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemies within a wedge (a cone) extending from a point in a facing direction. Unlike
 * {@code SonarSweep}, which decides hits against the arc swept <em>since the last tick</em>
 * because its beam continuously rotates, a wedge is static or only slowly reorients, so it is
 * always tested against its <em>current</em> heading - there is no "missed it between ticks"
 * case to guard against here. Meant to be combined via {@link TargetQuery#and} with an
 * {@link InRangeTargetQuery} bounding the wedge's reach.
 */
public final class InWedgeTargetQuery implements TargetQuery {

    private final int x;
    private final int y;
    private final double headingRadians;
    private final double halfWidthRadians;

    public InWedgeTargetQuery(int x, int y, double headingRadians, double halfWidthRadians) {
        if (halfWidthRadians <= 0) {
            throw new IllegalArgumentException("halfWidthRadians must be positive: " + halfWidthRadians);
        }
        this.x = x;
        this.y = y;
        this.headingRadians = headingRadians;
        this.halfWidthRadians = halfWidthRadians;
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (e.validTarget() && this.withinWedge(e)) {
                matches.add(e);
            }
        }
        return matches;
    }

    private boolean withinWedge(EnemyMob e) {
        double bearing = TurretAim.angleTo(this.x, this.y, e.getX(), e.getY());
        double diff = TurretAim.normalizeRadians(bearing - this.headingRadians);
        return Math.abs(diff) <= this.halfWidthRadians;
    }
}
