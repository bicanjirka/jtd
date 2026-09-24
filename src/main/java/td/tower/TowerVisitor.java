package td.tower;

/**
 * Double dispatch over the concrete {@link Tower} types, so rendering needs no type checks. Adding
 * a method forces every visitor to handle the new tower.
 */
public interface TowerVisitor<R> {
    R visitSniperTower(SniperTower tower);

    R visitSplashTower(SplashTower tower);

    R visitSonarTower(SonarTower tower);

    R visitPulseTower(PulseTower tower);

    R visitAuraTower(AuraTower tower);

    R visitMortarTower(MortarTower tower);

    R visitSeekerTower(SeekerTower tower);

    R visitCinderTower(CinderTower tower);
}
