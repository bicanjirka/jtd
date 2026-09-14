package td.effect;

import td.damage.Damage;

/**
 * Where a damage-over-time {@link Effect} sends the damage it deals each tick, with the
 * target already captured by whoever produced the effect. A tower producing a burn binds
 * this to its own {@code dealDamage(enemy, damage)}, so a damage-over-time tick is credited
 * to the tower that applied it exactly like an instant hit is.
 * <p>
 * Deliberately one argument, not {@code apply(EnemyMob, Damage)}: a two-argument form would
 * force this package to depend on {@code td.enemy}, while {@code td.enemy} already needs to
 * depend on this package to hold an enemy's active effects. Keeping the target bound at
 * production time avoids that cycle.
 */
@FunctionalInterface
public interface DamageSink {
    void apply(Damage damage);
}
