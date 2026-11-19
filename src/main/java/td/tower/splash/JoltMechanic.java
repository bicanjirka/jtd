package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Jolt: each jump of the Arc leaves its enemy Vulnerable. */
public final class JoltMechanic implements SplashMechanic {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().jolting());
    }

    @Override
    public TermText describe() {
        return TermText.of("each jump of the ").plus(TowerTerm.ARC).plus(" leaves its enemy ").plus(EffectKind.VULNERABLE)
                .plus(" for " + TermNumbers.seconds(ArcSpec.JOLT_SECONDS));
    }
}
