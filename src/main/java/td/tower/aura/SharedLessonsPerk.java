package td.tower.aura;

/** Shared Lessons: buffed towers each earn the XP of every enemy any of them reached. */
public final class SharedLessonsPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.sharingLessons();
    }

    @Override
    public String describe() {
        return "buffed towers share what they see: each earns the XP of every enemy any of them reached";
    }
}
