package td.stat;

import td.damage.DamageType;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Locale;
import java.util.Optional;

/**
 * One number on an enemy's stat sheet, with the base value an enemy has unless its definition
 * authors another, the range the resolved value is clamped to, and how the player reads it.
 * <p>
 * Plating, insulation and regeneration are in the hundredths of a point that health and damage use.
 */
public enum EnemyStat implements StatKey, StatTerm {

    /** Physical mitigation: a hit is multiplied by {@code 100 / (100 + armor)}. */
    ARMOR(0f, 0f, Float.MAX_VALUE, "Armor", "Armor", StatUnit.POINTS, StatDirection.LOWER, StatGroup.DEFENCE,
            "cuts physical damage; each point is worth less than the one before"),
    MAGIC_RESIST(0f, 0f, Float.MAX_VALUE, "Magic resist", "Magic resist", StatUnit.POINTS, StatDirection.LOWER, StatGroup.DEFENCE,
            "cuts magic damage; each point is worth less than the one before"),
    /** Points every physical hit loses after mitigation. */
    PLATING(0f, 0f, Float.MAX_VALUE, "Plating", "Plating", StatUnit.LOSS_PER_HIT, StatDirection.LOWER, StatGroup.DEFENCE,
            "takes a fixed amount off every physical hit, after armor"),
    /** Points every magic hit loses after mitigation. */
    INSULATION(0f, 0f, Float.MAX_VALUE, "Insulation", "Insulation", StatUnit.LOSS_PER_HIT, StatDirection.LOWER,
            StatGroup.DEFENCE, "takes a fixed amount off every magic hit, after magic resist"),
    /** Pixels per tick; its base is the definition's speed times the spawn multiplier. */
    MOVE_SPEED(0f, 0f, Float.MAX_VALUE, "Move speed", "Move speed", StatUnit.PIXELS_PER_TICK, StatDirection.LOWER, StatGroup.SPEED,
            "how fast it walks the path"),
    // Floored above zero so a stacked reduction never makes an enemy immune by accident.
    PHYSICAL_DAMAGE_TAKEN(1f, 0.1f, Float.MAX_VALUE, "Phys taken", "Physical damage taken", StatUnit.PERCENT,
            StatDirection.HIGHER, StatGroup.DAMAGE_TAKEN, "the share of every physical hit it takes, after armor"),
    MAGIC_DAMAGE_TAKEN(1f, 0.1f, Float.MAX_VALUE, "Magic taken", "Magic damage taken", StatUnit.PERCENT, StatDirection.HIGHER,
            StatGroup.DAMAGE_TAKEN, "the share of every magic hit it takes, after magic resist"),
    /**
     * Each point removes 1% of an attacker's crit chance and crit bonus; below zero each point adds
     * 1% to both; 100 is crit-immune.
     */
    RESILIENCE(0f, -100f, 100f, "Resilience", "Resilience", StatUnit.POINTS, StatDirection.LOWER, StatGroup.DEFENCE,
            "shrinks the crit chance and crit damage of hits on it; below zero it grows them"),
    /** An aimed hit misses with chance {@code agility / (agility + 100)}, after the attacker's accuracy. */
    AGILITY(0f, 0f, Float.MAX_VALUE, "Agility", "Agility", StatUnit.POINTS, StatDirection.LOWER, StatGroup.DEFENCE,
            "the chance to dodge an aimed hit, less the accuracy of the tower; each point is worth less than the one before"),
    /** Multiplies damage that ticks (a burn, a poison, a field), whatever its type. */
    PERIODIC_DAMAGE_TAKEN(1f, 0.1f, Float.MAX_VALUE, "DoT taken", "Damage over time taken", StatUnit.PERCENT,
            StatDirection.HIGHER, StatGroup.DAMAGE_TAKEN, "the share of damage that ticks it takes, whatever its type"),
    /** What the enemy's regeneration restores and its shields hold is scaled by {@code StatCurves.restoreFactor}. */
    SPIRIT(0f, -100f, Float.MAX_VALUE, "Spirit", "Spirit", StatUnit.POINTS, StatDirection.LOWER, StatGroup.HEALING,
            "makes its regeneration and shields stronger; below zero weaker, and at -100 worth nothing"),
    REGENERATION(0f, -Float.MAX_VALUE, Float.MAX_VALUE, "Regeneration", "Regeneration", StatUnit.HP_PER_SECOND,
            StatDirection.LOWER, StatGroup.HEALING, "health it gains back every second; below zero, health it loses"),
    CHILL_RESIST(0f, 0f, 1f, "Chill resist", "Chill resist", StatUnit.PERCENT, StatDirection.LOWER, StatGroup.RESISTS,
            "cuts how long that effect lasts on it; at full it never takes hold"),
    BURN_RESIST(0f, 0f, 1f, "Burn resist", "Burn resist", StatUnit.PERCENT, StatDirection.LOWER, StatGroup.RESISTS,
            "cuts how long that effect lasts on it; at full it never takes hold"),
    FREEZE_RESIST(0f, 0f, 1f, "Freeze resist", "Freeze resist", StatUnit.PERCENT, StatDirection.LOWER,
            StatGroup.RESISTS, "cuts how long that effect lasts on it; at full it never takes hold"),
    /** At {@code 1} towers cannot target the enemy. */
    STEALTH(0f, 0f, 1f, "Stealth", "Stealth", StatUnit.PERCENT, StatDirection.LOWER, StatGroup.RESISTS,
            "at full towers cannot aim at it");

