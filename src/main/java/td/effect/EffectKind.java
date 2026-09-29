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
    SICKENED;

    public EffectCategory category() {
        return switch (this) {
            case CHILL -> EffectCategory.SOFT_CC;
            case FREEZE -> EffectCategory.HARD_CC;
            case BURN, POISON -> EffectCategory.DAMAGE_OVER_TIME;
            case VULNERABLE, SCORCHED, SICKENED -> EffectCategory.DEBUFF;
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
     * Whether this kind is a count of stacks that only the fuel pools earn and that wear off one at a
     * time, at a pace the enemy's spirit sets.
     */
    public boolean isStackDebuff() {
        return this == SCORCHED || this == SICKENED;
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
            case SHIELD, INVISIBLE, HEAL, VULNERABLE, REVEALED, POISON, SCORCHED, SICKENED -> Optional.empty();
        };
    }
}
