package td.tower.cinder;

/** White Flame: the flame burns white, and each Stoke step raises a burn 15% instead of 10%. */
public final class WhiteFlamePerk implements CinderPerk {

    private static final float STEP = 0.15f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLook(FlameLook.WHITE).withStoke(spec.stoke().withStep(STEP));
    }
}
