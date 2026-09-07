package td.tower;

/**
 * Double-dispatch over the closed set of concrete {@link Tower} types, used
 * by td.ui's rendering code so it can draw type-specific tower effects
 * without an instanceof chain (see CLAUDE.md's rule 9).
 */
public interface TowerVisitor<R> {
    R visitTowerOne(TowerOne tower);

    R visitTowerTwo(TowerTwo tower);

    R visitTowerThree(TowerThree tower);

    R visitTowerFour(TowerFour tower);

    R visitTowerUpgrade(TowerUpgrade tower);
}
