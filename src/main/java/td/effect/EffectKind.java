package td.effect;

import td.stat.EnemyStat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum EffectKind {
    CHILL,
    BURN,
    FREEZE,
    SHIELD,
    INVISIBLE,
    HEAL,
    VULNERABLE,
    REVEALED,
    POISON,
    /** The lasting mark of burning: each stack lowers resilience by one. */
    SCORCHED,
    /** The lasting mark of poison: each stack lowers spirit by one. */
    SICKENED,
    /** Each stack lowers armor by five. */
    SUNDERED,
    /** Crit chance taken doubles. */
    EXPOSED,
    /** The next hit lands as a guaranteed crit and spends the mark. */
    MARKED,
    /** Takes extra damage from every tower, and single-target towers aim at it. */
    PRIORITY,
    /** Each stack raises the magic damage taken. */
    RESONATING,
    /** Each stack lowers resilience by ten, and wears off one stack at a time. */
    FRACTURED,
    /** Stops the enemy and its abilities, like a freeze, but keeps its burn and its chill. */
    DAZED,
    /** Each stack makes the Splash's blasts hit it harder; it is the Splash's own rhythm, not a stat. */
    SATURATED,
    /** The next hit from another tower discharges it for extra magic, credited to the tower that charged it. */
    CHARGED,
    /** When it ends, the target takes a share of all the damage it took while it lasted. */
    DOOM,
    /** The target is poisoned for as long as it lasts. */
    BLIGHT,
    /** When the target dies, its hexes and debuffs jump to the enemies nearest it. */
    CONTAGION,
    /** Freezing the target buys twice the chill's extra time, and lands its burn at once instead of putting it out. */
    RIME,
    /** The target's burn and poison hold twice as much and mark it twice as fast; it can't be frozen and shrugs off chill. */
    ASH,
    /** Heals and shields it receives are dealt to it as damage instead, and it can't turn invisible. */
    INVERSION,
    /** Once a second its Vulnerable, Sundered, Exposed and chill are copied to the Hexer's other hexed enemies nearby. */
    SYMPATHY,
    /** When the target dies, the Hexer's Dooms near it release at once and start again. */
    RECKONING,
    /** Its abilities don't fire; death abilities and auras still do. */
    SILENCED,
    /** Can't be sped up, and its speed is held to three quarters of its base. */
    ANCHORED,
    /** Each stack lowers magic resist by ten. */
    UNRAVELED,
    /** A frozen enemy takes extra physical damage. */
    BRITTLE,
    /** Each stack makes every other debuff wear off a tenth slower; it fades soon after the enemy leaves the field. */
    TOLL,
    /** Lowers armor by thirty while the enemy is in the field. */
    CORRODED,
    /** Chilled a quarter that doesn't fade while the enemy is in the field, and counts as a chill for a freeze. */
    UNDERTOW,
    /** Heals and shields can't take hold while the enemy is in the field. */
    DEAD_ZONE,
    /** Takes extra damage from every source while the enemy is in the field. */
    KILL_ZONE,
    /** Plating is halved. */
    CRACKED,
    /** Stuck in tar: slowed by 40%, a burn it catches starts at double the pool, and a freeze it suffers lasts a second longer. */
    TARRED,
    /** Takes physical damage for every cell it travels; a stopped enemy takes none. */
    BLEEDING;

    public EffectCategory category() {
        return switch (this) {
            case CHILL, SILENCED, ANCHORED, UNDERTOW -> EffectCategory.SOFT_CC;
            case FREEZE, DAZED -> EffectCategory.HARD_CC;
            case BURN, POISON, BLEEDING -> EffectCategory.DAMAGE_OVER_TIME;
            case VULNERABLE, SCORCHED, SICKENED, SUNDERED, RESONATING, FRACTURED, SATURATED, UNRAVELED, BRITTLE, TOLL,
                    CORRODED, DEAD_ZONE, KILL_ZONE, CRACKED, TARRED -> EffectCategory.DEBUFF;
            case EXPOSED, MARKED, PRIORITY, CHARGED -> EffectCategory.SPOTTED;
            case DOOM, BLIGHT, CONTAGION, RIME, ASH, INVERSION, SYMPATHY, RECKONING -> EffectCategory.HEX;
            case SHIELD, HEAL -> EffectCategory.RESTORATIVE;
            case INVISIBLE, REVEALED -> EffectCategory.STEALTH;
        };
    }

    private static final List<EffectKind> STOPPING = Arrays.stream(values()).filter(EffectKind::stopsEnemy).toList();

    /** Whether this kind stops the enemy where it stands and keeps it from casting. */
    public boolean stopsEnemy() {
        return this == FREEZE || this == DAZED;
    }

    /** Every kind that {@link #stopsEnemy() stops an enemy}. */
    public static List<EffectKind> stopping() {
        return STOPPING;
    }

    /**
     * Whether this kind is a decaying level rather than a countdown: it ends when the level runs
     * out, and has no time left to show.
     */
    public boolean isDecaying() {
        return this == CHILL || this == BURN || this == POISON || this.isStackDebuff();
    }

    /**
     * Whether this kind is a fuel pool: damage that decays exponentially, and a debuff that grows a
     * stack every {@code Effect.STACK_INTERVAL_TICKS} while it lasts.
     */
    public boolean isFuelPool() {
        return this == BURN || this == POISON;
    }

    /**
     * Whether this kind is a count of stacks that wear off one at a time, at a pace the enemy's
     * spirit sets. A pool earns {@code SCORCHED} and {@code SICKENED}; a tower applies
     * {@code FRACTURED}.
     */
    public boolean isStackDebuff() {
        return this == SCORCHED || this == SICKENED || this == FRACTURED;
    }

    /**
     * The most stacks of a kind that counts them on one timer, every application refreshing it; 0
     * for a kind that does not stack that way.
     */
    public int maxStacks() {
        return switch (this) {
            case VULNERABLE, RESONATING, SATURATED -> 3;
            case UNRAVELED, TOLL -> 5;
            case SUNDERED -> 10;
            case FRACTURED -> 5;
            default -> 0;
        };
    }

    /**
     * Whether the enemy's spirit sets how fast this kind's timer runs. Stack debuffs have their own
     * pace, a pool's damage keeps its curve, and an enemy's own invisibility, shield and heal are
     * not debuffs.
     */
    public boolean isPacedBySpirit() {
        return switch (this) {
            case CHILL, VULNERABLE, REVEALED, SUNDERED, EXPOSED, MARKED, PRIORITY, RESONATING, CHARGED, UNRAVELED,
                    BRITTLE, CRACKED -> true;
            case DOOM, BLIGHT, CONTAGION, RIME, ASH, INVERSION, SYMPATHY, RECKONING -> true;
            default -> false;
        };
    }

    /**
     * Whether a curse carries this kind on to the next enemy when its carrier dies: the debuffs
     * that weaken an enemy, not the marks a tower leaves for itself.
     */
    public boolean spreadsWithCurse() {
        return switch (this) {
            case VULNERABLE, SUNDERED, EXPOSED, POISON, SCORCHED, SICKENED, RESONATING, FRACTURED, UNRAVELED -> true;
            default -> false;
        };
    }

    /** Whether a Sympathy carrier shares this kind with the hexed enemies near it. */
    public boolean sharedBySympathy() {
        return this == VULNERABLE || this == SUNDERED || this == EXPOSED || this == CHILL || this == UNRAVELED;
    }

    /** The stack debuff a burn or poison earns while it lasts; empty for any other kind. */
    public Optional<EffectKind> debuffEarned() {
        return switch (this) {
            case BURN -> Optional.of(SCORCHED);
            case POISON -> Optional.of(SICKENED);
            default -> Optional.empty();
        };
    }

    /** The resistance that shortens this kind; empty for kinds an enemy cannot resist. */
    public Optional<EnemyStat> resistedBy() {
        return switch (this) {
            case CHILL -> Optional.of(EnemyStat.CHILL_RESIST);
            case BURN -> Optional.of(EnemyStat.BURN_RESIST);
            case FREEZE -> Optional.of(EnemyStat.FREEZE_RESIST);
            case SHIELD, INVISIBLE, HEAL, VULNERABLE, REVEALED, POISON, SCORCHED, SICKENED, SUNDERED, EXPOSED, MARKED,
                    PRIORITY, RESONATING, FRACTURED, DAZED, SATURATED, CHARGED, DOOM, BLIGHT, CONTAGION, RIME, ASH, INVERSION,
                    SYMPATHY, RECKONING, SILENCED, ANCHORED, UNRAVELED, BRITTLE, TOLL, CORRODED, UNDERTOW, DEAD_ZONE,
                    KILL_ZONE, CRACKED, TARRED, BLEEDING -> Optional.empty();
        };
    }
}
