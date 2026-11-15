package td.effect;

/**
 * What a freeze is made of: the colour it wears, and whether it holds an enemy immune to freezing. Every ice is the
 * same {@link EffectKind#FREEZE}, so it fills and follows the one freeze meter.
 */
public enum IceKind {
    FROST(false),
    RUBY(true),
    SHADE(false);

    private final boolean holdsTheImmune;

    IceKind(boolean holdsTheImmune) {
        this.holdsTheImmune = holdsTheImmune;
    }

    /** Whether this ice freezes an enemy that an immunity to freezing would keep a freeze off. */
    public boolean holdsTheImmune() {
        return this.holdsTheImmune;
    }
}
