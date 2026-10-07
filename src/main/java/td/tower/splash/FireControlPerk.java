package td.tower.splash;

/**
 * Fire Control: the blast lands on the enemy with most neighbours inside it, and every blast that
 * catches an enemy Saturates it.
 */
public final class FireControlPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withBlast(spec.blast().aimingAtCrowds().withSaturation(SaturationRule.attuned()));
    }
}
