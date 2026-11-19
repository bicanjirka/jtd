package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Contagion: a hex of an enemy killed with it splits in two, each half jumping on. */
public final class ContagionMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().contagious());
    }

    @Override
    public TermText describe() {
        return TermText.of("when a hexed enemy is killed, its ").plus(TowerTerm.HEX)
                .plus(" splits in two, each half jumping, with the time it had left, to the nearest enemy within "
                + TermNumbers.cells(HexSpec.SPREAD_CELLS) + " without one; ").plus(EffectKind.DOOM)
                .plus(" shares what it recorded between them");
    }
}
