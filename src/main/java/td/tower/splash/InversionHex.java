package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.FurthestAlongPathSelector;

import java.util.List;
import java.util.Optional;

/**
 * Hex of Inversion: what helps the enemy hurts it, and it can't vanish. Picks the enemy closest to leaking that heals,
 * shields, vanishes or regenerates, without Inversion.
 */
public final class InversionHex implements Hex {

    private static final float SECONDS = 6f;

    @Override
    public EffectKind kind() {
        return EffectKind.INVERSION;
    }

    @Override
    public float seconds() {
        return SECONDS;
    }

    @Override
    public Optional<EnemyMob> target(List<EnemyMob> candidates, float cellPixels) {
        return new FurthestAlongPathSelector().selectFrom(candidates.stream()
                .filter(enemy -> (enemy.appliesEffects() || enemy.regenerates())
                        && !enemy.hasEffect(EffectKind.INVERSION))
                .toList());
    }

    @Override
    public void lay(EnemyMob target, int ticks, float grownSlow, HexHand hand) {
        hand.curse(target, EffectKind.INVERSION, ticks);
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.INVERSION, TermText.of("for " + TermNumbers.seconds(SECONDS)
                + ", on an enemy that heals, shields, vanishes or regenerates"));
    }
}
