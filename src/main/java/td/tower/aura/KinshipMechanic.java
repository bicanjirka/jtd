package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Kinship: a mixed cluster learns faster. */
public final class KinshipMechanic implements AuraMechanic {

    /** The XP share each other tower type in the light adds. */
    public static final float XP_PER_TYPE = 0.05f;
    /** The most other types it counts. */
    public static final int TYPE_CAP = 5;

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withLesson(LightSpec.Lesson.KINSHIP);
    }

    /** The XP share it adds for {@code otherTypes} other tower types in the light. */
    public static float xpFor(int otherTypes) {
        return XP_PER_TYPE * Math.min(TYPE_CAP, otherTypes);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.KINSHIP).plus(": ").plus(TowerTerm.LIT).plus(" towers earn " + TermNumbers.signedPercent(XP_PER_TYPE)
                + " XP for each other tower type in the light, up to " + TYPE_CAP);
    }
}
