package td.effect;

/**
 * How strongly a field's effect holds, in stacks: {@link #FULL} is the effect as authored, and each stack is a tenth
 * of it. An enemy that has just entered the field feels it at {@link #ENTRY}; time inside adds a stack a second, up
 * to {@link #MOST}.
 */
public final class FieldStrength {

    public static final int ENTRY = 5;
    public static final int FULL = 10;
    public static final int MOST = 15;

    private FieldStrength() {
    }

    /** The stacks for an enemy that has spent {@code toll} stacks' worth of time in the field. */
    public static int at(int toll) {
        return Math.min(MOST, ENTRY + toll);
    }

    /** The factor one stack multiplies by, so that {@link #FULL} stacks multiply by {@code atFull}. */
    static float perStackOf(float atFull) {
        return (float) Math.pow(atFull, 1.0 / FULL);
    }
}
