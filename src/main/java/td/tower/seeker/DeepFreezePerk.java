package td.tower.seeker;

/** Deep Freeze: every freeze lasts half as long again. */
public final class DeepFreezePerk implements SeekerPerk {

    private static final float LONGER = 1.5f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().lastingLonger(LONGER));
    }
}
