package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Arcane Warhead: every impact Unravels its target, twice over if it was already frozen or chilled. */
public final class ArcaneWarheadMechanic implements SeekerMechanic {

    private static final int CONTROLLED_STACKS = 2;

    @Override
    public void react(Impact impact, SeekerActions actions) {
        actions.applyStacks(impact.target(), EffectKind.UNRAVELED, impact.wasControlled() ? CONTROLLED_STACKS : 1);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.UNRAVELED);
    }

    @Override
    public TermText describe() {
        return TermText.of("every impact applies a stack of ").plus(EffectKind.UNRAVELED)
                .plus(", " + CONTROLLED_STACKS + " stacks to an enemy with ").plus(EffectKind.FREEZE).plus(" or ")
                .plus(EffectKind.CHILL);
    }
}
