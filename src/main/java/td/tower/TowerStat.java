package td.tower;

import td.damage.AttackProfile;
import td.damage.DamageUnits;
import td.stat.StatKey;
import td.util.TickRate;

import java.math.BigDecimal;

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
    WITHER_INTERVAL(0f, 0f, Float.MAX_VALUE, Unit.TICKS, "Hits apply Vulnerable every", true);

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
