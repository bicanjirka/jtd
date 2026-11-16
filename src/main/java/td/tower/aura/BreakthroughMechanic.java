package td.tower.aura;

import td.term.TermText;
import td.tower.TowerTerm;

/** Breakthrough: towers pushing their tiers teach the others. */
public final class BreakthroughMechanic implements AuraMechanic {

    /** What every other Lit tower gains when one opens a tier. */
    public static final int XP = 10;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withLesson(LightSpec.Lesson.BREAKTHROUGH);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.BREAKTHROUGH).plus(": when a ").plus(TowerTerm.LIT)
                .plus(" tower opens a tier, every other one gains " + XP + " XP");
    }
}
