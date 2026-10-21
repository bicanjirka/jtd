package td.tower.cinder;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Everburn: a pool never decays below two fifths of its strongest application while its enemy is in range. */
public final class EverburnPerk implements CinderPerk {

    private static final float FLOOR = 0.4f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withEverburnFloor(FLOOR);
    }

    @Override
    public TermText describe() {
        return TermText.of("a pool never decays below " + PerkText.percent(FLOOR) + " while its enemy is in this tower's range");
    }
}
