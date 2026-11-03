package td.tower;

import td.damage.AttackProfile;
import td.effect.EffectKind;
import td.stat.StatDirection;
import td.stat.StatGroup;
import td.stat.StatKey;
import td.stat.StatModifier;
import td.stat.StatTerm;
import td.stat.StatUnit;
import td.stat.StatWording;
import td.term.TermText;

import java.util.Locale;
import java.util.Optional;

/**
 * One number on a tower's stat sheet: its default, its clamp, how the player reads it and when its row shows.
 * Constants are in sheet order within each group.
 */
public enum TowerStat implements StatKey, StatTerm {

    RANGE(0f, 0.5f, Float.MAX_VALUE, StatUnit.CELLS, "Range", "Range", StatDirection.HIGHER, StatGroup.REACH,
            Shows.IN_CORE_ROW, true),
    DEAD_ZONE(0f, 0f, Float.MAX_VALUE, StatUnit.CELLS, "Dead zone", "Dead zone", StatDirection.LOWER,
            StatGroup.REACH, Shows.OFF_DEFAULT, false),
    AREA_RADIUS(0f, 0f, Float.MAX_VALUE, StatUnit.CELLS, "Blast radius", "Blast radius", StatDirection.HIGHER,
            StatGroup.REACH, Shows.OFF_DEFAULT, false),
    BLAST_EDGE(0f, 0f, 1f, StatUnit.PERCENT, "Blast edge", "Blast edge damage", StatDirection.HIGHER,
            StatGroup.REACH, Shows.OFF_DEFAULT, false),
    /** The cone's full width. */
    CONE_WIDTH(0f, 0f, (float) (2 * Math.PI), StatUnit.DEGREES, "Cone width", "Cone width", StatDirection.NEUTRAL,
            StatGroup.REACH, Shows.OFF_DEFAULT, false),
    /** In hundredths of a point, like health. */
    DAMAGE(0f, 0f, Float.MAX_VALUE, StatUnit.DAMAGE_PER_HIT, "Damage", "Damage", StatDirection.HIGHER,
            StatGroup.DAMAGE, Shows.IN_CORE_ROW, true),
    /** Attacks per second are the base rate times {@code 1 + attack speed}. */
    ATTACK_SPEED(0f, -0.75f, 9f, StatUnit.SIGNED_PERCENT, "Fire rate", "Fire rate", StatDirection.HIGHER,
            StatGroup.DAMAGE, Shows.IN_CORE_ROW, true),
    BRACKET_DAMAGE_STEP(0f, 0f, Float.MAX_VALUE, StatUnit.SIGNED_PERCENT, "Bracketing", "Bracketing step",
            StatDirection.HIGHER, StatGroup.DAMAGE, Shows.OFF_DEFAULT, false),
    CRIT_CHANCE(0f, 0f, 1f, StatUnit.PERCENT, "Crit chance", "Critical chance", StatDirection.HIGHER, StatGroup.CRIT,
            Shows.OFF_DEFAULT, true),
    CRIT_MULTIPLIER(AttackProfile.DEFAULT_CRIT_MULTIPLIER, 1f, Float.MAX_VALUE, StatUnit.PERCENT, "Crit damage",
            "Critical damage", StatDirection.HIGHER, StatGroup.CRIT, Shows.WHILE_IT_CRITS, true),
    AIM_STACKS(0f, 0f, Float.MAX_VALUE, StatUnit.STACKS, "Steady Aim", "Steady Aim stacks", StatDirection.HIGHER,
            StatGroup.CRIT, Shows.OFF_DEFAULT, false),
    ARMOR_PENETRATION(0f, 0f, 1f, StatUnit.PERCENT, "Armor pen", "Armor penetration", StatDirection.HIGHER,
            StatGroup.CRIT, Shows.OFF_DEFAULT, true),
    ARMOR_PENETRATION_FLAT(0f, 0f, Float.MAX_VALUE, StatUnit.POINTS, "Armor pierce", "Armor ignored",
            StatDirection.HIGHER, StatGroup.CRIT, Shows.OFF_DEFAULT, true),
    MAGIC_PENETRATION(0f, 0f, 1f, StatUnit.PERCENT, "Magic pen", "Magic penetration", StatDirection.HIGHER,
            StatGroup.CRIT, Shows.OFF_DEFAULT, true),
    FLAT_PENETRATION(0f, 0f, 1f, StatUnit.PERCENT, "Flat pen", "Flat penetration", StatDirection.HIGHER,
            StatGroup.CRIT, Shows.OFF_DEFAULT, true),
    /** Agility points an aimed hit ignores. */
    ACCURACY(0f, 0f, Float.MAX_VALUE, StatUnit.POINTS, "Accuracy", "Accuracy", StatDirection.HIGHER, StatGroup.CRIT,
            Shows.OFF_DEFAULT, true),
    EFFECT_DURATION(1f, 0f, Float.MAX_VALUE, StatUnit.PERCENT, "Effect time", "Effect duration",
            StatDirection.HIGHER, StatGroup.EFFECTS, Shows.WHILE_IT_APPLIES, true),
    BUFF_DURATION(1f, 0f, Float.MAX_VALUE, StatUnit.PERCENT, "Buff time", "Buff duration", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, true),
    BURN_DURATION(0f, 1f, Float.MAX_VALUE, StatUnit.TICKS, "Burn time", "Burn duration", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    STOKE_STEP(0f, 0f, Float.MAX_VALUE, StatUnit.SIGNED_PERCENT, "Stoke step", "Stoke step", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    STOKE_GRACE(0f, 0f, Float.MAX_VALUE, StatUnit.TICKS, "Stoke grace", "Stoke kept after leaving",
            StatDirection.HIGHER, StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    BURN_POOL_CAP(0f, 1f, Float.MAX_VALUE, StatUnit.PERCENT, "Burn cap", "Burn pool cap", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    BURN_MARK_RATE(1f, 1f, Float.MAX_VALUE, StatUnit.PERCENT, "Scorch rate", "Scorch rate", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    FREEZE_DURATION(0f, 0f, Float.MAX_VALUE, StatUnit.TICKS, "Freeze time", "Freeze duration", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    SATURATION_CAP(0f, 0f, Float.MAX_VALUE, StatUnit.STACKS, "Saturation", "Saturation cap", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    SATURATION_INNER_STACKS(0f, 0f, Float.MAX_VALUE, StatUnit.STACKS, "Inner sat", "Saturation in the inner half",
            StatDirection.HIGHER, StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    TOLL_CAP(0f, 0f, Float.MAX_VALUE, StatUnit.STACKS, "Toll cap", "Toll cap", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    TOLL_RATE(0f, 0f, Float.MAX_VALUE, StatUnit.PER_SECOND, "Toll rate", "Toll a second", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    TOLL_LINGER(0f, 0f, Float.MAX_VALUE, StatUnit.TICKS, "Toll linger", "Toll lingers", StatDirection.HIGHER,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, false),
    /**
     * Only ever set, so two sources keep the shorter interval; 0 is never. Neutral: switching it on raises the
     * number, so neither way reads as better.
     */
    WITHER_INTERVAL(0f, 0f, Float.MAX_VALUE, StatUnit.TICKS, "Wither", "Wither interval", StatDirection.NEUTRAL,
            StatGroup.EFFECTS, Shows.OFF_DEFAULT, true),
    PROJECTILE_SPEED(1f, 0.1f, Float.MAX_VALUE, StatUnit.PERCENT, "Proj speed", "Projectile speed",
            StatDirection.HIGHER, StatGroup.DELIVERY, Shows.IN_CORE_ROW, false),
    PROJECTILE_SIZE(1f, 0.1f, Float.MAX_VALUE, StatUnit.PERCENT, "Proj size", "Projectile size",
            StatDirection.NEUTRAL, StatGroup.DELIVERY, Shows.IN_CORE_ROW, false),
    WAVE_SPEED(1f, 0.1f, Float.MAX_VALUE, StatUnit.PERCENT, "Wave speed", "Flame wave speed", StatDirection.HIGHER,
            StatGroup.DELIVERY, Shows.OFF_DEFAULT, false),
    NEST_CAPACITY(0f, 0f, Float.MAX_VALUE, StatUnit.MISSILES, "Nest", "Nest capacity", StatDirection.HIGHER,
            StatGroup.DELIVERY, Shows.OFF_DEFAULT, false),
    MISSILES_PER_LAUNCH(1f, 1f, Float.MAX_VALUE, StatUnit.COUNT, "Missiles", "Missiles a shot", StatDirection.HIGHER,
            StatGroup.DELIVERY, Shows.OFF_DEFAULT, false),
    PING_COUNT(0f, 0f, Float.MAX_VALUE, StatUnit.COUNT, "Pings", "Pings", StatDirection.HIGHER, StatGroup.DELIVERY,
            Shows.OFF_DEFAULT, false),
    BOUNTY(0f, 0f, Float.MAX_VALUE, StatUnit.SIGNED_PERCENT, "Bounty", "Bounty bonus", StatDirection.HIGHER,
            StatGroup.ECONOMY, Shows.OFF_DEFAULT, true),
    /** The share of an enemy's disruption that reaches the tower. */
    DISRUPTION_TAKEN(1f, 0f, 1f, StatUnit.PERCENT, "Jam taken", "Disruption taken", StatDirection.LOWER,
            StatGroup.ECONOMY, Shows.OFF_DEFAULT, true),
    AURA_POWER(0f, 0f, Float.MAX_VALUE, StatUnit.PERCENT, "Buff power", "Buff strength", StatDirection.HIGHER,
            StatGroup.GIVES, Shows.OFF_DEFAULT, false);

    /** No tower stat is read in pixels, so its words never need the board's cell size. */
    private static final StatWording BOARDLESS = new StatWording(Float.NaN);

    private final float defaultBase;
    private final float min;
    private final float max;
    private final StatUnit unit;
    private final String shortName;
    private final String title;
    private final StatDirection direction;
    private final StatGroup group;
    private final Shows shows;
    private final boolean universal;

    TowerStat(float defaultBase, float min, float max, StatUnit unit, String shortName, String title,
              StatDirection direction, StatGroup group, Shows shows, boolean universal) {
        this.defaultBase = defaultBase;
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.shortName = shortName;
        this.title = title;
        this.direction = direction;
        this.group = group;
        this.shows = shows;
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

    /** Whether every tower has this stat, so an aura or a status may change it on any tower. */
    public boolean universal() {
        return this.universal;
    }

    /** What {@code modifier} does to this stat, by its kind: {@code "+0.5 cells"}, {@code "+25%"}, {@code "x0.7"}. */
    public String change(StatModifier modifier) {
        return BOARDLESS.change(this, modifier);
    }

    /** {@code value} in this stat's unit: {@code "4.5 cells"}. */
    public String value(float value) {
        return BOARDLESS.value(this, value);
    }

    /** The stat named, then what {@code modifier} does to it: "Fire rate +25%". */
    public TermText changeText(StatModifier modifier) {
        return TermText.of(this).plus(" " + this.change(modifier));
    }

    /** When its own row shows on a tower's sheet. */
    public Shows shows() {
        return this.shows;
    }

    @Override
    public String key() {
        return "towerstat." + this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String title() {
        return this.title;
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

    /** Wither's number reads as a pause, so its row says what the pause is between. */
    @Override
    public Optional<String> inline(float value) {
        return this == WITHER_INTERVAL && value > 0f ? Optional.of("between marks") : Optional.empty();
    }

    @Override
    public TermText describe() {
        return switch (this) {
            case RANGE -> TermText.of("how far from its centre it reaches");
            case DEAD_ZONE -> TermText.of("how close to it an enemy is too close to aim at");
            case AREA_RADIUS -> TermText.of("how far from where a shot lands its blast reaches");
            case BLAST_EDGE -> TermText.of("the share of full damage the edge of a blast deals");
            case CONE_WIDTH -> TermText.of("how wide its cone of fire is");
            case DAMAGE -> TermText.of("what one hit deals, before the target's defences");
            case ATTACK_SPEED -> TermText.of("how often it attacks");
            case BRACKET_DAMAGE_STEP -> TermText.of("extra damage each shot in a row on one spot adds");
            case CRIT_CHANCE -> TermText.of("the chance a hit is critical");
            case CRIT_MULTIPLIER -> TermText.of("what a critical hit deals, as a share of a normal one");
            case AIM_STACKS -> TermText.format("how many stacks of {} it can hold", TowerTerm.STEADY_AIM);
            case ARMOR_PENETRATION -> TermText.of("the share of armor its hits ignore");
            case ARMOR_PENETRATION_FLAT -> TermText.of("points of armor its hits ignore, after the share");
            case MAGIC_PENETRATION -> TermText.of("the share of magic resist its hits ignore");
            case FLAT_PENETRATION -> TermText.of("the share of plating and insulation its hits ignore");
            case ACCURACY -> TermText.of("points of agility its aimed hits ignore");
            case EFFECT_DURATION -> TermText.of("how long what it puts on enemies lasts, as a share of the usual");
            case BUFF_DURATION -> TermText.of("how long its own timed buffs last, as a share of the usual");
            case BURN_DURATION -> TermText.format("how long a {} it lights lasts", EffectKind.BURN);
            case STOKE_STEP -> TermText.format("what each second of {} adds to its damage", TowerTerm.STOKE);
            case STOKE_GRACE -> TermText.format("how long {} holds after a target leaves", TowerTerm.STOKE);
            case BURN_POOL_CAP -> TermText.format("how much {} one enemy can hold, as a share of one", EffectKind.BURN);
            case BURN_MARK_RATE -> TermText.format("how fast it builds {}, as a share of the usual",
                    EffectKind.SCORCHED);
            case FREEZE_DURATION -> TermText.format("how long a {} it causes lasts", EffectKind.FREEZE);
            case SATURATION_CAP -> TermText.format("how many stacks of {} one enemy can hold", EffectKind.SATURATED);
            case SATURATION_INNER_STACKS -> TermText.format("stacks of {} a hit in the inner half adds",
                    EffectKind.SATURATED);
            case TOLL_CAP -> TermText.format("how many stacks of {} one enemy can hold", EffectKind.TOLL);
            case TOLL_RATE -> TermText.format("stacks of {} it adds each second", EffectKind.TOLL);
            case TOLL_LINGER -> TermText.format("how long {} stays once an enemy leaves", EffectKind.TOLL);
            case WITHER_INTERVAL -> TermText.format("the pause between its hits that leave {}",
                    EffectKind.VULNERABLE);
            case PROJECTILE_SPEED -> TermText.of("how fast its shots fly");
            case PROJECTILE_SIZE -> TermText.of("how big its shots are, as a share of the usual");
            case WAVE_SPEED -> TermText.of("how fast its flame wave travels, as a share of the usual");
            case NEST_CAPACITY -> TermText.of("how many missiles it can store");
            case MISSILES_PER_LAUNCH -> TermText.of("how many missiles one shot launches");
            case PING_COUNT -> TermText.format("how many enemies one {} marks", TowerTerm.PING);
            case BOUNTY -> TermText.of("extra cash for each kill it makes");
            case DISRUPTION_TAKEN -> TermText.format("the share of an enemy's {} that reaches it",
                    TowerTerm.JAMMED);
            case AURA_POWER -> TermText.format("how strong what its {} gives is, as a share of the usual",
                    TowerTerm.AURA);
        };
    }

    /** When a stat's own row shows on a tower's sheet. */
    public enum Shows {
        /** Never as its own row: a core row the tower builds shows it. */
        IN_CORE_ROW,
        /** When its base or current value is off its default. */
        OFF_DEFAULT,
        /** Also at its default while the tower can crit. */
        WHILE_IT_CRITS,
        /** Also at its default on a tower that applies any effect. */
        WHILE_IT_APPLIES
    }
}
