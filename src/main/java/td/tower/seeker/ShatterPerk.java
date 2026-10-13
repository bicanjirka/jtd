package td.tower.seeker;

import td.tower.upgrade.PerkText;

/** Deep Freeze II: a frozen enemy the Seeker kills shatters, hurting what stands near it. */
public final class ShatterPerk implements SeekerPerk {

    private static final float SHARE = 0.5f;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withShatter(spec.shatter().alsoOnKill(SHARE));
    }

    @Override
    public String describe() {
        return "a frozen enemy this tower kills shatters for " + PerkText.percent(SHARE) + " of its damage around it";
    }
}
