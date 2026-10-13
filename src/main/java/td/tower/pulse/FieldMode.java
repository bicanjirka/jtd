package td.tower.pulse;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** A rule that holds while an enemy is inside the field. */
public enum FieldMode {
    /** Every enemy inside is Silenced. */
    SILENCE,
    /** Nothing inside can be healed or shielded. */
    DEAD_ZONE,
    /** Armor is lowered inside. */
    CORROSION,
    /** What a shield absorbs of the field's hits is dealt back to the enemy. */
    MIRROR,
    /** Enemies inside are chilled and Anchored. */
    UNDERTOW,
    /** Enemies inside take extra damage from every source. */
    KILL_ZONE,
    /** Each second inside costs spirit, and lost spirit makes the field hit harder. */
    SOUL_DRAIN,
    /** Each tick has a chance to add a Sundered or Exposed. */
    RATTLE;

    /** Rattle Field's chance each tick to add a stack, and how long an Exposed it adds lasts. */
    public static final double RATTLE_CHANCE = 0.05;
    public static final float RATTLE_EXPOSED_SECONDS = 4f;
    /** Soul Drain's cost each second, and the spirit at which it has taken everything. */
    public static final int SOUL_DRAIN_SPIRIT_PER_SECOND = 5;
    public static final float SOUL_DRAIN_SPIRIT_FLOOR = -100f;
    /** Each point of spirit below zero makes the field hit this much harder under Soul Drain. */
    public static final float SOUL_DRAIN_DAMAGE_PER_POINT = 0.01f;

    /** What the rule does, lower-case first: {@code "enemies inside are Silenced"}. */
    public String describe() {
        return switch (this) {
            case SILENCE -> "enemies inside are Silenced";
            case DEAD_ZONE -> "nothing inside can be healed or shielded";
            case CORROSION -> Math.round(EffectKind.CORRODED.modifiersAt(1).flat(EnemyStat.ARMOR))
                    + " armor while inside, armor stopping at 0";
            case MIRROR -> "what a shield absorbs of the field's hits is dealt back to its enemy as magic";
            case UNDERTOW -> "inside, enemies are chilled "
                    + PerkText.percent(1f - EffectKind.UNDERTOW.modifiersAt(1).get(EnemyStat.MOVE_SPEED).multiply())
                    + " and the chill doesn't fade while they stay, it counts for freezes, and they are Anchored";
            case KILL_ZONE -> "enemies inside take +"
                    + PerkText.percent(EffectKind.KILL_ZONE.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN))
                    + " damage from every source";
            case SOUL_DRAIN -> "each second inside costs " + SOUL_DRAIN_SPIRIT_PER_SECOND + " spirit, and the field deals +"
                    + PerkText.percent(SOUL_DRAIN_DAMAGE_PER_POINT) + " damage per point of spirit below zero; at "
                    + Math.round(SOUL_DRAIN_SPIRIT_FLOOR) + " "
                    + PerkText.times(1f - SOUL_DRAIN_DAMAGE_PER_POINT * SOUL_DRAIN_SPIRIT_FLOOR)
                    + " damage and no heals or shields";
            case RATTLE -> "each tick, a " + PerkText.percent((float) RATTLE_CHANCE) + " chance to add a stack of Sundered or "
                    + "Exposed: about " + Math.round(RATTLE_CHANCE * TickRate.TICKS_PER_SECOND) + " a second";
        };
    }
}
