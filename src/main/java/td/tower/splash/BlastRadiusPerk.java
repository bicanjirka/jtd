package td.tower.splash;

/** A wider blast; every distance the payload uses grows with it. */
public final class BlastRadiusPerk implements SplashPerk {

    private final float bonus;

    public BlastRadiusPerk(float bonus) {
        this.bonus = bonus;
    }

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withBlast(spec.blast().withRadiusBonus(this.bonus));
    }
}
