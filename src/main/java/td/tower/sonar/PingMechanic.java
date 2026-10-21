package td.tower.sonar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Ping: every revolution Exposes the healthiest enemy the beam passed. */
public final class PingMechanic implements SonarMechanic {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withPing(spec.ping().withCount(1));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.EXPOSED);
    }

    @Override
    public TermText describe() {
        return TermText.of("each revolution, ").plus(TowerTerm.PING).plus(" applies ").plus(EffectKind.EXPOSED)
                .plus(" to the enemy with the most health the beam passes, until the next pass");
    }
}
