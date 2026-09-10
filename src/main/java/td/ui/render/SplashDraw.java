package td.ui.render;

/**
 * A filled disc marking where a splash shot landed - centred on the mob that was hit, not on
 * the tower that fired, and sized to the blast radius rather than to the tower's range.
 */
public record SplashDraw(Palette palette, float centerX, float centerY, float radius) implements TowerEffectDraw {
}
