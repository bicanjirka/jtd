package td.ui.render;

/**
 * Ice over a frozen enemy's body, in addition to its freeze marker, so frozen reads apart from
 * slowed.
 */
public record IceCrystalDraw(Palette palette, float centerX, float centerY, float scale) implements EnemyOverlayDraw {
}
