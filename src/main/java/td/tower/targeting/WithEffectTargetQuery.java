package td.tower.targeting;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.ArrayList;
import java.util.List;

/** Visible enemies within a radius of a point that carry at least one of some effects. */
public final class WithEffectTargetQuery implements TargetQuery {

    private final int x;
    private final int y;
    private final float range;
    private final List<EffectKind> kinds;

    public WithEffectTargetQuery(int x, int y, float range, EffectKind... kinds) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.kinds = List.of(kinds);
    }

    @Override
    public List<EnemyMob> matching(EnemyRegistry enemyRegistry) {
        float range2 = this.range * this.range;
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : enemyRegistry.getEnemies()) {
            if (e.canBeTargeted() && WithinRange.of(e, this.x, this.y, range2) && this.kinds.stream().anyMatch(e::hasEffect)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
