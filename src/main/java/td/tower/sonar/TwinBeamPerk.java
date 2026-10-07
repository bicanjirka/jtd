package td.tower.sonar;

/** Twin Beam: a second beam opposite the first, at part of the damage. */
public final class TwinBeamPerk implements SonarPerk {

    private static final float SHARE = 0.7f;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withBeam(spec.beam().withTwinShare(SHARE));
    }
}
