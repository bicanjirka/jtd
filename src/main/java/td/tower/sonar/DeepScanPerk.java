package td.tower.sonar;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

import java.util.Set;

/** Deep Scan: every revolution reveals the invisible enemies in the outer quarter of the range. */
public final class DeepScanPerk implements SonarPerk {

    private static final float OUTER_QUARTER_FROM = 0.75f;
    private static final float REVEAL_SECONDS = 1f;

    @Override
    public void onRevolution(Revolution revolution, SonarActions actions) {
        actions.revealHiddenBeyond(OUTER_QUARTER_FROM, Math.round(REVEAL_SECONDS * TickRate.TICKS_PER_SECOND));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.REVEALED);
    }

    @Override
    public String describe() {
        return "Deep Scan: each revolution reveals invisible enemies in the outer " + PerkText.percent(1f - OUTER_QUARTER_FROM)
                + " of its range for " + PerkText.seconds(Math.round(REVEAL_SECONDS * TickRate.TICKS_PER_SECOND));
    }
}
