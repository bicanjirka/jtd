package td.tower.seeker;

/** Rearm: a missile that freezes its target launches a new one at once; a rearmed missile rearms nothing. */
public final class RearmPerk implements SeekerPerk {

    @Override
    public void react(Impact impact, SeekerActions actions) {
        if (impact.froze() && !impact.rearmed()) {
            actions.rearm();
        }
    }
}
