package td.tower.pulse;

import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.stat.EnemyStat;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** A rule that holds while an enemy is inside the field. */
public enum FieldMode {
    /** Every enemy inside is Silenced. */
    SILENCE,
    /** Nothing inside can be healed or shielded. */
    DEAD_ZONE,
    /** Armor and magic resist are lowered inside, more the longer the enemy stays. */
    CORROSION,
    /** Enemies inside are Anchored, and the drag counts as a chill for a freeze. */
    UNDERTOW,
    /** Enemies inside take extra damage from every source, more the longer they stay. */
    FRAIL,
    /** Each second inside costs spirit. */
    SOUL_DRAIN,
    /** An enemy coming in loses every buff and shield. */
    PURGE;

    /** Soul Drain's cost each second. */
    public static final int SOUL_DRAIN_SPIRIT_PER_SECOND = 15;
    /** How long a rule the field holds on an enemy lasts after the enemy leaves it, at neutral spirit. */
    public static final int LINGER_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    /** What the rule does, lower-case first: {@code "enemies inside are Silenced, and for 2s after they leave"}. */
    public String describe() {
        return this.lingers() ? this.rule() + ", and for " + PerkText.seconds(LINGER_TICKS) + " after they leave" : this.rule();
    }

    /** Whether the rule holds an effect on the enemies inside, which outlasts their leaving. */
    private boolean lingers() {
        return switch (this) {
            case SILENCE, DEAD_ZONE, CORROSION, UNDERTOW, FRAIL -> true;
            case SOUL_DRAIN, PURGE -> false;
        };
    }

    private String rule() {
        return switch (this) {
            case SILENCE -> "enemies inside are Silenced";
            case DEAD_ZONE -> "nothing inside can be healed or shielded";
            case CORROSION -> "armor and magic resist " + resistAt(FieldStrength.ENTRY) + " on entry, "
                    + resistAt(FieldStrength.FULL) + " at full Toll, stopping at 0";
            case UNDERTOW -> "enemies inside are Anchored, and the drag counts as a chill for a freeze";
            case FRAIL -> "enemies inside take +" + PerkText.percent(damageTakenAt(FieldStrength.ENTRY))
                    + " damage from every source on entry, +" + PerkText.percent(damageTakenAt(FieldStrength.FULL))
                    + " at full Toll";
            case SOUL_DRAIN -> "each second inside costs " + SOUL_DRAIN_SPIRIT_PER_SECOND + " spirit, so every debuff on "
                    + "the enemy, the field's own included, wears off slower and heals land weaker";
            case PURGE -> "an enemy loses every buff and shield as it comes in";
        };
    }

    private static long resistAt(int strength) {
        return Math.round(EffectKind.CORRODED.modifiersAt(strength).flat(EnemyStat.ARMOR));
    }

    private static float damageTakenAt(int strength) {
        return EffectKind.FRAIL.modifiersAt(strength).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN);
    }
}
