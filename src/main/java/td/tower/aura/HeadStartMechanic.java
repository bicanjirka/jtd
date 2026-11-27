package td.tower.aura;

import td.term.TermText;
import td.tower.TowerTerm;

/** Head Start: Tier III's own node. A Lit tower with less XP than this is lifted to it. */
public final class HeadStartMechanic implements AuraMechanic {

    /** The XP the light lifts a Lit tower to. */
    public static final int XP = 100;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withXpFloor(XP);
    }

    @Override
    public TermText describe() {
        return TermText.of("Head Start: a ").plus(TowerTerm.LIT).plus(" tower with under " + XP + " XP is lifted to " + XP
                + " XP, once it is built or the light reaches it");
    }
}
