package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;

/** Railgun: the shot pierces every enemy on the line through its target. */
public final class RailgunPerk implements SniperPerk {

    private static final PierceRule PIERCE = new PierceRule(2f, 0.75f, 0.35f);

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withPiercing(PIERCE);
    }

    @Override
    public TermText describe() {
        return TermText.of("the shot pierces every enemy on the line through its target, "
                + TermNumbers.cells(PIERCE.extraReachCells()) + " past max range; each one after the first takes "
                + TermNumbers.percent(1f - PIERCE.falloff()) + " less");
    }
}
