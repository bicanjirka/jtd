package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.targeting.HighestHealthSelector;
import td.util.TickRate;

import java.util.Optional;

/**
 * Hex of Rime or Hex of Ash: the coven's hex that bends how the enemy takes fire and ice. Picks the
 * healthiest enemy without it. Rime also chills as it is cast.
 */
public final class CovenHex implements Hex {

    private static final float RIME_CHILL = 0.3f;

    private final EffectKind kind;

    private CovenHex(EffectKind kind) {
        this.kind = kind;
    }

    /** Hex of Rime: freezing the enemy buys twice the chill's extra time and lands its burn at once. */
    public static CovenHex rime() {
        return new CovenHex(EffectKind.RIME);
    }

    /** Hex of Ash: its burns and poisons hold twice as much and mark it twice as fast; no freeze takes. */
    public static CovenHex ash() {
        return new CovenHex(EffectKind.ASH);
    }

    @Override
    public EffectKind kind() {
        return this.kind;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        return new HighestHealthSelector().selectFrom(scene.candidates().stream()
                .filter(enemy -> !enemy.hasEffect(this.kind))
                .toList());
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        int ticks = Math.round(HexSpec.HEX_SECONDS * TickRate.TICKS_PER_SECOND);
        actions.curse(target, this.kind, ticks);
        if (this.kind == EffectKind.RIME) {
            actions.chill(target, RIME_CHILL, ticks);
        }
    }
}
