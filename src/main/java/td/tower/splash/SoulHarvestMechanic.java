package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;

/** Soul Harvest: the stones earn Souls faster. */
public final class SoulHarvestMechanic implements SplashMechanic {

    private static final float DAMAGE_PER_SOUL = SplashSpec.DAMAGE_PER_UNIT / 2f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withDamagePerUnitAtMost(DAMAGE_PER_SOUL);
    }

    @Override
    public TermText describe() {
        int times = Math.round(SplashSpec.DAMAGE_PER_UNIT / DAMAGE_PER_SOUL);
        return TermText.of("its stones earn ").plus(TowerTerm.SOUL).plus("s "
                + (times == 2 ? "twice" : times + " times") + " as fast");
    }
}
