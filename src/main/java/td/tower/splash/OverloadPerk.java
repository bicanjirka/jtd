package td.tower.splash;

/** Overload: an arc crit Dazes its target, and arcs into a fully Saturated enemy crit more often. */
public final class OverloadPerk implements SplashPerk {

    private static final float DAZE_SECONDS = 0.5f;
    private static final float SATURATED_CRIT_BONUS = 0.1f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withDaze(DAZE_SECONDS).withSaturatedCritBonus(SATURATED_CRIT_BONUS));
    }
}