    private static final float FULL = 100f;

    private final float defaultBase;
    private final float min;
    private final float max;
    private final String shortName;
    private final String title;
    private final StatUnit unit;
    private final StatDirection direction;
    private final StatGroup group;
    private final String meaning;

    EnemyStat(float defaultBase, float min, float max, String shortName, String title, StatUnit unit,
              StatDirection direction, StatGroup group, String meaning) {
        this.defaultBase = defaultBase;
        this.min = min;
        this.max = max;
        this.shortName = shortName;
        this.title = title;
        this.unit = unit;
        this.direction = direction;
        this.group = group;
        this.meaning = meaning;
    }

    public static EnemyStat mitigationFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> ARMOR;
            case MAGIC -> MAGIC_RESIST;
        };
    }

    public static EnemyStat flatReductionFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> PLATING;
            case MAGIC -> INSULATION;
        };
    }

    public static EnemyStat damageTakenFor(DamageType type) {
        return switch (type) {
            case PHYSICAL -> PHYSICAL_DAMAGE_TAKEN;
            case MAGIC -> MAGIC_DAMAGE_TAKEN;
        };
    }


    @Override
    public float defaultBase() {
        return this.defaultBase;
    }

    @Override
    public float clamp(float value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    @Override
    public String key() {
        return "enemystat." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public TermText describe() {
        return TermText.of(this.meaning);
    }

    @Override
    public String shortName() {
        return this.shortName;
    }

    @Override
    public StatUnit unit() {
        return this.unit;
    }

    @Override
    public StatDirection direction() {
        return this.direction;
    }

    @Override
    public StatGroup group() {
        return this.group;
    }

    /** What the stats read on a curve do at {@code value}; nothing at a value that does nothing. */
    @Override
    public Optional<String> inline(float value) {
        return switch (this) {
            case ARMOR, MAGIC_RESIST -> value > 0f
                    ? Optional.of(TermNumbers.signedPercent(StatCurves.mitigationMultiplier(value) - 1f) + " dmg")
                    : Optional.empty();
            case AGILITY -> value > 0f
                    ? Optional.of("dodges " + TermNumbers.percent(StatCurves.dodgeChance(value)))
                    : Optional.empty();
            case RESILIENCE -> resilienceMeaning(value);
            case SPIRIT -> value == 0f ? Optional.empty()
                    : Optional.of(TermNumbers.signedPercent(value / FULL) + " recovery");
            case PLATING, INSULATION, CHILL_RESIST, BURN_RESIST, FREEZE_RESIST, MOVE_SPEED, PHYSICAL_DAMAGE_TAKEN, MAGIC_DAMAGE_TAKEN, PERIODIC_DAMAGE_TAKEN,
                 REGENERATION, STEALTH -> Optional.empty();
        };
    }

    private static Optional<String> resilienceMeaning(float value) {
        if (value >= FULL) {
            return Optional.of("immune");
        }
        return value == 0f ? Optional.empty() : Optional.of(TermNumbers.signedPercent(-value / FULL) + " crits");
    }
}
