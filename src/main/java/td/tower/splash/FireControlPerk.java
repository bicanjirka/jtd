package td.tower.splash;

/**
 * Fire Control: the blast lands on the enemy with most neighbours inside it, and every blast that
 * catches an enemy Saturates it.
 */
public final class FireControlPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withBlast(spec.blast().aimingAtCrowds().saturating());
    }

    @Override
    public String describe() {
        return "Fire Control: aims at the enemy with most neighbours in the blast, and every blast that catches an enemy "
                + "Saturates it";
    }
}
