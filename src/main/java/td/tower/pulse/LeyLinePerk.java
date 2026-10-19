package td.tower.pulse;

import td.tower.upgrade.PerkText;

/** Ley Line: the field casts a beam to another field, and an enemy on it gets both fields' rules. */
public final class LeyLinePerk implements PulsePerk {

    private static final float REACH_CELLS = 5f;
    private static final float BAND_CELLS = 0.3f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withLine(new LineSpec(REACH_CELLS, BAND_CELLS));
    }

    @Override
    public String describe() {
        return "casts a beam to another Obelisk within " + PerkText.cells(REACH_CELLS) + " cells, the one whose beam "
                + "crosses the path most; an enemy on it gets both fields' rules at the Toll it carries, but no damage "
                + "and no Toll";
    }
}
