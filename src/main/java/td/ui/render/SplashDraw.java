package td.ui.render;

/** A filled disc where a splash landed, sized to the blast. */
public record SplashDraw(Palette palette, float centerX, float centerY, float radius) implements TowerEffectDraw {
}
