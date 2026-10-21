package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

/** Dragon's Breath: a continuous stream that keeps every enemy's Stoke full. */
public final class DragonsBreathMechanic implements CinderMechanic {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(spec.stoke().full());
    }

    @Override
    public TermText describe() {
        return TermText.of("a steady stream that keeps every ").plus(EffectKind.BURN).plus(" topped up and ")
                .plus(TowerTerm.STOKE).plus(" always full");
    }
}
