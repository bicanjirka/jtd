package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** Everburn: a pool never decays below two fifths of its strongest application while its enemy is in range. */
public final class EverburnMechanic implements CinderMechanic {

    private static final float FLOOR = 0.4f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withEverburnFloor(FLOOR);
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(EffectKind.BURN)
                .plus(" never fades below " + TermNumbers.percent(FLOOR) + " while its enemy is in this tower's range");
    }
}
