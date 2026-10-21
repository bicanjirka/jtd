package td.tower.sonar;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** A ping picks the healthiest enemy past half the range, and it stays Exposed for two passes. */
public final class FarPingMechanic implements SonarMechanic {

    private static final float NEAREST = 0.5f;
    private static final int PASSES = 2;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withMinRangeShare(NEAREST).withPasses(PASSES));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.PING)
                .plus(" picks the enemy with the most health past " + TermNumbers.percent(NEAREST) + " of its range, and its ")
                .plus(EffectKind.EXPOSED).plus(" lasts " + PASSES + " passes");
    }
}
