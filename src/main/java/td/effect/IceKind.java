package td.effect;

/**
 * What a freeze is made of: the colour it wears, whether it holds an enemy immune to freezing, and how strong it is
 * against another ice. Every ice is the same {@link EffectKind#FREEZE}, so it fills and follows the one freeze meter.
 */
public enum IceKind {
    FROST(false, 0),
    RUBY(true, 2),
    SHADE(false, 1);

    private final boolean holdsTheImmune;
    private final int strength;

    IceKind(boolean holdsTheImmune, int strength) {
        this.holdsTheImmune = holdsTheImmune;
        this.strength = strength;
    }

    /** The stronger of this ice and {@code other}: the one a freeze is made of once both land on it. */
    public IceKind strongerOf(IceKind other) {
        return other.strength > this.strength ? other : this;
    }

    /** Whether this ice freezes an enemy that an immunity to freezing would keep a freeze off. */
    public boolean holdsTheImmune() {
        return this.holdsTheImmune;
    }
}
