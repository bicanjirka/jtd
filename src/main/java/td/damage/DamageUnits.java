package td.damage;

/**
 * The unit health, damage, plating and regeneration are stored in: a fixed fraction of the point
 * the UI shows. Content is authored in points and converted here, so a percentage or a
 * damage-over-time pulse can remove part of a point without rounding it away.
 */
public final class DamageUnits {

    public static final int PER_POINT = 100;

    private DamageUnits() {
    }

    public static int ofPoints(float points) {
        return Math.round(points * PER_POINT);
    }

    public static float inPoints(float units) {
        return units / PER_POINT;
    }
}
