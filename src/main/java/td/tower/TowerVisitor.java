package td.tower;

/**
 * Double-dispatch over the closed set of concrete {@link Tower} types, used
 * by td.ui's rendering code so it can draw type-specific tower effects
 * without an instanceof chain (see CLAUDE.md §5 rule 11).
 * <p>
 * Adding a method here is deliberately a breaking change: it forces both
 * {@code TowerSpriteFrameBuilder} and {@code TowerEffectFrameBuilder} to
 * describe the new tower rather than silently skipping it.
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
