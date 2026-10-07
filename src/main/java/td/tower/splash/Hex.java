package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.TickRate;

import java.util.Optional;

/**
 * One hex in the Hexer's pool: the curse it puts on, whom it picks, and how it casts. Stateless:
 * what a hex pays out later is the ledger's to watch.
 */
public interface Hex {

    /** The effect that marks an enemy as carrying this hex, and times it. */
    EffectKind kind();

    /** Whom this hex would curse now, by its own rule; empty when nobody in range fits. */
    Optional<EnemyMob> target(HexScene scene);

    void cast(EnemyMob target, HexSpec spec, HexActions actions);

    /** How many ticks this hex lasts on {@code target}; a hex lasts {@link HexSpec#HEX_SECONDS} unless it says otherwise. */
    default int ticksOn(EnemyMob target) {
        return Math.round(HexSpec.HEX_SECONDS * TickRate.TICKS_PER_SECOND);
    }
}
