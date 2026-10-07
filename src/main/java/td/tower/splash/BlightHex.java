package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.MostNeighboursSelector;
import td.util.TickRate;

import java.util.Optional;

/** Hex of Blight: poisons the enemy for as long as the hex lasts. Picks the most crowded enemy without Blight. */
public final class BlightHex implements Hex {

    @Override
    public EffectKind kind() {
        return EffectKind.BLIGHT;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new MostNeighboursSelector(scene.blastRadius()).selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.BLIGHT))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        int ticks = Math.round(HexSpec.HEX_SECONDS * TickRate.TICKS_PER_SECOND);
        actions.curse(target, EffectKind.BLIGHT, ticks);
        actions.poison(target, spec.blightShare(), ticks);
    }
}
