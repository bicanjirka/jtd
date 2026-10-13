package td.tower.sonar;

import td.tower.upgrade.PerkText;

/** A ping picks the healthiest enemy past half the range, and it stays Exposed for two passes. */
public final class FarPingPerk implements SonarPerk {

    private static final float NEAREST = 0.5f;
    private static final int PASSES = 2;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withMinRangeShare(NEAREST).withPasses(PASSES));
    }

    @Override
    public String describe() {
        return "Ping picks the healthiest enemy past " + PerkText.percent(NEAREST) + " of its range, Exposed for " + PASSES
                + " passes";
    }
}
