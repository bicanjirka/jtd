package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermText;

/** Choking Blight: Blight also Silences. */
public final class ChokingBlightMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().chokingBlight());
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.BLIGHT).plus(" also applies ").plus(EffectKind.SILENCED).plus(" while it lasts");
    }
}
