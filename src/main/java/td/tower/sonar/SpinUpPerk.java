package td.tower.sonar;

/** Spin-Up: a revolution takes two seconds instead of three. */
public final class SpinUpPerk implements SonarPerk {

    private static final float SECONDS = 2f;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withBeam(spec.beam().withSecondsPerRevolution(SECONDS));
    }
}
