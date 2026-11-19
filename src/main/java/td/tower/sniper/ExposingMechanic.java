package td.tower.sniper;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/**
 * Exposing Crits: a crit Exposes its target. Each Sniper holds one share of the Expose and a crit only refreshes it, so
 * only a Sniper that crits often keeps it up.
 */
public final class ExposingMechanic implements SniperMechanic {

    private static final int EXPOSE_TICKS = Math.round(2 * TickRate.TICKS_PER_SECOND);

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical() && !result.killed()) {
            actions.expose(result.target(), EXPOSE_TICKS);
        }
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.EXPOSED);
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit ").plus(EffectKind.EXPOSED).plus(" its target for "
                + TermNumbers.seconds(EXPOSE_TICKS / TickRate.TICKS_PER_SECOND) + "; another crit only refreshes it");
    }
}
