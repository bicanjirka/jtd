package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;

/** Enemies of one type anywhere on the board; combine with a range query. */
public final class OfTypeTargetQuery implements TargetQuery {

    private final EnemyMob.Type type;

    private OfTypeTargetQuery(EnemyMob.Type type) {
        this.type = type;
    }

    public static OfTypeTargetQuery of(EnemyMob.Type type) {
        return new OfTypeTargetQuery(type);
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (e.validTarget(this.type)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
