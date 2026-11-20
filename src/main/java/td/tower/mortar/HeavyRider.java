package td.tower.mortar;

import td.effect.EffectKind;

import java.util.Optional;

/** What a Heavy shell adds to everything its blast reaches: the Heavy-shell pick. */
public enum HeavyRider {
    NONE,
    /** Daze, longer in its full-damage zone than at its edge. */
    CONCUSSION,
    /** Bleed, a share of the shell's damage for every cell walked. */
    SHRAPNEL;

    /** The effect it puts on what a Heavy's blast reaches. */
    public Optional<EffectKind> applies() {
        return switch (this) {
            case NONE -> Optional.empty();
            case CONCUSSION -> Optional.of(EffectKind.DAZED);
            case SHRAPNEL -> Optional.of(EffectKind.BLEEDING);
        };
    }
}
