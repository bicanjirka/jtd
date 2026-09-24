package td.effect;

import td.stat.EnemyStat;

import java.util.Optional;

public enum EffectKind {
    SLOW,
    BURN,
    FREEZE,
    SHIELD,
    INVISIBLE,
    HEAL;

    /** The resistance that shortens this kind; empty for kinds an enemy cannot resist. */
    public Optional<EnemyStat> resistedBy() {
        return switch (this) {
            case SLOW -> Optional.of(EnemyStat.SLOW_RESIST);
            case BURN -> Optional.of(EnemyStat.BURN_RESIST);
            case FREEZE -> Optional.of(EnemyStat.FREEZE_RESIST);
            case SHIELD, INVISIBLE, HEAL -> Optional.empty();
        };
    }
}
