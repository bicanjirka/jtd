package td.tower.cinder;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Combustion: an enemy whose fire grows hotter than a single wave could make it bursts for half of what is left within a cell. */
public final class CombustionPerk implements CinderPerk {

    private static final float SHARE = 0.5f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCombustion(CombustionSpec.of(SHARE, RADIUS_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("a burn that grows hotter than a single wave could make it bursts for " + PerkText.percent(SHARE)
                + " of what it has left onto enemies within " + PerkText.distance(RADIUS_CELLS) + ", and can burst again once it has cooled to half");
    }
}
