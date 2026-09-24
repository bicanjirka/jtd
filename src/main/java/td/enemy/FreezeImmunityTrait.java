package td.enemy;

import td.effect.EffectKind;

/** Rejects a freeze outright, so it never applies. */
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
