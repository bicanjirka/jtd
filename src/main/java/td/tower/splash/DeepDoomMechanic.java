package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Deep Doom: Doom returns more of the damage. */
public final class DeepDoomMechanic implements SplashMechanic {

    private static final float SHARE = 0.8f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withHexes(spec.hexes().withDoomShareAtLeast(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.DOOM).plus(" returns " + TermNumbers.percent(SHARE) + " of the damage, up from "
                + TermNumbers.percent(HexSpec.DOOM_SHARE));
    }
}
