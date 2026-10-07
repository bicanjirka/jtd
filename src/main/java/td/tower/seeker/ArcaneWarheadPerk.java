package td.tower.seeker;

import td.effect.EffectKind;

/** Arcane Warhead: every impact Unravels its target, twice over if it was already frozen or chilled. */
public final class ArcaneWarheadPerk implements SeekerPerk {

    @Override
    public void react(Impact impact, SeekerActions actions) {
        actions.applyStacks(impact.target(), EffectKind.UNRAVELED, impact.wasControlled() ? 2 : 1);
    }
}
