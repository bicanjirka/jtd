package td.enemy;

/**
 * The closed set of rendering body shapes an {@link EnemyDefinition} picks from, mirroring the
 * towers' one-flat-shape-per-type convention (see {@code td/ui/CLAUDE.md}). Kept closed and
 * small on purpose: an open per-definition art model would be a real departure from this
 * codebase's "renderer owns every shape/colour choice" rule. A new case needs a
 * {@code td.ui.render.Palette} role and a shape/colour case in {@code Java2DFrameRenderer}
 * wired at the same time it's added here - see {@code td/enemy/CLAUDE.md}'s "Adding a new
 * enemy type" checklist - since {@code Java2DFrameRenderer.enemyShape}'s per-palette switch
 * isn't compiler-enforced exhaustive (it falls back to a runtime exception for an unhandled
 * palette): a case added here without its render wiring landing in the same change would
 * compile clean and only fail the first time something actually renders it. {@code EGG}
 * reuses {@code circleShape} with its own tint rather than a new geometry, matching
 * {@code CIRCLE}/{@code GHOST}'s existing precedent.
 */
public enum BodyArchetype {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    GHOST,
    EGG,
    WARDEN
}
