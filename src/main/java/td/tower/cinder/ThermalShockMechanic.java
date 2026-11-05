package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Set;

/** Thermal Shock: a burning enemy that dies lands what is left of its Burn on its neighbours, and chills them. */
public final class ThermalShockMechanic implements CinderMechanic {

    public static final float BURST_SHARE = 1.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withThermalShock(BURST_SHARE);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CHILL);
    }

    @Override
    public TermText describe() {
        return TermText.of("when an enemy it set alight dies with ").plus(EffectKind.BURN)
                .plus(", what is left of it lands on the enemies near it at " + TermNumbers.percent(BURST_SHARE)
                        + ", and they gain ")
                .plus(EffectKind.CHILL);
    }
}
