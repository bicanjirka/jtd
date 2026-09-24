package td.enemy;

import td.damage.Damage;

/** Strips a critical hit's bonus, so it lands as an ordinary hit. */
public record CriticalImmunityTrait() implements Trait {

    @Override
    public Damage onHit(Damage incoming, TraitContext context) {
        return incoming.stripCritical();
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.CRITICAL_IMMUNE;
    }
}
