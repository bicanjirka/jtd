package td.enemy;

import td.effect.EffectKind;

/**
 * Rejects an incoming {@link EffectKind#FREEZE} outright, so it is never applied at all - see
 * {@link BurnImmunityTrait}'s own doc comment for why this is a rejection rather than a
 * resistance, and {@link DefinedEnemyMob#applyEffect} for where it is consulted.
 */
public record FreezeImmunityTrait() implements Trait {

    @Override
    public boolean blocksEffect(EffectKind kind) {
        return kind == EffectKind.FREEZE;
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.FREEZE_IMMUNE;
    }
}
