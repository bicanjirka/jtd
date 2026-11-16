package td.tower.aura;

import td.term.TermText;
import td.tower.TowerTerm;

/** Daylight: an aura stops at the light's edge. */
public final class DaylightMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withEdge(LightSpec.Edge.DAYLIGHT);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.DAYLIGHT).plus(": an enemy in the light gives its allies no aura");
    }
}
