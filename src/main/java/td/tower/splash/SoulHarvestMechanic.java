package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Soul Harvest: the stones earn Souls faster. */
public final class SoulHarvestMechanic implements SplashMechanic {

    private static final float DAMAGE_PER_SOUL = 100f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withDamagePerUnitAtMost(DAMAGE_PER_SOUL);
    }

    @Override
    public TermText describe() {
        return TermText.of("its stones earn 1 ").plus(TowerTerm.SOUL).plus(" per " + TermNumbers.whole(DAMAGE_PER_SOUL)
                + " damage, up from " + TermNumbers.whole(SplashSpec.DAMAGE_PER_UNIT));
    }
}
