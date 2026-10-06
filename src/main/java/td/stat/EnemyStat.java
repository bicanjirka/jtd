package td.stat;

import td.damage.DamageType;

/**
 * One number on an enemy's stat sheet, with the base value an enemy has unless its definition
 * authors another and the range the resolved value is clamped to.
 * <p>
 * Plating and regeneration are in the hundredths of a point that health and damage use.
 */
public enum EnemyStat {

    /** Physical mitigation: a hit is multiplied by {@code 100 / (100 + armor)}. */
    ARMOR(0f, 0f, Float.MAX_VALUE),
    MAGIC_RESIST(0f, 0f, Float.MAX_VALUE),
    PHYSICAL_PLATING(0f, 0f, Float.MAX_VALUE),
    MAGIC_PLATING(0f, 0f, Float.MAX_VALUE),
    /** Pixels per tick; its base is the definition's speed times the spawn multiplier. */
    MOVE_SPEED(0f, 0f, Float.MAX_VALUE),
    // Floored above zero so a stacked reduction never makes an enemy immune by accident.
    PHYSICAL_DAMAGE_TAKEN(1f, 0.1f, Float.MAX_VALUE),
    MAGIC_DAMAGE_TAKEN(1f, 0.1f, Float.MAX_VALUE),
    /**
     * Each point removes 1% of an attacker's crit chance and crit bonus; 100 is crit-immune. Below
     * zero it only adds to the crit bonus taken, never to the chance.
     */
    RESILIENCE(0f, -100f, 100f),
    CRIT_CHANCE_TAKEN(1f, 0f, Float.MAX_VALUE),
    /** Heals and shields the enemy receives are scaled by {@code max(0, 1 + spirit / 100)}. */
    SPIRIT(0f, -100f, Float.MAX_VALUE),
    REGENERATION(0f, 0f, Float.MAX_VALUE),
    CHILL_RESIST(0f, 0f, 1f),
    BURN_RESIST(0f, 0f, 1f),
    FREEZE_RESIST(0f, 0f, 1f),
    /** At {@code 1} towers cannot target the enemy. */
    STEALTH(0f, 0f, 1f);

    private final float defaultBase;
    private final float min;
    private final float max;

    EnemyStat(float defaultBase, float min, float max) {
        this.defaultBase = defaultBase;
        this.min = min;
        this.max = max;
    }

    public static EnemyStat mitigationFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> ARMOR;
            case MAGIC -> MAGIC_RESIST;
        };
    }

    public static EnemyStat platingFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> PHYSICAL_PLATING;
            case MAGIC -> MAGIC_PLATING;
        };
    }

    public static EnemyStat damageTakenFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> PHYSICAL_DAMAGE_TAKEN;
            case MAGIC -> MAGIC_DAMAGE_TAKEN;
        };
    }

    public float defaultBase() {
        return this.defaultBase;
    }

    public float clamp(float value) {
        return Math.max(this.min, Math.min(this.max, value));
    }
}
