package td.tower.sonar;

/** Ping: every revolution Exposes the healthiest enemy the beam passed. */
public final class PingPerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withCount(1));
    }

    @Override
    public String describe() {
        return "each revolution Exposes the healthiest enemy the beam passes, until the next pass";
    }
}
