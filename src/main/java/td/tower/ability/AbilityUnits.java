package td.tower.ability;

import td.damage.DamageUnits;

/**
 * An amount of a tower's ability currency. Content and the player speak in whole units and their fractions; the
 * finer count kept here only lets a stream of small hits add up without rounding away.
 */
public record AbilityUnits(long parts) implements Comparable<AbilityUnits> {

    private static final long PARTS_PER_UNIT = 1_000_000L;
    private static final AbilityUnits NONE = new AbilityUnits(0L);

    public AbilityUnits {
        if (parts < 0L) {
            throw new IllegalArgumentException("An amount of currency is never negative: " + parts);
        }
    }

    public static AbilityUnits none() {
        return NONE;
    }

    public static AbilityUnits whole(int units) {
        return new AbilityUnits(units * PARTS_PER_UNIT);
    }

    /** {@code units} as content writes it: {@code 0.25f} is a quarter of a unit. */
    public static AbilityUnits of(float units) {
        return new AbilityUnits(Math.round((double) units * PARTS_PER_UNIT));
    }

    /**
     * What {@code credited} damage earns at {@code pointsPerUnit} damage a unit.
     *
     * @param credited the damage the hit was credited with, in {@link DamageUnits}
     */
    public static AbilityUnits forDamage(int credited, float pointsPerUnit) {
        if (pointsPerUnit <= 0f) {
            throw new IllegalArgumentException("A unit costs some damage: " + pointsPerUnit);
        }
        double units = DamageUnits.inPoints(Math.max(0, credited)) / (double) pointsPerUnit;
        return new AbilityUnits(Math.round(units * PARTS_PER_UNIT));
    }

    public AbilityUnits plus(AbilityUnits other) {
        return new AbilityUnits(this.parts + other.parts);
    }

    /** This less {@code other}, never below none. */
    public AbilityUnits minus(AbilityUnits other) {
        return new AbilityUnits(Math.max(0L, this.parts - other.parts));
    }

    public AbilityUnits atMost(AbilityUnits cap) {
        return this.parts <= cap.parts ? this : cap;
    }

    public boolean atLeast(AbilityUnits other) {
        return this.parts >= other.parts;
    }

    /** In units, for words and pictures only: no rule compares it. */
    public float inUnits() {
        return (float) ((double) this.parts / PARTS_PER_UNIT);
    }

    @Override
    public int compareTo(AbilityUnits other) {
        return Long.compare(this.parts, other.parts);
    }
}
