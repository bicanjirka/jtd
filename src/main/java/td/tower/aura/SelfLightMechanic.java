package td.tower.aura;

import td.term.TermText;
import td.tower.TowerTerm;

/** Self Light: Tier II's own node. The Beacon is {@link TowerTerm#LIT} by its own light, with everything that brings. */
public final class SelfLightMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withLitSelf();
    }

    @Override
    public TermText describe() {
        return TermText.of("The Beacon is ").plus(TowerTerm.LIT).plus(" by its own light: the jam share, the Lessons "
                + "and every other gift it makes to a Lit tower");
    }
}
