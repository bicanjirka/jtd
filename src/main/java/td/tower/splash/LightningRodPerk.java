package td.tower.splash;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/**
 * Lightning Rod: an arc with nowhere to go returns to the primary at full strength, and arcs on a
 * frozen or Dazed enemy deal more crit damage.
 */
public final class LightningRodPerk implements SplashPerk {

    private static final int RETURNS = 3;
    private static final float STOPPED_CRIT_DAMAGE = 0.5f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withReturns(RETURNS).withStoppedCritDamage(STOPPED_CRIT_DAMAGE));
    }

    @Override
    public TermText describe() {
        return TermText.of("an arc with nowhere to go returns to the primary at full strength, up to " + RETURNS + " times; arcs on a Dazed "
                + "enemy deal +" + PerkText.percent(STOPPED_CRIT_DAMAGE) + " crit damage");
    }
}
