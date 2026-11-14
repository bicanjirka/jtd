package td.tower.splash;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Hexer: its stones brand what they damage with Soulbrand, which bursts when the enemy dies and sends its soul home. */
public final class SoulbrandMechanic implements SplashMechanic {

    /** What one stack bursts for, in points of magic damage. */
    public static final float BURST_POINTS = 10f;
    /** How far the burst reaches, in cells. */
    public static final float BURST_CELLS = 1f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withPassive(Passive.SOULBRAND);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(TowerTerm.SOULBRAND);
    }

    @Override
    public TermText describe() {
        return TermText.of("passive ").plus(TowerTerm.SOULBRAND).plus(": every enemy its stones damage carries a stack; "
                + "when it dies it bursts for " + TermNumbers.whole(BURST_POINTS) + " magic damage per stack within "
                + TermNumbers.cells(BURST_CELLS) + ", and its soul gives ").plus(PassivePayout.describe(TowerTerm.SOUL));
    }
}
