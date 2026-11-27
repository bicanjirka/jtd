package td.tower.aura;

import td.term.TermText;
import td.tower.TowerTerm;

/** Steady Light: Tier I's own node. The light lets half of what it let through reach a Lit tower. */
public final class SteadyLightMechanic implements AuraMechanic {

    private static final float FACTOR = 0.5f;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withJamTakenTimes(FACTOR);
    }

    @Override
    public TermText describe() {
        float lit = LanternMechanic.JAM_TAKEN;
        return TermText.of("Steady Light: a ").plus(TowerTerm.LIT).plus(" tower takes " + lit * FACTOR + " of the jam and the "
                + "blinding, not " + lit + "; the light alone lets " + FACTOR + " through");
    }
}
