package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.enemy.EnemyTarget;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Enemies within a radius of a point: {@link #visible} for what a tower may aim at, {@link
 * #everyone} for what an area effect touches, hidden enemies included.
 */
public final class InRangeTargetQuery implements TargetQuery {

    private final int x;
    private final int y;
    private final float range;
    private final Predicate<EnemyMob> isLegal;

    private InRangeTargetQuery(int x, int y, float range, Predicate<EnemyMob> isLegal) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.isLegal = isLegal;
    }

    public static InRangeTargetQuery everyone(int x, int y, float range) {
        return new InRangeTargetQuery(x, y, range, EnemyMob::validTarget);
    }

    public static InRangeTargetQuery visible(int x, int y, float range) {
        return new InRangeTargetQuery(x, y, range, EnemyMob::canBeTargeted);
    }

    /** {@link #everyone(int, int, float)} around {@code at}, cut to its whole pixel. */
    public static InRangeTargetQuery everyone(Vec2 at, float range) {
        return everyone((int) at.x(), (int) at.y(), range);
    }

    /** {@link #visible(int, int, float)} around {@code at}, cut to its whole pixel. */
    public static InRangeTargetQuery visible(Vec2 at, float range) {
        return visible((int) at.x(), (int) at.y(), range);
    }

    /** {@link #everyone(Vec2, float)} around where {@code center} stands, but {@code center} itself. */
    public static InRangeTargetQuery around(EnemyTarget center, float range) {
        Vec2 at = center.position();
        return new InRangeTargetQuery((int) at.x(), (int) at.y(), range,
                enemy -> enemy != center && enemy.validTarget());
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        float range2 = this.range * this.range;
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (this.isLegal.test(e) && WithinRange.of(e, this.x, this.y, range2)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
