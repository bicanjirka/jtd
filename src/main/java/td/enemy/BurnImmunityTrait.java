package td.enemy;

import td.effect.EffectKind;

/**
 * Rejects an incoming {@link EffectKind#BURN} outright, so it is never applied at all -
 * distinct from a resistance, which reduces a hit that still lands. See {@link
 * DefinedEnemyMob#applyEffect}, the one place a mob's traits are consulted before a status
 * effect is applied.
 */
public record BurnImmunityTrait() implements Trait {

    @Override
    public boolean blocksEffect(EffectKind kind) {
        return kind == EffectKind.BURN;
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.BURN_IMMUNE;
    }
}
