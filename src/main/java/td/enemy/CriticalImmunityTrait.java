package td.enemy;

import td.damage.Damage;

/**
 * Strips a critical hit's bonus back off, treating it as an ordinary hit - armor thick enough
 * that a precisely placed shot lands no harder than any other. Reuses {@link Damage#stripCritical()}
 * rather than reducing the amount by some fraction of its own, so immunity is exact regardless
 * of which tower's roll produced the bonus.
 */
public record CriticalImmunityTrait() implements Trait {

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        return incoming.stripCritical();
    }
}
