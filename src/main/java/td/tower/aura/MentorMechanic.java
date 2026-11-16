package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Mentor: a tower built late catches up. */
public final class MentorMechanic implements AuraMechanic {

    /** The XP share the least experienced Lit tower adds to what it earns. */
    public static final float XP = 1f;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withLesson(LightSpec.Lesson.MENTOR);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.MENTOR).plus(": the ").plus(TowerTerm.LIT).plus(" tower with the least XP earns "
                + TermNumbers.times(1f + XP) + " XP");
    }
}
