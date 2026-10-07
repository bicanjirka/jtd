package td.tower.sonar;

/** A ping Exposes the two healthiest enemies. */
public final class TwoPingsPerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withCount(2));
    }
}
