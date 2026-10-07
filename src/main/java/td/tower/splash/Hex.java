package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

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
}
