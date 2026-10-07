package td.tower.seeker;

/** Twin Warhead II: a launch sends two missiles, the second at the next target. */
public final class SecondMissilePerk implements SeekerPerk {

    private static final int MISSILES = 2;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withMissilesPerShot(Math.max(spec.missilesPerShot(), MISSILES));
    }
}
