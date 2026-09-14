package td.tower;

/**
 * Double-dispatch over the closed set of concrete {@link Tower} types, used
 * by td.ui's rendering code so it can draw type-specific tower effects
 * without an instanceof chain (see CLAUDE.md's no-instanceof rule).
 * <p>
 * Adding a method here is deliberately a breaking change: it forces both
 * {@code TowerSpriteFrameBuilder} and {@code TowerEffectFrameBuilder} to
 * describe the new tower rather than silently skipping it.
 */
public interface TowerVisitor<R> {
    R visitTowerOne(TowerOne tower);

    R visitTowerTwo(TowerTwo tower);

    R visitTowerThree(TowerThree tower);

    R visitTowerFour(TowerFour tower);

    R visitTowerAura(TowerAura tower);
}
