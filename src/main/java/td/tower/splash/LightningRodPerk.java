package td.tower.splash;

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
}
