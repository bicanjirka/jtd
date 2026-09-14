package td.enemy;

import td.damage.Damage;

/**
 * Reduces every incoming hit by a flat amount, clamped at zero rather than healing - the
 * Warden's armor, distinct in kind from {@link PercentResistTrait}'s percentage so the
 * feature demonstrates more than one shield flavor. Deliberately a per-hit flat reduction, not
 * a depleting absorption pool: {@link Trait} instances are shared across every mob built from
 * the same {@link EnemyDefinition} (see {@link TraitContext}), so a pool that's "used up" over
 * one mob's lifetime doesn't fit this model without per-mob mutable trait state, which nothing
 * else in v1 needs either.
 */
public record FlatResistTrait(int flatReduction) implements Trait {

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        return new Damage(incoming.amount() - this.flatReduction, incoming.type());
    }
}
