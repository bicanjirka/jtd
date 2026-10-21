package td.tower.seeker;

import td.term.TermText;

/** Rearm: a missile that freezes its target launches a new one at once; a rearmed missile rearms nothing. */
public final class RearmPerk implements SeekerPerk {

    @Override
    public void react(Impact impact, SeekerActions actions) {
        if (impact.froze() && !impact.rearmed()) {
            actions.rearm();
        }
    }

    @Override
    public TermText describe() {
        return TermText.of("a missile that freezes its target launches a new missile from the tower at once; a rearmed missile's freeze "
                + "rearms nothing");
    }
}
