package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

/** Agony: a hexed enemy is Vulnerable while its hex lasts. */
public final class AgonyMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().agonizing());
    }

    @Override
    public TermText describe() {
        return TermText.of("a hexed enemy has ").plus(EffectKind.VULNERABLE).plus(" while its ").plus(TowerTerm.HEX).plus(" lasts");
    }
}
