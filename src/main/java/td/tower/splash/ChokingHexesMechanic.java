package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermText;
import td.tower.TowerTerm;

/** Choking Hexes: every hex also Silences. */
public final class ChokingHexesMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().chokingHexes());
    }

    @Override
    public TermText describe() {
        return TermText.of("a hexed enemy has ").plus(EffectKind.SILENCED).plus(" while its ").plus(TowerTerm.HEX).plus(" lasts");
    }
}
