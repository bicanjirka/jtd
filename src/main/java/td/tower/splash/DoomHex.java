package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.HighestHealthSelector;
import td.util.TickRate;

import java.util.Optional;

/**
 * Hex of Doom: when it ends, the enemy takes a share of all the damage it took while hexed. It
 * lasts longer on a Saturated enemy. Picks the healthiest enemy in range without Doom.
 */
public final class DoomHex implements Hex {

    private static final float SECONDS = 4f;
    private static final float SECONDS_PER_SATURATION_STACK = 1f;

    @Override
    public EffectKind kind() {
        return EffectKind.DOOM;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new HighestHealthSelector().selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.DOOM))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        float seconds = SECONDS + SECONDS_PER_SATURATION_STACK * target.effectStacks(EffectKind.SATURATED);
        actions.curse(target, EffectKind.DOOM, Math.round(seconds * TickRate.TICKS_PER_SECOND));
    }
}
