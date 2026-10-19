package td.effect;

/**
 * How hard a field drags, in points of speed taken away out of a hundred: one stack each. Every field tops the stacks
 * up to its own drag, so of two fields the stronger holds, and no field drags past {@link #MOST}.
 */
public final class DragStrength {

    /** The drag of a field with no node that deepens it. */
    public static final int BASE = 15;
    /** The most any field drags, however deep its nodes and its Toll make it. */
    public static final int MOST = 50;

    private DragStrength() {
    }

    /** {@code atFull} points of drag at a field strength of {@code strength} stacks, never below the base or above the most. */
    public static int at(int atFull, int strength) {
        return Math.clamp(Math.round((float) atFull * strength / FieldStrength.FULL), BASE, MOST);
    }
}
