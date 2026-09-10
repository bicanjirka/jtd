package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.OptionalInt;

/**
 * The round-robin scan behind {@code TowerThree}: the first enemy of the given type in range
 * at an index strictly greater than the one passed in. Scans forward only and does not wrap -
 * running out is the signal that every enemy in range has been hit once, which is what the
 * caller turns into a recharge.
 */
public final class InRangeAfterIndexQuery implements NextTargetQuery {

    private final int x;
    private final int y;
    private final float range;
    private final EnemyMob.type type;

    public InRangeAfterIndexQuery(int x, int y, float range, EnemyMob.type type) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.type = type;
    }

    @Override
    public OptionalInt nextIndexAfter(EnemyRegistry enemyRegistry, int index) {
        EnemyMob[] enemies = enemyRegistry.getEnemies();
        float range2 = this.range * this.range;
        for (int i = index + 1; i < enemies.length; i++) {
            EnemyMob e = enemies[i];
            if (e.validTarget(this.type) && WithinRange.of(e, this.x, this.y, range2)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }
}
