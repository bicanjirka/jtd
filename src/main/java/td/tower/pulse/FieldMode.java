package td.tower.pulse;

import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

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

    /** What the rule does, lower-case first: {@code "enemies inside gain [Silence], and keep it 2 s after they leave"}. */
    public TermText describe() {
        TermText rule = this.rule();
        if (!this.lingers()) {
            return rule;
        }
        String them = this.applies().size() > 1 ? "them" : "it";
        return rule.plus(", and keep " + them + " "
                + TermNumbers.seconds(LINGER_TICKS / (float) TickRate.TICKS_PER_SECOND) + " after they leave");
    }

    /** The effects the rule puts on the enemies inside. */
    public Set<EffectKind> applies() {
        return switch (this) {
            case SILENCE -> Set.of(EffectKind.SILENCED);
            case DEAD_ZONE -> Set.of(EffectKind.DEAD_ZONE);
            case CORROSION -> Set.of(EffectKind.CORRODED);
            case UNDERTOW -> Set.of(EffectKind.ANCHORED, EffectKind.UNDERTOW);
            case FRAIL -> Set.of(EffectKind.FRAIL);
            case SOUL_DRAIN, PURGE -> Set.of();
        };
    }

    /** Whether the rule holds an effect on the enemies inside, which outlasts their leaving. */
    private boolean lingers() {
        return switch (this) {
            case SILENCE, DEAD_ZONE, CORROSION, UNDERTOW, FRAIL -> true;
            case SOUL_DRAIN, PURGE -> false;
        };
    }

    private TermText rule() {
        return switch (this) {
            case SILENCE -> TermText.of("enemies inside gain ").plus(EffectKind.SILENCED);
            case DEAD_ZONE -> TermText.of("enemies inside gain ").plus(EffectKind.DEAD_ZONE);
            case CORROSION -> TermText.of("enemies inside gain ").plus(EffectKind.CORRODED)
                    .plus(": " + TermNumbers.signed(resistAt(FieldStrength.ENTRY)) + " armor and magic resist on entry, "
                            + TermNumbers.signed(resistAt(FieldStrength.FULL)) + " at full ")
                    .plus(EffectKind.TOLL).plus(", stopping at 0");
            case UNDERTOW -> TermText.of("enemies inside gain ").plus(EffectKind.ANCHORED).plus(" and ")
                    .plus(EffectKind.UNDERTOW);
            case FRAIL -> TermText.of("enemies inside gain ").plus(EffectKind.FRAIL)
                    .plus(": " + TermNumbers.signedPercent(damageTakenAt(FieldStrength.ENTRY))
                            + " damage taken from every source on entry, "
                            + TermNumbers.signedPercent(damageTakenAt(FieldStrength.FULL)) + " at full ")
                    .plus(EffectKind.TOLL);
            case SOUL_DRAIN -> TermText.of("each second inside costs " + SOUL_DRAIN_SPIRIT_PER_SECOND
                    + " spirit, so every debuff on the enemy, the field's own included, wears off slower and heals land weaker");
            case PURGE -> TermText.of("an enemy loses every buff and ").plus(EffectKind.SHIELD)
                    .plus(" as it comes in");
        };
    }

    private static long resistAt(int strength) {
        return Math.round(EffectKind.CORRODED.modifiersAt(strength).flat(EnemyStat.ARMOR));
    }

    private static float damageTakenAt(int strength) {
        return EffectKind.FRAIL.modifiersAt(strength).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN);
    }
}
