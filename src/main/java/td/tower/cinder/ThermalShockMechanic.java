package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Thermal Shock: more Flame into an enemy another tower holds. */
public final class ThermalShockMechanic implements CinderMechanic {

    private static final float MORE = 0.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withThermalShock(1f + MORE);
    }

    @Override
    public TermText describe() {
        return TermText.of("it pours " + TermNumbers.percent(MORE) + " more ").plus(TowerTerm.FLAME)
                .plus(" into an enemy with ").plus(EffectKind.CHILL).plus(" or ").plus(EffectKind.FREEZE);
    }
}
