package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.targeting.LowestHealthSelector;

import java.util.List;
import java.util.Optional;

/**
 * Hex of Contagion: when the enemy dies, its hexes and debuffs jump on to the enemies nearest it.
 * Picks the enemy closest to dying that has a neighbour to jump to, without Contagion.
 */
public final class ContagionHex implements Hex {

    /** How many enemies a Contagion jumps to. */
    public static final int TARGETS = 2;
    /** How far, in cells before the blast radius bonus, a curse spreads. */
    public static final float SPREAD_CELLS = 1.5f;
    /** How often a hex may jump at most. */
    static final int MAX_JUMPS = 2;

    @Override
    public EffectKind kind() {
        return EffectKind.CONTAGION;
    }

    @Override
    public Optional<EnemyMob> target(HexScene scene) {
        List<EnemyMob> candidates = scene.candidates();
        return new LowestHealthSelector().selectFrom(candidates.stream()
                .filter(enemy -> !enemy.hasEffect(EffectKind.CONTAGION) && hasNeighbour(enemy, candidates,
                        scene.spreadDistance()))
                .toList());
    }

    private static boolean hasNeighbour(EnemyMob enemy, List<EnemyMob> candidates, float distance) {
        return candidates.stream().anyMatch(other -> other != enemy
                && Math.hypot(other.getX() - enemy.getX(), other.getY() - enemy.getY()) <= distance);
    }

    @Override
    public void cast(EnemyMob target, HexSpec spec, HexActions actions) {
        actions.curse(target, EffectKind.CONTAGION, this.ticksOn(target));
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.CONTAGION, TermText.of("when the enemy dies, its hexes and debuffs jump to the " + TARGETS
                + " nearest enemies without a ").plus(TowerTerm.HEX));
    }
}
