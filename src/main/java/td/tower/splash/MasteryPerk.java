package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Mastery: strengthens the chain's levels II and III. On Arc, arcs crit more and Daze longer; on
 * Hex, Blight poisons half as hard again and a hexed enemy that dies leaves a cursed cloud.
 */
public final class MasteryPerk implements SplashPerk {

    private static final float ARC_CRIT_BONUS = 0.1f;
    private static final float BLIGHT_SHARE = 0.06f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        if (spec.arcs().active()) {
            return spec.withArcs(spec.arcs().withCritBonus(ARC_CRIT_BONUS).withLongDaze());
        }
        if (spec.hexes().isActive()) {
            return spec.withHexes(spec.hexes().withBlightShareAtLeast(BLIGHT_SHARE).withCursedCloud());
        }
        return spec;
    }

    @Override
    public TermText describe() {
        return TermText.of("Stormcaller: each ").plus(TowerTerm.ARC)
                .plus(" gets " + TermNumbers.signedPercent(ARC_CRIT_BONUS) + " crit chance, and Overload's ")
                .plus(EffectKind.DAZED).plus(" lasts " + TermNumbers.seconds(ArcSpec.LONG_DAZE_SECONDS) + "; Hexer: ")
                .plus(EffectKind.BLIGHT)
                .plus(" deals " + TermNumbers.percent(BLIGHT_SHARE / HexSpec.BLIGHT_SHARE - 1f)
                        + " more, and an enemy that dies carrying this tower's ")
                .plus(TowerTerm.HEX).plus(" leaves a cursed cloud");
    }
}
