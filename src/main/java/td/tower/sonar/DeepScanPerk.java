package td.tower.sonar;

import td.util.TickRate;

/** Deep Scan: every revolution reveals the invisible enemies in the outer quarter of the range. */
public final class DeepScanPerk implements SonarPerk {

    private static final float OUTER_QUARTER_FROM = 0.75f;
    private static final float REVEAL_SECONDS = 1f;

    @Override
    public void onRevolution(Revolution revolution, SonarActions actions) {
        actions.revealHiddenBeyond(OUTER_QUARTER_FROM, Math.round(REVEAL_SECONDS * TickRate.TICKS_PER_SECOND));
    }
}
