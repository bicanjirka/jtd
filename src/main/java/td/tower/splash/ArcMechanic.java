package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Arc: the blast carries past its edge as lightning. */
public final class ArcMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(ArcSpec.arcs());
    }

    @Override
    public TermText describe() {
        return TermText.of("the blast carries past its edge as an ").plus(TowerTerm.ARC)
                .plus(": " + ArcSpec.BASE_JUMPS + " jumps at " + TermNumbers.percent(ArcSpec.BASE_SHARE)
                        + " as magic, reaching further from an enemy with ")
                .plus(EffectKind.SATURATED);
    }
}
