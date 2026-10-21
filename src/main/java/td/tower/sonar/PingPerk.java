package td.tower.sonar;

import td.effect.EffectKind;
import td.term.Term;

import java.util.Set;

/** Ping: every revolution Exposes the healthiest enemy the beam passed. */
public final class PingPerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withCount(1));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.EXPOSED);
    }

    @Override
    public String describe() {
        return "Ping: each revolution Exposes the healthiest enemy the beam passes, until the next pass";
    }
}
