package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerStat;
import td.tower.TowerTerm;

/**
 * Lightning Rod: an arc with nowhere to go returns to the primary at full strength, and arcs deal more
 * crit damage.
 */
public final class LightningRodMechanic implements SplashMechanic {

    private static final int RETURNS = 3;
    private static final float CRIT_DAMAGE = 0.5f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withReturns(RETURNS).withCritDamage(CRIT_DAMAGE));
    }

    @Override
    public TermText describe() {
        return TermText.of("an ").plus(TowerTerm.ARC)
                .plus(" with nowhere to go returns to the primary at full strength, up to " + RETURNS + " times; an ")
                .plus(TowerTerm.ARC).plus(" gets ").plus(TowerStat.CRIT_MULTIPLIER).plus(" " + TermNumbers.signedPercent(CRIT_DAMAGE));
    }
}
