package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Hex of Reckoning: when the enemy dies, the Hexer's Dooms near it release at once. Picks the
 * enemy with most Doomed neighbours, without Reckoning; nobody when no enemy has one.
 */
public final class ReckoningHex implements Hex {

    @Override
    public EffectKind kind() {
        return EffectKind.RECKONING;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        List<EnemyMob> candidates = scene.candidates();
        return candidates.stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.RECKONING)
                        && doomedNeighbours(enemy, candidates, scene.shareDistance()) > 0)
                .max(Comparator.comparingInt((EnemyMob enemy) -> doomedNeighbours(enemy, candidates,
                        scene.shareDistance())).thenComparingInt(EnemyMob::getProgression));
    }

    private static int doomedNeighbours(EnemyMob enemy, List<EnemyMob> candidates, float distance) {
        return (int) candidates.stream().filter(other -> other != enemy && other.hasEffect(EffectKind.DOOM)
                && Math.hypot(other.getX() - enemy.getX(), other.getY() - enemy.getY()) <= distance).count();
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        actions.curse(target, EffectKind.RECKONING, this.ticksOn(target));
    }
}
