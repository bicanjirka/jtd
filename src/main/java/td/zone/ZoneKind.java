package td.zone;

import td.effect.EffectKind;

import java.util.Optional;

/** What a patch of ground does to the enemies, or the towers, within it. */
public enum ZoneKind {
    /** Sets whatever stands on it alight. */
    BURNING_GROUND,
    /** Slows and poisons, and leaves an enemy tarred. */
    TAR,
    /** Chills what stands on it, and freezes what stays. */
    FROST_GROUND,
    /** Drains the spirit of what stands on it and keeps heals and shields from taking hold. */
    FALLOUT,
    /** A flare: reveals what walks into it. */
    FLARE,
    /** Heals what stands on it. */
    HEALING_GROUND,
    /** Speeds up what stands on it and keeps slows off it. */
    HASTE_GROUND,
    /** Goes off as a shield on the first enemy to step on it, once it is armed. */
    WARD_MINE,
    /** Makes the towers inside it miss; it works on towers, not on enemies. */
    BLINDING_FIELD;

    /** The pool each pulse adds its strength to: a burn or a poison; empty for ground that deals no damage. */
    public Optional<EffectKind> fuels() {
        return switch (this) {
            case BURNING_GROUND -> Optional.of(EffectKind.BURN);
            case TAR, FALLOUT -> Optional.of(EffectKind.POISON);
            case FROST_GROUND, FLARE, HEALING_GROUND, HASTE_GROUND, WARD_MINE, BLINDING_FIELD -> Optional.empty();
        };
    }
}
