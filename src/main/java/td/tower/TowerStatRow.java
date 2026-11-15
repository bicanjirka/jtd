package td.tower;

import td.effect.EffectKind;
import td.stat.StatDirection;
import td.stat.StatGroup;
import td.stat.StatTerm;
import td.stat.StatUnit;
import td.term.TermText;

import java.util.Locale;

/** A row a tower works out from its stats rather than reads off one: how much and how often it deals. */
public enum TowerStatRow implements StatTerm {
    /** In hundredths of a point a hit. */
    PHYSICAL_DAMAGE("Phys dmg", "Physical damage", StatUnit.DAMAGE_PER_HIT, StatDirection.HIGHER),
    MAGIC_DAMAGE("Magic dmg", "Magic damage", StatUnit.DAMAGE_PER_HIT, StatDirection.HIGHER),
    /** In points a second, for a tower that deals damage while an enemy stays in its field. */
    PHYSICAL_FIELD_DAMAGE("Phys dmg", "Physical damage", StatUnit.DAMAGE_PER_SECOND, StatDirection.HIGHER),
    MAGIC_FIELD_DAMAGE("Magic dmg", "Magic damage", StatUnit.DAMAGE_PER_SECOND, StatDirection.HIGHER),
    /** Attacks a second. */
    FIRE_RATE("Fire rate", "Fire rate", StatUnit.PER_SECOND, StatDirection.HIGHER),
    /** Points a second on one target, before its defences. */
    PHYSICAL_DPS("DPS", "Damage per second", StatUnit.PER_TARGET, StatDirection.HIGHER),
    MAGIC_DPS("DPS", "Damage per second", StatUnit.PER_TARGET, StatDirection.HIGHER),
    /** Fuel a second it pours into each enemy in its cone, in points. */
    FLAME("Flame", "Flame", StatUnit.DAMAGE_PER_SECOND, StatDirection.HIGHER),
    /** Fuel one stream gives an enemy, in points. */
    STREAM_FUEL("Per stream", "Fuel a stream", StatUnit.PER_TARGET, StatDirection.HIGHER),
    /** Points a second it really dealt this wave, on everything it reached. */
    WAVE_DPS("Wave DPS", "Damage per second this wave", StatUnit.DAMAGE_PER_SECOND, StatDirection.HIGHER),
    /** In cells a second. */
    PROJECTILE_SPEED("Proj speed", "Projectile speed", StatUnit.CELLS_PER_SECOND, StatDirection.HIGHER),
    /** A share of the standard projectile's size. */
    PROJECTILE_SIZE("Proj size", "Projectile size", StatUnit.PERCENT, StatDirection.NEUTRAL);

    private final String shortName;
    private final String title;
    private final StatUnit unit;
    private final StatDirection direction;

    TowerStatRow(String shortName, String title, StatUnit unit, StatDirection direction) {
        this.shortName = shortName;
        this.title = title;
        this.unit = unit;
        this.direction = direction;
    }

    @Override
    public String key() {
        return "towerrow." + this.name().toLowerCase(Locale.ROOT);
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
        return switch (this) {
            case PHYSICAL_DAMAGE, MAGIC_DAMAGE, PHYSICAL_FIELD_DAMAGE, MAGIC_FIELD_DAMAGE, FIRE_RATE,
                 PHYSICAL_DPS, MAGIC_DPS, FLAME, STREAM_FUEL, WAVE_DPS -> StatGroup.DAMAGE;
            case PROJECTILE_SPEED, PROJECTILE_SIZE -> StatGroup.DELIVERY;
        };
    }

    @Override
    public TermText describe() {
        return switch (this) {
            case PHYSICAL_DAMAGE -> TermText.of("what one hit deals, before armor and plating");
            case MAGIC_DAMAGE -> TermText.of("what one hit deals, before magic resist and insulation");
            case PHYSICAL_FIELD_DAMAGE -> TermText.of("what it deals each second to every enemy in its field, "
                    + "before armor and plating");
            case MAGIC_FIELD_DAMAGE -> TermText.of("what it deals each second to every enemy in its field, "
                    + "before magic resist and insulation");
            case FIRE_RATE -> TermText.of("how many attacks it makes each second");
            case PHYSICAL_DPS, MAGIC_DPS -> TermText.of("damage x fire rate x average crit on one target, before its "
                    + "defences; it leaves out splash, effects, damage over time, extra hits and misses");
            case FLAME -> TermText.of("the fuel a second its stream pours into the ").plus(EffectKind.BURN)
                    .plus(" of each enemy in its cone; a pool deals all its fuel, before the enemy's defences");
            case STREAM_FUEL -> TermText.of("the fuel one whole stream gives an enemy in its cone, outside the ")
                    .plus(TowerTerm.CORE);
            case WAVE_DPS -> TermText.of("what it really dealt this wave, to every enemy and after their defences, "
                    + "from its first damage to its last");
            case PROJECTILE_SPEED -> TermText.of("how fast its shots fly");
            case PROJECTILE_SIZE -> TermText.of("how big its shots are, as a share of the usual");
        };
    }

    /** The damage row of a tower that deals {@code magic} or physical damage, per hit or in a field. */
    static TowerStatRow damage(boolean magic, boolean field) {
        if (field) {
            return magic ? MAGIC_FIELD_DAMAGE : PHYSICAL_FIELD_DAMAGE;
        }
        return magic ? MAGIC_DAMAGE : PHYSICAL_DAMAGE;
    }

    static TowerStatRow dps(boolean magic) {
        return magic ? MAGIC_DPS : PHYSICAL_DPS;
    }
}
