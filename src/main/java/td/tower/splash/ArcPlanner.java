package td.tower.splash;

import td.enemy.EnemyMob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Where a shot's arcs run: from enemy to nearest enemy the shot hasn't hit yet, jump by jump. Pure:
 * it picks the strikes and the tower lands them.
 */
public final class ArcPlanner {

    /** The jump that forks into two arcs when the spec forks. */
    private static final int FORK_JUMP = 3;

    private ArcPlanner() {
    }

    /**
     * The arcs a shot makes.
     *
     * @param primary    the enemy the shot aimed at; an arc with nowhere to go returns to it
     * @param start      the enemy the first arc jumps from
     * @param candidates the enemies an arc may strike
     * @param struck     the enemies the shot has already hit; an arc never jumps to one
     * @param spec       how the arcs run
     * @param reachFrom  how far, in pixels, an arc jumps from an enemy
     */
    public static List<ArcStrike> plan(EnemyMob primary, EnemyMob start, List<EnemyMob> candidates,
            Set<EnemyMob> struck, ArcSpec spec, ToDoubleFunction<EnemyMob> reachFrom) {
        List<ArcStrike> strikes = new ArrayList<>();
        Set<EnemyMob> hit = Collections.newSetFromMap(new IdentityHashMap<>());
        hit.addAll(struck);
        List<EnemyMob> heads = List.of(start);
        int returnsLeft = spec.returns();
        for (int jump = 1; jump <= spec.jumps() && !heads.isEmpty(); jump++) {
            List<EnemyMob> next = new ArrayList<>();
            for (int h = 0; h < heads.size(); h++) {
                EnemyMob head = heads.get(h);
                int branches = spec.forks() && jump == FORK_JUMP && h == 0 ? 2 : 1;
                for (int b = 0; b < branches; b++) {
                    Optional<EnemyMob> to = nearest(head, candidates, hit, reachFrom.applyAsDouble(head));
                    if (to.isPresent()) {
                        strikes.add(new ArcStrike(head, to.get(), spec.share()));
                        hit.add(to.get());
                        next.add(to.get());
                    } else if (returnsLeft > 0 && !primary.isDead()) {
                        strikes.add(new ArcStrike(head, primary, 1f));
                        returnsLeft--;
                        next.add(primary);
                    }
                }
            }
            heads = next;
        }
        return strikes;
    }

    private static Optional<EnemyMob> nearest(EnemyMob from, List<EnemyMob> candidates, Set<EnemyMob> hit,
            double reach) {
        double best = reach * reach;
        EnemyMob nearest = null;
        for (EnemyMob candidate : candidates) {
            if (hit.contains(candidate) || !candidate.validTarget()) {
                continue;
            }
            double dx = candidate.getX() - from.getX();
            double dy = candidate.getY() - from.getY();
            double d2 = dx * dx + dy * dy;
            if (d2 <= best) {
                best = d2;
                nearest = candidate;
            }
        }
        return Optional.ofNullable(nearest);
    }
}
