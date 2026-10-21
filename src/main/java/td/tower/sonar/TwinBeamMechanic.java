package td.tower.sonar;

import td.term.TermNumbers;
import td.term.TermText;

/** Twin Beam: a second beam opposite the first, at part of the damage. */
public final class TwinBeamMechanic implements SonarMechanic {

    private static final float SHARE = 0.7f;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withBeam(spec.beam().withTwinShare(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of("a second beam, opposite, at " + TermNumbers.percent(SHARE) + " damage");
    }
}
