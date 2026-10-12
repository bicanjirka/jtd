package td.tower.aura;

/** Shared Lessons: buffed towers each earn the XP of every enemy any of them reached. */
public final class SharedLessonsPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.sharingLessons();
    }
}
