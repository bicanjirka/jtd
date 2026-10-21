package td.tower.aura;

import td.term.TermText;

/** Shared Lessons: buffed towers each earn the XP of every enemy any of them reached. */
public final class SharedLessonsMechanic implements AuraMechanic {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.sharingLessons();
    }

    @Override
    public TermText describe() {
        return TermText.of("buffed towers share what they see: each earns the XP of every enemy any of them reached");
    }
}
