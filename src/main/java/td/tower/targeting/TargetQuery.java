package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/**
 * Which enemies are legal targets now, as a fresh snapshot.
 * <p>
 * {@link #and} intersects two queries and {@link #or} joins them. {@link #all()} is its identity and {@link #none()} its
 * absorber, which short-circuits.
 */
public interface TargetQuery {

    static TargetQuery all() {
        return AllTargetQuery.INSTANCE;
    }

    static TargetQuery none() {
        return NoneTargetQuery.INSTANCE;
    }

    List<EnemyMob> matching(EnemyRegistry enemies);

    default TargetQuery and(TargetQuery other) {
        return new IntersectingTargetQuery(this, other);
    }

    /** Whatever either query matches. */
    default TargetQuery or(TargetQuery other) {
        return new UnionTargetQuery(this, other);
    }
}
