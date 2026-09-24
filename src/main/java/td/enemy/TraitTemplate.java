package td.enemy;

import td.damage.DamageMix;

import java.util.Optional;

/**
 * What an {@link EnemyDefinition}'s trait slot holds: resolved into the mob's actual {@link Trait}
 * once, when the mob is built. A plain {@link Trait} resolves to itself; an adaptive one reads the
 * level's damage mix at that moment.
 */
public interface TraitTemplate {

    /** The trait one mob spawned now carries; empty when it would have no effect. */
    Optional<Trait> resolvedFor(DamageMix mix);
}
