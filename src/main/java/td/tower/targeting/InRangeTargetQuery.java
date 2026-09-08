package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Enemies within a radius of a point, optionally restricted to one
 * {@link EnemyMob.type}. Use {@link #anyType} or {@link #ofType} - never
 * {@code null} - to say which.
 */
public final class InRangeTargetQuery implements TargetQuery {

    private final int x;
    private final int y;
    private final float range;
    private final Predicate<EnemyMob> isLegalType;

    private InRangeTargetQuery(int x, int y, float range, Predicate<EnemyMob> isLegalType) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.isLegalType = isLegalType;
    }

    public static InRangeTargetQuery anyType(int x, int y, float range) {
        return new InRangeTargetQuery(x, y, range, EnemyMob::validTarget);
    }

    public static InRangeTargetQuery ofType(int x, int y, float range, EnemyMob.type type) {
        return new InRangeTargetQuery(x, y, range, e -> e.validTarget(type));
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        float range2 = this.range * this.range;
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (this.isLegalType.test(e) && WithinRange.of(e, this.x, this.y, range2)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
