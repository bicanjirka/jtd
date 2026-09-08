package td.tower.targeting;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;

import java.util.List;

/**
 * Answers "which enemies are legal targets right now", as a fresh immutable snapshot - never
 * the registry's live enemy array itself. Replaces each tower hand-rolling its own scan
 * over {@code enemies.getEnemies()}.
 *
 * <p>{@link #and} composes two queries into their intersection. {@link #all()} is the
 * identity element - {@code all().and(x)} matches exactly what {@code x} matches - and
 * {@link #none()} is the absorber - {@code none().and(x)} is always empty, and short-circuits
 * without ever evaluating {@code x}.
 */
public interface TargetQuery {

    List<EnemyMob> matching(EnemyRegistry enemies);

    default TargetQuery and(TargetQuery other) {
        return new IntersectingTargetQuery(this, other);
    }

    static TargetQuery all() {
        return AllTargetQuery.INSTANCE;
    }

    static TargetQuery none() {
        return NoneTargetQuery.INSTANCE;
    }
}
