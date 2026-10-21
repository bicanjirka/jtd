package td.tower.cinder;

import td.effect.EffectKind;
import td.effect.PoolTuning;
import td.term.Term;
import td.term.TermText;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Thermal Shock: freezing a burning enemy detonates its pool at 150% and chills its neighbours. */
public final class ThermalShockPerk implements CinderPerk {

    private static final float BURST_SHARE = 1.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withThermalShock().withTuning(spec.tuning().withFreezeBurstShare(BURST_SHARE));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CHILL);
    }

    @Override
    public TermText describe() {
        return TermText.of("when a burning enemy is frozen by anything, its remaining pool detonates at " + PerkText.percent(BURST_SHARE)
                + " instead of " + PerkText.percent(PoolTuning.standard().freezeBurstShare()) + " and chills its neighbours");
    }
}
