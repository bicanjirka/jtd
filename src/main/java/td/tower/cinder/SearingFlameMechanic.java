package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Searing Flame: an ignition makes the enemy Vulnerable, and so does every wave that hits a burning enemy, once a second at most. */
public final class SearingFlameMechanic implements CinderMechanic {

    private static final int INTERVAL_TICKS = 20;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSearing(INTERVAL_TICKS).withLook(FlameLook.SEARING);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.VULNERABLE);
    }

    @Override
    public TermText describe() {
        return TermText.of("an ignition applies a stack of ").plus(EffectKind.VULNERABLE)
                .plus(", and so does every wave that hits an enemy with ").plus(EffectKind.BURN)
                .plus(", at most one stack every " + TermNumbers.seconds(INTERVAL_TICKS / (float) TickRate.TICKS_PER_SECOND));
    }
}
