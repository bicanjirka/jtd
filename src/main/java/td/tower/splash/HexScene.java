package td.tower.splash;

import td.enemy.EnemyMob;

import java.util.List;

/**
 * What a hex picks its target from.
 *
 * @param candidates     the enemies in range the Hexer may curse
 * @param blastRadius    the blast's radius, in pixels: within it, enemies count as neighbours
 * @param spreadDistance how far, in pixels, a curse spreads or shares, grown by the blast radius bonus
 */
public record HexScene(List<EnemyMob> candidates, float blastRadius, float spreadDistance) {

    public HexScene {
        candidates = List.copyOf(candidates);
    }
}
