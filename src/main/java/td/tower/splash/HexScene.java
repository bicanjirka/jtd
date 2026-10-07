package td.tower.splash;

import td.enemy.EnemyMob;

import java.util.List;

/**
 * What a hex picks its target from.
 *
 * @param candidates     the enemies in range the Hexer may curse
 * @param blastRadius    the blast's radius, in pixels: within it, enemies count as neighbours
 * @param spreadDistance how far, in pixels, a curse spreads, grown by the blast radius bonus
 * @param shareDistance  how far, in pixels, a hex shares or releases its effect, grown the same way
 */
public record HexScene(List<EnemyMob> candidates, float blastRadius, float spreadDistance, float shareDistance) {

    public HexScene {
        candidates = List.copyOf(candidates);
    }
}
