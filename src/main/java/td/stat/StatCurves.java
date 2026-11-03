package td.stat;

/** What a point of a diminishing stat is worth: the curves a hit uses and a sheet reads. */
public final class StatCurves {

    private static final float CURVE = 100f;

    private StatCurves() {
    }

    /** {@code 100 / (100 + armor)}: each point matters less than the one before. */
    public static float mitigationMultiplier(float armor) {
        return CURVE / (CURVE + Math.max(0f, armor));
    }

    /**
     * {@code max(0, 1 + spirit / 100)}: what the enemy's recovery is worth. Regeneration restores this many times
     * itself, and a shield holds this many times its points; at the floor both are worth nothing.
     */
    public static float restoreFactor(float spirit) {
        return Math.max(0f, 1f + spirit / CURVE);
    }

    /** {@code agility / (agility + 100)}: each point matters less than the one before, and it never reaches 1. */
    public static float dodgeChance(float agility) {
        float effective = Math.max(0f, agility);
        return effective / (effective + CURVE);
    }
}
