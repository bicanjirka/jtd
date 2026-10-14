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
    /** Armor is lowered inside, more the longer the enemy stays. */
    CORROSION,
    /** What a shield absorbs of the field's hits is dealt back to the enemy. */
    MIRROR,
    /** Enemies inside are chilled, more the longer they stay, and Anchored. */
    UNDERTOW,
    /** Enemies inside take extra damage from every source, more the longer they stay. */
    KILL_ZONE,
    /** Enemies at full Toll are Vulnerable while inside and a while after. */
    AMPLIFY,
    /** Each second inside costs spirit; with none left, nothing heals or shields. */
    SOUL_DRAIN,
    /** Each tick has a chance to add a Sundered or Exposed. */
    RATTLE;

    /** Rattle Field's chance each tick to add a stack, and how long an Exposed it adds lasts. */
    public static final double RATTLE_CHANCE = 0.05;
    public static final float RATTLE_EXPOSED_SECONDS = 4f;
    /** Soul Drain's cost each second, and the spirit at which it has taken everything. */
    public static final int SOUL_DRAIN_SPIRIT_PER_SECOND = 15;
    public static final float SOUL_DRAIN_SPIRIT_FLOOR = -100f;
    /** How long an enemy stays Vulnerable after it leaves the field at full Toll, and how many stacks it holds. */
    public static final int AMPLIFY_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);
    public static final int AMPLIFY_STACKS = 2;

    /** What the rule does, lower-case first: {@code "enemies inside are Silenced"}. */
    public String describe() {
        return switch (this) {
            case SILENCE -> "enemies inside are Silenced";
            case DEAD_ZONE -> "nothing inside can be healed or shielded";
            case CORROSION -> "armor " + armorAt(FieldStrength.ENTRY) + " on entry, " + armorAt(FieldStrength.FULL)
                    + " at full Toll, armor stopping at 0";
            case MIRROR -> "what a shield absorbs of the field's hits is dealt back to its enemy as magic";
            case UNDERTOW -> "inside, enemies are chilled " + PerkText.percent(chillAt(FieldStrength.ENTRY)) + " on entry, "
                    + PerkText.percent(chillAt(FieldStrength.FULL)) + " at full Toll; the chill doesn't fade while they "
                    + "stay and counts for freezes, and they are Anchored";
            case KILL_ZONE -> "enemies inside take +" + PerkText.percent(damageTakenAt(FieldStrength.ENTRY))
                    + " damage from every source on entry, +" + PerkText.percent(damageTakenAt(FieldStrength.FULL))
                    + " at full Toll";
            case AMPLIFY -> "an enemy at full Toll is Vulnerable while inside and " + PerkText.seconds(AMPLIFY_TICKS)
                    + " after: +" + PerkText.percent(EffectKind.VULNERABLE.modifiersAt(AMPLIFY_STACKS)
                    .flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " damage taken";
            case SOUL_DRAIN -> "each second inside costs " + SOUL_DRAIN_SPIRIT_PER_SECOND + " spirit, so every debuff on "
                    + "the enemy wears off slower; at " + Math.round(SOUL_DRAIN_SPIRIT_FLOOR) + " no heals or shields";
            case RATTLE -> "each tick, a " + PerkText.percent((float) RATTLE_CHANCE) + " chance to add a stack of Sundered or "
                    + "Exposed: about " + Math.round(RATTLE_CHANCE * TickRate.TICKS_PER_SECOND) + " a second";
        };
    }

    private static long armorAt(int strength) {
        return Math.round(EffectKind.CORRODED.modifiersAt(strength).flat(EnemyStat.ARMOR));
    }

    private static float chillAt(int strength) {
        return 1f - EffectKind.UNDERTOW.modifiersAt(strength).get(EnemyStat.MOVE_SPEED).multiply();
    }

    private static float damageTakenAt(int strength) {
        return EffectKind.KILL_ZONE.modifiersAt(strength).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN);
    }
}
