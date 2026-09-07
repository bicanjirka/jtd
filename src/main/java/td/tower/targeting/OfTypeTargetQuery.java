package td.tower.targeting;

import td.enemy.EnemyMob;
import td.util.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemies of one {@link EnemyMob.type}, anywhere on the board. Meant to be combined via
 * {@link TargetQuery#and} with a range query, e.g. to count how many in-range enemies are
 * ghosts without a tower hand-rolling that filter itself.
 */
public final class OfTypeTargetQuery implements TargetQuery {

    private final EnemyMob.type type;

    private OfTypeTargetQuery(EnemyMob.type type) {
        this.type = type;
    }

    public static OfTypeTargetQuery of(EnemyMob.type type) {
        return new OfTypeTargetQuery(type);
    }

    @Override
    public List<EnemyMob> matching(Context context) {
        List<EnemyMob> matches = new ArrayList<>();
        for (EnemyMob e : context.getEnemies()) {
            if (e.validTarget(this.type)) {
                matches.add(e);
            }
        }
        return matches;
    }
}
