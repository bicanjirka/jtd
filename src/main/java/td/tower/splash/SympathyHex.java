package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.MostNeighboursSelector;

import java.util.Optional;

/**
 * Hex of Sympathy: what one suffers, all suffer. Picks the most crowded enemy without Sympathy.
 */
public final class SympathyHex implements Hex {

    @Override
    public EffectKind kind() {
        return EffectKind.SYMPATHY;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new MostNeighboursSelector(scene.blastRadius()).selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.SYMPATHY))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        actions.curse(target, EffectKind.SYMPATHY, this.ticksOn(target));
    }
}
