package td.enemy;

import td.effect.EffectKind;

/**
 * Rejects a burn outright, so it never applies - unlike a resistance, which reduces a hit that
 * still lands.
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
