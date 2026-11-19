package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Blight Cloud: Blight also curses the enemies around its target. */
public final class BlightCloudMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().clouded());
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.BLIGHT).plus(" forks as it nears its target, flying on to the " + HexSpec.CLOUD_TARGETS
                + " enemies within " + TermNumbers.cells(HexSpec.CLOUD_CELLS) + " of it that would leave soonest");
    }
}
