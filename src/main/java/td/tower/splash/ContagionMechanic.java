package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Contagion: every hex jumps on from an enemy that dies with it, and a Doom starts over on the healthiest. */
public final class ContagionMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().contagious());
    }

    @Override
    public TermText describe() {
        return TermText.of("when a hexed enemy dies, its ").plus(TowerTerm.HEX).plus(" jumps, as it is, to the nearest enemy within "
                + TermNumbers.cells(HexSpec.SPREAD_CELLS) + " without one; a ").plus(EffectKind.DOOM)
                .plus(" starts over on the healthiest enemy in reach");
    }
}
