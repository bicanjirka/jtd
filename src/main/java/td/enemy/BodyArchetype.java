package td.enemy;

/**
 * The closed set of rendering body shapes an {@link EnemyDefinition} picks from and
 * parameterizes (via {@code td.ui.render.Palette} and the definition's own base health/price),
 * mirroring the towers' one-flat-shape-per-type convention (see {@code td/ui/CLAUDE.md}).
 * Kept closed and small on purpose: an open per-definition art model would be a real departure
 * from this codebase's "renderer owns every shape/colour choice" rule.
 */
public enum BodyArchetype {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    GHOST,
    EGG
}
