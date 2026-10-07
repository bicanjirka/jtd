package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.FurthestAlongPathSelector;

import java.util.Optional;

/**
 * Hex of Inversion: what helps the enemy hurts it, and it can't vanish. Picks the enemy closest to
 * leaking that heals, shields or vanishes, without Inversion.
 */
public final class InversionHex implements Hex {

    @Override
    public EffectKind kind() {
        return EffectKind.INVERSION;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new FurthestAlongPathSelector().selectFrom(scene.candidates().stream()
                .filter(enemy -> enemy.appliesEffects() && !enemy.hasEffect(EffectKind.INVERSION))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        actions.curse(target, EffectKind.INVERSION, this.ticksOn(target));
    }
}
