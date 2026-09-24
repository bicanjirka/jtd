package td.effect;

import td.damage.Damage;

/**
 * Where a damage-over-time {@link Effect} sends each tick's damage, with the target already bound,
 * so the tick is credited to the tower that applied it.
 * <p>
 * One argument on purpose: taking the enemy too would make this package depend on {@code td.enemy},
 * which already depends on it.
 */
@FunctionalInterface
public interface DamageSink {
    void apply(Damage damage);
}
