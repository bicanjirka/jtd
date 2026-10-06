package td.effect;

import td.stat.EnemyStat;

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
    FRACTURED;

    public EffectCategory category() {
        return switch (this) {
            case CHILL -> EffectCategory.SOFT_CC;
            case FREEZE -> EffectCategory.HARD_CC;
            case BURN, POISON -> EffectCategory.DAMAGE_OVER_TIME;
            case VULNERABLE, SCORCHED, SICKENED, SUNDERED, RESONATING, FRACTURED -> EffectCategory.DEBUFF;
            case EXPOSED, MARKED, PRIORITY -> EffectCategory.SPOTTED;
            case SHIELD, HEAL -> EffectCategory.RESTORATIVE;
            case INVISIBLE, REVEALED -> EffectCategory.STEALTH;
        };
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
            case VULNERABLE, RESONATING -> 3;
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
            case CHILL, VULNERABLE, REVEALED, SUNDERED, EXPOSED, MARKED, PRIORITY, RESONATING -> true;
            default -> false;
        };
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
                    PRIORITY, RESONATING, FRACTURED -> Optional.empty();
        };
    }
}
