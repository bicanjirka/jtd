package td.zone;

import td.effect.EffectKind;

import java.util.Optional;

/** What a patch of ground does to the enemies, or the towers, within it. */
public enum ZoneKind {
    /** Feeds the burn of whatever stands on it. */
    BURNING_GROUND,
    /** Feeds the poison of what stands on it and holds it tarred. */
    TAR,
    /** Hits what stands on it with magic and holds it chilled. */
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

    /** Whether each pulse deals its strength at once as magic damage, rather than into a pool. */
    public boolean hitsDirectly() {
        return this == FROST_GROUND;
    }

    /** The slow each pulse holds on, at the strength its pulse gives; empty for ground that slows nothing. */
    public Optional<EffectKind> slows() {
        return switch (this) {
            case TAR -> Optional.of(EffectKind.TARRED);
            case FROST_GROUND -> Optional.of(EffectKind.CHILL);
            case BURNING_GROUND, FALLOUT, FLARE, HEALING_GROUND, HASTE_GROUND, WARD_MINE, BLINDING_FIELD -> Optional.empty();
        };
    }

    /** The pool each pulse adds its strength to: a burn or a poison; empty for ground that feeds none. */
    public Optional<EffectKind> fuels() {
        return switch (this) {
            case BURNING_GROUND -> Optional.of(EffectKind.BURN);
            case TAR -> Optional.of(EffectKind.POISON);
            case FROST_GROUND, FALLOUT, FLARE, HEALING_GROUND, HASTE_GROUND, WARD_MINE, BLINDING_FIELD -> Optional.empty();
        };
    }
}
