package td.tower;

import td.damage.AttackProfile;
import td.damage.DamageUnits;
import td.stat.StatKey;
import td.stat.StatModifier;
import td.util.TickRate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** One number on a tower's stat sheet, with its default, its clamp, and how a bonus to it reads. */
public enum TowerStat implements StatKey {

    /** In hundredths of a point, like health. */
    DAMAGE(0f, 0f, Float.MAX_VALUE, Unit.DAMAGE, "Damage", true),
    RANGE(0f, 0.5f, Float.MAX_VALUE, Unit.CELLS, "Range", true),
    /** Attacks per second are the base rate times {@code 1 + attack speed}. */
    ATTACK_SPEED(0f, -0.75f, 9f, Unit.PERCENT, "Attack speed", true),
    CRIT_CHANCE(0f, 0f, 1f, Unit.PERCENT, "Crit chance", true),
    CRIT_MULTIPLIER(AttackProfile.DEFAULT_CRIT_MULTIPLIER, 1f, Float.MAX_VALUE, Unit.PERCENT, "Crit damage", true),
    ARMOR_PENETRATION(0f, 0f, 1f, Unit.PERCENT, "Armor penetration", true),
    ARMOR_PENETRATION_FLAT(0f, 0f, Float.MAX_VALUE, Unit.POINTS, "Armor ignored", true),
    MAGIC_PENETRATION(0f, 0f, 1f, Unit.PERCENT, "Magic penetration", true),
    PLATING_PENETRATION(0f, 0f, 1f, Unit.PERCENT, "Plating penetration", true),
    BOUNTY(0f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Bounty", true),
    EFFECT_DURATION(1f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Effect duration", true),
    BUFF_DURATION(1f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Buff duration", true),
    /** The share of an enemy's disruption that reaches the tower. */
    DISRUPTION_TAKEN(1f, 0f, 1f, Unit.PERCENT, "Disruption taken", true),
    /** Only ever set, so two sources keep the shorter interval; 0 is never. */
    WITHER_INTERVAL(0f, 0f, Float.MAX_VALUE, Unit.TICKS, "Hits apply Vulnerable every", true),
    // The rest belong to one tower each; a tower that reads one sets it in its base stats.
    DEAD_ZONE(0f, 0f, Float.MAX_VALUE, Unit.CELLS, "Dead zone", false),
    AREA_RADIUS(0f, 0f, Float.MAX_VALUE, Unit.CELLS, "Blast radius", false),
    BLAST_EDGE(0f, 0f, 1f, Unit.PERCENT, "Blast edge damage", false),
    SATURATION_CAP(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Saturation cap", false),
    SATURATION_INNER_STACKS(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Saturation in the inner half", false),
    PROJECTILE_SPEED(1f, 0.1f, Float.MAX_VALUE, Unit.PERCENT, "Shell speed", false),
    PROJECTILE_SIZE(1f, 0.1f, Float.MAX_VALUE, Unit.PERCENT, "Shell size", false),
    BRACKET_DAMAGE_STEP(0f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Bracketing step", false),
    /** The cone's full width. */
    CONE_WIDTH(0f, 0f, (float) (2 * Math.PI), Unit.RADIANS, "Cone width", false),
    WAVE_SPEED(1f, 0.1f, Float.MAX_VALUE, Unit.PERCENT, "Wave speed", false),
    BURN_DURATION(0f, 1f, Float.MAX_VALUE, Unit.TICKS, "Burn duration", false),
    STOKE_STEP(0f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Stoke step", false),
    STOKE_GRACE(0f, 0f, Float.MAX_VALUE, Unit.TICKS, "Stoke kept after leaving", false),
    BURN_POOL_CAP(0f, 1f, Float.MAX_VALUE, Unit.MULTIPLE, "Burn pool cap", false),
    BURN_MARK_RATE(1f, 1f, Float.MAX_VALUE, Unit.PERCENT, "Scorched rate", false),
    TOLL_CAP(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Toll cap", false),
    TOLL_RATE(0f, 0f, Float.MAX_VALUE, Unit.DECIMAL, "Toll a second", false),
    TOLL_LINGER(0f, 0f, Float.MAX_VALUE, Unit.TICKS, "Toll lingers", false),
    FREEZE_DURATION(0f, 0f, Float.MAX_VALUE, Unit.TICKS, "Freeze duration", false),
    NEST_CAPACITY(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Nest", false),
    MISSILES_PER_LAUNCH(1f, 1f, Float.MAX_VALUE, Unit.COUNT, "Missiles a shot", false),
    PING_COUNT(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Pings", false),
    AIM_STACKS(0f, 0f, Float.MAX_VALUE, Unit.COUNT, "Steady Aim stacks", false),
    AURA_POWER(0f, 0f, Float.MAX_VALUE, Unit.PERCENT, "Buff strength", false);

    private final float defaultBase;
    private final float min;
    private final float max;
    private final Unit unit;
    private final String label;
    private final boolean universal;

    TowerStat(float defaultBase, float min, float max, Unit unit, String label, boolean universal) {
        this.defaultBase = defaultBase;
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.label = label;
        this.universal = universal;
    }

    @Override
    public float defaultBase() {
        return this.defaultBase;
    }

    @Override
    public float clamp(float value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    public Unit unit() {
        return this.unit;
    }

    public String label() {
        return this.label;
    }

    /** Whether every tower has this stat, so an aura or a status may change it on any tower. */
    public boolean universal() {
        return this.universal;
    }

    /**
     * How {@code modifier} reads as a bonus to this stat: a set value plainly, otherwise its flat in this
     * stat's unit, its percent and its multiplier as signed percentages, joined with ", ".
     */
    public String describe(StatModifier modifier) {
        if (modifier.hasSetValue()) {
            return this.unit.plain(modifier.setTo());
        }
        List<String> parts = new ArrayList<>();
        if (modifier.flat() != 0f) {
            parts.add(this.unit.signed(modifier.flat()));
        }
        if (modifier.percentAdd() != 0f) {
            parts.add(signedPercent(modifier.percentAdd()));
        }
        if (modifier.multiply() != 1f) {
            parts.add(signedPercent(modifier.multiply() - 1f));
        }
        return String.join(", ", parts);
    }

    private static String signedPercent(float fraction) {
        return (fraction < 0f ? "-" : "+") + Math.round(Math.abs(fraction) * 100) + "%";
    }

    /** How a stat's numbers read in a bonus row. */
    public enum Unit {
        DAMAGE, CELLS, PERCENT, POINTS, TICKS, COUNT, RADIANS, MULTIPLE, DECIMAL;

        /** A flat change, signed: "+6", "+0.6", "+25%", "+30", "+1.5 s", "+2", "+12°", "+x2", "+0.5". */
        public String signed(float delta) {
            return (delta < 0f ? "-" : "+") + this.plain(Math.abs(delta));
        }

        /** A value as it reads on its own: "6", "0.6", "25%", "30", "1.5 s", "2", "12°", "x2", "0.5". */
        public String plain(float value) {
            return switch (this) {
                case DAMAGE -> number(DamageUnits.inPoints(Math.round(value)));
                case CELLS, DECIMAL -> number(value);
                case PERCENT -> Math.round(value * 100) + "%";
                case POINTS, COUNT -> Integer.toString(Math.round(value));
                case TICKS -> number(value / TickRate.TICKS_PER_SECOND) + " s";
                case RADIANS -> Math.round(Math.toDegrees(value)) + "°";
                case MULTIPLE -> "x" + number(value);
            };
        }

        private static String number(float value) {
            return BigDecimal.valueOf(Math.round(value * 100) / 100.0).stripTrailingZeros().toPlainString();
        }
    }
}
