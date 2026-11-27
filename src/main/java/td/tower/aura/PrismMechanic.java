package td.tower.aura;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

/** Prism: the light carries the mark of every kind of tower it lights. */
public final class PrismMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withPrism();
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.PRISM).plus(": an enemy in the light carries a stack of each ").plus(TowerTerm.LIT)
                .plus(" tower type's mark: Sniper ").plus(EffectKind.SUNDERED).plus(", Mortar ").plus(EffectKind.CRACKED)
                .plus(", Aerie ").plus(EffectKind.UNRAVELED).plus(", Scorcher ").plus(EffectKind.SINGED)
                .plus(", Burst ").plus(EffectKind.VULNERABLE).plus(", Obelisk ").plus(EffectKind.TOLL)
                .plus("; a type's mark once, however many of it the light holds");
    }
}
